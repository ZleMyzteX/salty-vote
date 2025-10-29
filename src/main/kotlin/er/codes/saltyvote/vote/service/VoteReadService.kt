package er.codes.saltyvote.vote.service

import com.fasterxml.jackson.databind.ObjectMapper
import er.codes.saltyvote.auth.model.UserEntity
import er.codes.saltyvote.jooq.tables.daos.VoteOptionsDao
import er.codes.saltyvote.jooq.tables.daos.VotesDao
import er.codes.saltyvote.vote.model.AirbnbVoteOptionData
import er.codes.saltyvote.vote.model.AirbnbVoteOptionEnrichedDto
import er.codes.saltyvote.vote.model.AirbnbVoteOptionResponseDto
import er.codes.saltyvote.vote.model.VoteDetailDto
import er.codes.saltyvote.vote.model.VoteListDto
import er.codes.saltyvote.vote.model.VoteListWithRelationshipDto
import er.codes.saltyvote.vote.model.VoteOptionDto
import er.codes.saltyvote.vote.model.VoteWithAirbnbOptionsDto
import er.codes.saltyvote.vote.model.VoteWithEnrichedAirbnbOptionsDto
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class VoteReadService(
    private val voteDao: VotesDao,
    private val voteOptionsDao: VoteOptionsDao,
    private val objectMapper: ObjectMapper,
    private val voteCollaboratorService: VoteCollaboratorService,
    private val voteExternalDataService: VoteExternalDataService,
) {
    fun getVoteById(voteId: Long): VoteDetailDto {
        val vote =
            voteDao.fetchOneById(voteId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Vote not found")

        val options = voteOptionsDao.fetchByVoteId(voteId)

        return VoteDetailDto(
            id = vote.id!!,
            title = vote.title!!,
            description = vote.description,
            voteType = vote.voteType!!,
            allowMultiVote = vote.allowMulti!!,
            allowAnonymousVote = vote.allowAnonymous!!,
            done = vote.done!!,
            options =
                options.map { option ->
                    VoteOptionDto(
                        id = option.id!!,
                        label = option.label!!,
                        preDefinedPosition = option.position,
                    )
                },
        )
    }

    fun getAirbnbVoteById(voteId: Long): VoteWithAirbnbOptionsDto {
        val vote =
            voteDao.fetchOneById(voteId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Vote not found")

        val currentUser = getCurrentUser()
        val isCreator = vote.creatorId == currentUser?.id
        val isCollaborator = currentUser?.let { voteCollaboratorService.isCollaborator(voteId, it.id) } ?: false

        val options = voteOptionsDao.fetchByVoteId(voteId)

        return VoteWithAirbnbOptionsDto(
            id = vote.id!!,
            title = vote.title!!,
            description = vote.description,
            voteType = vote.voteType!!,
            allowMultiVote = vote.allowMulti!!,
            allowAnonymousVote = vote.allowAnonymous!!,
            done = vote.done!!,
            isCreator = isCreator,
            isCollaborator = isCollaborator,
            options =
                options.map { option ->
                    val airbnbData =
                        option.data?.let {
                            objectMapper.readValue(it.data(), AirbnbVoteOptionData::class.java)
                        }
                    AirbnbVoteOptionResponseDto(
                        id = option.id!!,
                        label = option.label!!,
                        data = airbnbData,
                        preDefinedPosition = option.position,
                    )
                },
        )
    }

    /**
     * Get Airbnb vote with enriched external data (scraped information)
     */
    fun getEnrichedAirbnbVoteById(voteId: Long): VoteWithEnrichedAirbnbOptionsDto {
        val vote =
            voteDao.fetchOneById(voteId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Vote not found")

        val currentUser = getCurrentUser()
        val isCreator = vote.creatorId == currentUser?.id
        val isCollaborator = currentUser?.let { voteCollaboratorService.isCollaborator(voteId, it.id) } ?: false

        val options = voteOptionsDao.fetchByVoteId(voteId)
        val optionIds = options.mapNotNull { it.id }

        // Fetch all external data in one go
        val externalDataMap = voteExternalDataService.getExternalDataForOptions(optionIds)

        return VoteWithEnrichedAirbnbOptionsDto(
            id = vote.id!!,
            title = vote.title!!,
            description = vote.description,
            voteType = vote.voteType!!,
            allowMultiVote = vote.allowMulti!!,
            allowAnonymousVote = vote.allowAnonymous!!,
            done = vote.done!!,
            isCreator = isCreator,
            isCollaborator = isCollaborator,
            options =
                options.map { option ->
                    val airbnbData =
                        option.data?.let {
                            objectMapper.readValue(it.data(), AirbnbVoteOptionData::class.java)
                        }
                    AirbnbVoteOptionEnrichedDto(
                        id = option.id!!,
                        label = option.label!!,
                        data = airbnbData,
                        externalData = externalDataMap[option.id],
                        preDefinedPosition = option.position,
                    )
                },
        )
    }

    fun getAllVotes(): List<VoteListDto> =
        voteDao.findAll().map { vote ->
            VoteListDto(
                id = vote.id!!,
                title = vote.title!!,
                description = vote.description,
            )
        }

    fun getMyVotes(): List<VoteListDto> {
        val currentUser = getCurrentUserOrThrow()
        return voteDao.fetchByCreatorId(currentUser.id).map { vote ->
            VoteListDto(
                id = vote.id!!,
                title = vote.title!!,
                description = vote.description,
            )
        }
    }

    fun getAllVotesWithRelationship(): List<VoteListWithRelationshipDto> {
        val currentUser = getCurrentUser()
        val allVotes = voteDao.findAll()

        // If no user is authenticated, return all votes with false flags
        if (currentUser == null) {
            return allVotes.map { vote ->
                VoteListWithRelationshipDto(
                    id = vote.id!!,
                    title = vote.title!!,
                    description = vote.description,
                    isCreator = false,
                    isCollaborator = false,
                )
            }
        }

        // Efficiently get all vote IDs where user is a collaborator
        val collaboratorVoteIds = voteCollaboratorService.getVoteIdsWhereUserIsCollaborator(currentUser.id)

        return allVotes.map { vote ->
            VoteListWithRelationshipDto(
                id = vote.id!!,
                title = vote.title!!,
                description = vote.description,
                isCreator = vote.creatorId == currentUser.id,
                isCollaborator = collaboratorVoteIds.contains(vote.id),
            )
        }
    }

    private fun getCurrentUser(): UserEntity? {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication?.principal is UserEntity) {
            authentication.principal as UserEntity
        } else {
            null
        }
    }

    private fun getCurrentUserOrThrow(): UserEntity =
        getCurrentUser()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated")
}
