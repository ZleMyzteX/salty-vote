package er.codes.saltyvote.vote.service

import com.fasterxml.jackson.databind.ObjectMapper
import er.codes.saltyvote.auth.model.UserEntity
import er.codes.saltyvote.jooq.tables.daos.VoteOptionsDao
import er.codes.saltyvote.jooq.tables.daos.VotesDao
import er.codes.saltyvote.jooq.tables.pojos.VoteOptions
import er.codes.saltyvote.jooq.tables.pojos.Votes
import er.codes.saltyvote.vote.model.AirbnbVoteOptionDto
import er.codes.saltyvote.vote.model.VoteCreationDto
import er.codes.saltyvote.vote.model.VoteDetailDto
import er.codes.saltyvote.vote.model.VoteOptionDto
import org.jooq.JSONB
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class VoteCreationService(
    private val voteDao: VotesDao,
    private val voteOptionsDao: VoteOptionsDao,
    private val objectMapper: ObjectMapper,
) {
    fun createVote(req: VoteCreationDto) {
        voteDao.insert(
            Votes(
                title = req.title,
                creatorId = (SecurityContextHolder.getContext().authentication.principal as UserEntity).id,
                description = req.description,
                voteType = req.voteType,
                allowMulti = req.allowMultiVote,
                allowAnonymous = req.allowAnonymousVote,
            ),
        )
    }

    fun createVoteOption(
        voteId: Long,
        req: VoteOptionDto,
    ) {
        val vote = voteDao.findById(voteId)

        voteOptionsDao.insert(
            VoteOptions(
                voteId = vote!!.id,
                label = req.label,
                position = req.preDefinedPosition,
            ),
        )
    }

    fun createVoteOption(
        voteId: Long,
        req: AirbnbVoteOptionDto,
    ) {
        val vote = voteDao.findById(voteId)

        voteOptionsDao.insert(
            VoteOptions(
                voteId = vote!!.id,
                label = req.label,
                data =
                    JSONB.valueOf(
                        objectMapper.writeValueAsString(
                            req.getAirbnbData(),
                        ),
                    ),
                position = req.predefinedPosition,
            ),
        )
    }

    fun getAllVotes(): List<VoteDetailDto> =
        voteDao.findAll().map { votes ->
            VoteDetailDto(
                id = votes.id!!,
                title = votes.title!!,
                description = votes.description,
                voteType = votes.voteType!!,
                allowMultiVote = votes.allowMulti!!,
                allowAnonymousVote = votes.allowAnonymous!!,
                done = votes.done!!,
                options =
                    voteOptionsDao.fetchByVoteId(votes.id!!).map { options ->
                        VoteOptionDto(
                            label = options.label!!,
                            preDefinedPosition = options.position,
                        )
                    },
            )
        }
}
