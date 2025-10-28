package er.codes.saltyvote.history.model.events

import er.codes.saltyvote.history.model.HistoryEvent
import er.codes.saltyvote.jooq.enums.ActionType
import er.codes.saltyvote.jooq.tables.pojos.VoteSubmissionEntries
import java.util.UUID

class VoteEntryCreatedEvent(
    private val entryVoteId: Long,
    private val entryUserId: UUID,
    entry: VoteSubmissionEntries,
    submissionId: Long,
    voteId: Long,
    userId: UUID,
) : HistoryEvent(
        actionType = ActionType.VOTE_ENTRY_CREATED,
        userId = userId,
        eventData =
            VoteEntryCreatedEventData(
                entryId = entry.id!!,
                submissionId = submissionId,
                voteId = voteId,
                optionId = entry.optionId!!,
                rank = entry.rank,
                selected = entry.selected,
            ),
    ) {
    constructor(
        entry: VoteSubmissionEntries,
        submissionId: Long,
        voteId: Long,
        userId: UUID,
    ) : this(
        entryVoteId = voteId,
        entryUserId = userId,
        entry = entry,
        submissionId = submissionId,
        voteId = voteId,
        userId = userId,
    )

    override fun toLog() = "Vote entry created for voteId $entryVoteId by user $entryUserId"

    private data class VoteEntryCreatedEventData(
        val entryId: Long,
        val submissionId: Long,
        val voteId: Long,
        val optionId: Long,
        val rank: Int?,
        val selected: Boolean?,
    )
}
