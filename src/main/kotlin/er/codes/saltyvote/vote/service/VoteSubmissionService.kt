package er.codes.saltyvote.vote.service

import er.codes.saltyvote.auth.model.UserEntity
import er.codes.saltyvote.auth.repository.UserDao
import er.codes.saltyvote.history.model.events.VoteEntryCreatedEvent
import er.codes.saltyvote.history.model.events.VoteSubmissionEntryData
import er.codes.saltyvote.history.model.events.VoteSubmittedEvent
import er.codes.saltyvote.jooq.tables.daos.VoteOptionsDao
import er.codes.saltyvote.jooq.tables.daos.VoteSubmissionEntriesDao
import er.codes.saltyvote.jooq.tables.daos.VoteSubmissionsDao
import er.codes.saltyvote.jooq.tables.daos.VotesDao
import er.codes.saltyvote.jooq.tables.pojos.VoteSubmissionEntries
import er.codes.saltyvote.jooq.tables.pojos.VoteSubmissions
import er.codes.saltyvote.vote.model.IndividualVoterDto
import er.codes.saltyvote.vote.model.VoteDetailedResultsDto
import er.codes.saltyvote.vote.model.VoteOptionResultDto
import er.codes.saltyvote.vote.model.VoteResultsDto
import er.codes.saltyvote.vote.model.VoteSubmissionDto
import er.codes.saltyvote.vote.model.VoteSubmissionEntryDto
import er.codes.saltyvote.vote.model.VoteSubmissionResponseDto
import er.codes.saltyvote.vote.model.VoterRankingDto
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class VoteSubmissionService(
    private val votesDao: VotesDao,
    private val voteOptionsDao: VoteOptionsDao,
    private val voteSubmissionsDao: VoteSubmissionsDao,
    private val voteSubmissionEntriesDao: VoteSubmissionEntriesDao,
    private val userDao: UserDao,
    private val voteCollaboratorService: VoteCollaboratorService,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun submitVote(request: VoteSubmissionDto): Long {
        val vote =
            votesDao.fetchOneById(request.voteId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Vote not found")

        if (vote.done == true) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Vote is closed")
        }

        val currentUser = getCurrentUserIfAuthenticated()

        // Check if user is allowed to vote
        if (currentUser == null && vote.allowAnonymous != true) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Anonymous voting not allowed")
        }

        // Validate options exist and belong to this vote
        val voteOptions = voteOptionsDao.fetchByVoteId(request.voteId)
        val validOptionIds = voteOptions.map { it.id }.toSet()

        request.entries.forEach { entry ->
            if (entry.optionId !in validOptionIds) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid option ID: ${entry.optionId}",
                )
            }
        }

        // Validate ranking for RANKING vote type
        if (vote.voteType == er.codes.saltyvote.jooq.enums.VoteType.RANKING) {
            // All entries must have a rank
            val missingRanks = request.entries.filter { it.rank == null }
            if (missingRanks.isNotEmpty()) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "All options must have a rank for RANKING votes",
                )
            }

            // Ranks should be unique
            val ranks = request.entries.mapNotNull { it.rank }
            if (ranks.size != ranks.toSet().size) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ranks must be unique",
                )
            }
        }

        // Check if user already voted (if authenticated)
        val existingSubmission =
            if (currentUser != null) {
                val existingSubmissions = voteSubmissionsDao.fetchByVoteId(request.voteId)
                existingSubmissions.find { it.userId == currentUser.id }
            } else {
                null
            }

        val submission: VoteSubmissions
        val submissionId =
            if (existingSubmission != null) {
                // Update existing submission - delete old entries and insert new ones
                val oldEntries = voteSubmissionEntriesDao.fetchBySubmissionId(existingSubmission.id!!)
                oldEntries.forEach { voteSubmissionEntriesDao.delete(it) }

                submission = existingSubmission
                existingSubmission.id!!
            } else {
                // Create new submission
                val userId = currentUser?.id ?: UUID.randomUUID()

                submission =
                    VoteSubmissions(
                        voteId = request.voteId,
                        userId = userId,
                    )

                voteSubmissionsDao.insert(submission)

                submission.id
                    ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create submission")
            }

        // Insert submission entries and collect entry data for events
        val entryDataList = mutableListOf<VoteSubmissionEntryData>()
        request.entries.forEach { entry ->
            val voteEntry =
                VoteSubmissionEntries(
                    submissionId = submissionId,
                    optionId = entry.optionId,
                    rank = entry.rank,
                    selected = entry.selected,
                )
            voteSubmissionEntriesDao.insert(voteEntry)

            // Publish event for each entry
            applicationEventPublisher.publishEvent(
                VoteEntryCreatedEvent(
                    entry = voteEntry,
                    submissionId = submissionId,
                    voteId = request.voteId,
                    userId = submission.userId!!,
                ),
            )

            // Collect entry data for vote submitted event
            entryDataList.add(
                VoteSubmissionEntryData(
                    optionId = entry.optionId,
                    rank = entry.rank,
                    selected = entry.selected,
                ),
            )
        }

        // Publish vote submitted event with all entries
        applicationEventPublisher.publishEvent(
            VoteSubmittedEvent(
                submission = submission,
                entries = entryDataList,
            ),
        )

        return submissionId
    }

    fun getMySubmission(voteId: Long): VoteSubmissionResponseDto? {
        val currentUser = getCurrentUserOrThrow()

        val submissions = voteSubmissionsDao.fetchByVoteId(voteId)
        val mySubmission = submissions.find { it.userId == currentUser.id } ?: return null

        val entries = voteSubmissionEntriesDao.fetchBySubmissionId(mySubmission.id!!)

        return VoteSubmissionResponseDto(
            id = mySubmission.id!!,
            voteId = voteId,
            userId = currentUser.id,
            entries =
                entries.map { entry ->
                    VoteSubmissionEntryDto(
                        optionId = entry.optionId!!,
                        rank = entry.rank,
                        selected = entry.selected,
                    )
                },
        )
    }

    fun getVoteResults(voteId: Long): VoteResultsDto {
        val vote =
            votesDao.fetchOneById(voteId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Vote not found")

        val submissions = voteSubmissionsDao.fetchByVoteId(voteId)
        val totalSubmissions = submissions.size

        val options = voteOptionsDao.fetchByVoteId(voteId)

        val results =
            options.map { option ->
                val optionId = option.id!!

                // Get all submission entries for this option across all submissions
                val allSubmissionIds = submissions.mapNotNull { it.id }
                val entries =
                    allSubmissionIds.flatMap { submissionId ->
                        voteSubmissionEntriesDao
                            .fetchBySubmissionId(submissionId)
                            .filter { it.optionId == optionId }
                    }

                val voteCount = entries.count { it.selected == true }
                val percentage =
                    if (totalSubmissions > 0) {
                        (voteCount.toDouble() / totalSubmissions) * 100
                    } else {
                        0.0
                    }

                // For ranking votes, calculate average rank (lower is better)
                val rankedEntries = entries.mapNotNull { it.rank }
                val averageRank =
                    if (rankedEntries.isNotEmpty()) {
                        rankedEntries.average()
                    } else {
                        null
                    }

                VoteOptionResultDto(
                    optionId = optionId,
                    label = option.label!!,
                    voteCount = voteCount,
                    percentage = percentage,
                    averageRank = averageRank,
                )
            }

        // Sort results: for ranking votes, sort by average rank (ascending), otherwise by vote count (descending)
        val sortedResults =
            if (vote.voteType == er.codes.saltyvote.jooq.enums.VoteType.RANKING) {
                results.sortedBy { it.averageRank ?: Double.MAX_VALUE }
            } else {
                results.sortedByDescending { it.voteCount }
            }

        return VoteResultsDto(
            voteId = voteId,
            title = vote.title!!,
            totalSubmissions = totalSubmissions,
            results = sortedResults,
        )
    }

    @Transactional
    fun deleteMySubmission(voteId: Long) {
        val currentUser = getCurrentUserOrThrow()

        val submissions = voteSubmissionsDao.fetchByVoteId(voteId)
        val mySubmission =
            submissions.find { it.userId == currentUser.id }
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No submission found")

        voteSubmissionsDao.delete(mySubmission)
    }

    fun getDetailedVoteResults(voteId: Long): VoteDetailedResultsDto {
        val vote =
            votesDao.fetchOneById(voteId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Vote not found")

        val currentUser = getCurrentUserIfAuthenticated()

        val submissions = voteSubmissionsDao.fetchByVoteId(voteId)
        val options = voteOptionsDao.fetchByVoteId(voteId)

        // Get individual voter details
        val individualVoters =
            submissions.mapNotNull { submission ->
                val user = userDao.findById(submission.userId!!)

                // Skip anonymous users (those not in the users table)
                user?.let {
                    val entries = voteSubmissionEntriesDao.fetchBySubmissionId(submission.id!!)
                    val rankings =
                        entries
                            .map { entry ->
                                val option = options.find { it.id == entry.optionId }
                                VoterRankingDto(
                                    optionId = entry.optionId!!,
                                    optionLabel = option?.label ?: "Unknown",
                                    rank = entry.rank,
                                    selected = entry.selected,
                                )
                            }.sortedBy { it.rank ?: Int.MAX_VALUE }

                    IndividualVoterDto(
                        userId = it.id,
                        email = it.email,
                        username = it.username,
                        submittedAt = submission.createdAt!!,
                        rankings = rankings,
                    )
                }
            }

        // Calculate aggregate results
        val totalSubmissions = submissions.size
        val results =
            options.map { option ->
                val optionId = option.id!!

                val allSubmissionIds = submissions.mapNotNull { it.id }
                val entries =
                    allSubmissionIds.flatMap { submissionId ->
                        voteSubmissionEntriesDao
                            .fetchBySubmissionId(submissionId)
                            .filter { it.optionId == optionId }
                    }

                val voteCount = entries.count { it.selected == true }
                val percentage =
                    if (totalSubmissions > 0) {
                        (voteCount.toDouble() / totalSubmissions) * 100
                    } else {
                        0.0
                    }

                val rankedEntries = entries.mapNotNull { it.rank }
                val averageRank =
                    if (rankedEntries.isNotEmpty()) {
                        rankedEntries.average()
                    } else {
                        null
                    }

                VoteOptionResultDto(
                    optionId = optionId,
                    label = option.label!!,
                    voteCount = voteCount,
                    percentage = percentage,
                    averageRank = averageRank,
                )
            }

        val sortedResults =
            if (vote.voteType == er.codes.saltyvote.jooq.enums.VoteType.RANKING) {
                results.sortedBy { it.averageRank ?: Double.MAX_VALUE }
            } else {
                results.sortedByDescending { it.voteCount }
            }

        return VoteDetailedResultsDto(
            voteId = voteId,
            title = vote.title!!,
            voteType = vote.voteType!!,
            totalSubmissions = totalSubmissions,
            results = sortedResults,
            individualVoters = individualVoters,
        )
    }

    private fun getCurrentUserIfAuthenticated(): UserEntity? {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication?.principal is UserEntity) {
            authentication.principal as UserEntity
        } else {
            null
        }
    }

    private fun getCurrentUserOrThrow(): UserEntity =
        getCurrentUserIfAuthenticated()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated")
}
