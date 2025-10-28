package er.codes.saltyvote.vote.service

import er.codes.saltyvote.auth.repository.UserDao
import er.codes.saltyvote.jooq.tables.daos.VoteCollaboratorsDao
import er.codes.saltyvote.jooq.tables.daos.VotesDao
import er.codes.saltyvote.jooq.tables.pojos.VoteCollaborators
import er.codes.saltyvote.vote.model.AddCollaboratorDto
import er.codes.saltyvote.vote.model.CollaboratorDto
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class VoteCollaboratorService(
    private val votesDao: VotesDao,
    private val voteCollaboratorsDao: VoteCollaboratorsDao,
    private val userDao: UserDao,
) {
    @Transactional
    fun addCollaborator(
        voteId: Long,
        creatorId: UUID,
        request: AddCollaboratorDto,
    ): CollaboratorDto {
        // Verify vote exists and user is the creator
        val vote =
            votesDao.fetchOneById(voteId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Vote not found")

        if (vote.creatorId != creatorId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only the vote creator can add collaborators")
        }

        // Verify user exists
        val user =
            userDao.findById(request.userId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        // Check if already a collaborator
        if (isCollaborator(voteId, request.userId)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "User is already a collaborator")
        }

        // Insert collaborator using DAO
        val collaborator =
            VoteCollaborators(
                voteId = voteId,
                userId = request.userId,
            )
        voteCollaboratorsDao.insert(collaborator)

        return CollaboratorDto(
            id = collaborator.id!!,
            userId = user.id,
            username = user.username,
            email = user.email,
        )
    }

    @Transactional
    fun removeCollaborator(
        voteId: Long,
        creatorId: UUID,
        collaboratorId: Long,
    ) {
        val vote =
            votesDao.fetchOneById(voteId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Vote not found")

        if (vote.creatorId != creatorId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only the vote creator can remove collaborators")
        }

        val collaborator =
            voteCollaboratorsDao.fetchOneById(collaboratorId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Collaborator not found")

        if (collaborator.voteId != voteId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Collaborator does not belong to this vote")
        }

        voteCollaboratorsDao.delete(collaborator)
    }

    fun getCollaborators(voteId: Long): List<CollaboratorDto> {
        // Verify vote exists
        votesDao.fetchOneById(voteId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Vote not found")

        val collaborators = voteCollaboratorsDao.fetchByVoteId(voteId)

        return collaborators.mapNotNull { collaborator ->
            val user = userDao.findById(collaborator.userId!!)
            user?.let {
                CollaboratorDto(
                    id = collaborator.id!!,
                    userId = it.id,
                    username = it.username,
                    email = it.email,
                )
            }
        }
    }

    fun isCollaborator(
        voteId: Long,
        userId: UUID,
    ): Boolean {
        val collaborators = voteCollaboratorsDao.fetchByVoteId(voteId)
        return collaborators.any { it.userId == userId }
    }

    fun canModifyVote(
        voteId: Long,
        userId: UUID,
    ): Boolean {
        val vote =
            votesDao.fetchOneById(voteId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Vote not found")

        return vote.creatorId == userId || isCollaborator(voteId, userId)
    }

    fun getVoteIdsWhereUserIsCollaborator(userId: UUID): Set<Long> {
        val collaborators = voteCollaboratorsDao.fetchByUserId(userId)
        return collaborators.mapNotNull { it.voteId }.toSet()
    }
}
