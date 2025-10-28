package er.codes.saltyvote.vote.service

import com.fasterxml.jackson.databind.ObjectMapper
import er.codes.saltyvote.auth.model.UserEntity
import er.codes.saltyvote.history.model.events.VoteCreatedEvent
import er.codes.saltyvote.jooq.tables.daos.VoteOptionsDao
import er.codes.saltyvote.jooq.tables.daos.VotesDao
import er.codes.saltyvote.jooq.tables.pojos.VoteOptions
import er.codes.saltyvote.jooq.tables.pojos.Votes
import er.codes.saltyvote.scrape.model.ScrapeDataEvent
import er.codes.saltyvote.vote.model.AirbnbVoteOptionDto
import er.codes.saltyvote.vote.model.CreateAirbnbVoteDto
import org.jooq.JSONB
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class VoteManagementService(
    private val voteDao: VotesDao,
    private val voteOptionsDao: VoteOptionsDao,
    private val voteCollaboratorService: VoteCollaboratorService,
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun createAirbnbVote(req: CreateAirbnbVoteDto): Long {
        val currentUser = getCurrentUserOrThrow()

        val newVote =
            Votes(
                title = req.title,
                creatorId = currentUser.id,
                description = req.description,
                voteType = req.voteType,
                allowMulti = req.allowMultiVote,
                allowAnonymous = req.allowAnonymousVote,
                done = req.done,
            )

        voteDao.insert(newVote)

        val voteId =
            newVote.id
                ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create vote")

        val createdOptions = mutableListOf<VoteOptions>()
        req.options.forEach { option ->
            val voteOption =
                VoteOptions(
                    voteId = voteId,
                    label = option.label,
                    data =
                        JSONB.valueOf(
                            objectMapper.writeValueAsString(option.getAirbnbData()),
                        ),
                    position = option.predefinedPosition,
                )
            voteOptionsDao.insert(voteOption)
            // TODO: double option.getAirbnbData() fix
            applicationEventPublisher.publishEvent(
                ScrapeDataEvent(option.getAirbnbData().airbnbLink, voteOption.voteId!!),
            )
            createdOptions.add(voteOption)
        }

        /* TODO: Fix, broken
        applicationEventPublisher.publishEvent(
            VoteCreatedEvent(
                voteTitle = newVote.title!!,
                voteId = voteId,
                creatorId = currentUser.id,
                vote = newVote,
                options = createdOptions,
            ),
        )*/

        return voteId
    }

    @Transactional
    fun updateVote(
        voteId: Long,
        req: CreateAirbnbVoteDto,
    ) {
        val currentUser = getCurrentUserOrThrow()
        val vote =
            voteDao.fetchOneById(voteId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Vote not found")

        if (vote.creatorId != currentUser.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can update the vote")
        }

        vote.title = req.title
        vote.description = req.description
        vote.voteType = req.voteType
        vote.allowMulti = req.allowMultiVote
        vote.allowAnonymous = req.allowAnonymousVote
        vote.done = req.done

        voteDao.update(vote)
    }

    @Transactional
    fun deleteVote(voteId: Long) {
        val currentUser = getCurrentUserOrThrow()
        val vote =
            voteDao.fetchOneById(voteId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Vote not found")

        if (vote.creatorId != currentUser.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can delete the vote")
        }

        voteDao.delete(vote)
    }

    @Transactional
    fun markVoteAsDone(
        voteId: Long,
        done: Boolean,
    ) {
        val currentUser = getCurrentUserOrThrow()
        val vote =
            voteDao.fetchOneById(voteId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Vote not found")

        if (vote.creatorId != currentUser.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can change vote status")
        }

        vote.done = done
        voteDao.update(vote)
    }

    @Transactional
    fun addVoteOption(
        voteId: Long,
        req: AirbnbVoteOptionDto,
    ): Long {
        val currentUser = getCurrentUserOrThrow()

        if (!voteCollaboratorService.canModifyVote(voteId, currentUser.id)) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the creator or collaborators can add options",
            )
        }

        val option =
            VoteOptions(
                voteId = voteId,
                label = req.label,
                data =
                    JSONB.valueOf(
                        objectMapper.writeValueAsString(req.getAirbnbData()),
                    ),
                position = req.predefinedPosition,
            )

        voteOptionsDao.insert(option)

        applicationEventPublisher.publishEvent(ScrapeDataEvent(req.getAirbnbData().airbnbLink, option.voteId!!))
        return option.id ?: throw ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Failed to create option",
        )
    }

    @Transactional
    fun deleteVoteOption(
        voteId: Long,
        optionId: Long,
    ) {
        val currentUser = getCurrentUserOrThrow()

        if (!voteCollaboratorService.canModifyVote(voteId, currentUser.id)) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the creator or collaborators can delete options",
            )
        }

        val option =
            voteOptionsDao.fetchOneById(optionId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Option not found")

        if (option.voteId != voteId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Option does not belong to this vote")
        }

        voteOptionsDao.delete(option)
    }

    private fun getCurrentUserOrThrow(): UserEntity {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication?.principal is UserEntity) {
            authentication.principal as UserEntity
        } else {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated")
        }
    }
}
