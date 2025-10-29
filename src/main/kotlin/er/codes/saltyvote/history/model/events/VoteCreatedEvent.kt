package er.codes.saltyvote.history.model.events

import er.codes.saltyvote.history.model.HistoryEvent
import er.codes.saltyvote.jooq.enums.ActionType
import er.codes.saltyvote.jooq.enums.VoteType
import er.codes.saltyvote.jooq.tables.pojos.VoteOptions
import er.codes.saltyvote.jooq.tables.pojos.Votes
import org.jooq.JSONB
import java.util.UUID

class VoteCreatedEvent(
    private val voteTitle: String,
    private val voteId: Long,
    private val creatorId: UUID,
    vote: Votes,
    options: List<VoteOptions> = emptyList(),
) : HistoryEvent(
        actionType = ActionType.VOTE_CREATED,
        userId = vote.creatorId,
        eventData =
            VoteCreatedEventData(
                voteId = vote.id!!,
                title = vote.title!!,
                description = vote.description,
                creatorId = vote.creatorId!!,
                voteType = vote.voteType!!,
                allowMulti = vote.allowMulti!!,
                allowAnonymous = vote.allowAnonymous!!,
                done = vote.done!!,
                options =
                    options.map { option ->
                        VoteOptionEventData(
                            id = option.id!!,
                            label = option.label!!,
                            data = option.data,
                        )
                    },
            ),
    ) {
    constructor(vote: Votes) : this(
        voteTitle = vote.title!!,
        voteId = vote.id!!,
        creatorId = vote.creatorId!!,
        vote = vote,
        options = emptyList(),
    )

    override fun toLog() = "Vote '$voteTitle' (id: $voteId) created by user $creatorId"

    private data class VoteCreatedEventData(
        val voteId: Long,
        val title: String,
        val description: String?,
        val creatorId: UUID,
        val voteType: VoteType,
        val allowMulti: Boolean,
        val allowAnonymous: Boolean,
        val done: Boolean,
        val options: List<VoteOptionEventData>,
    )

    // TODO: broken
    private data class VoteOptionEventData(
        val id: Long,
        val label: String,
        val data: JSONB?,
    )
}
