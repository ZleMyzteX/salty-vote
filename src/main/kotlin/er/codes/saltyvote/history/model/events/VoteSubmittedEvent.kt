package er.codes.saltyvote.history.model.events

import er.codes.saltyvote.history.model.HistoryEvent
import er.codes.saltyvote.jooq.enums.ActionType
import er.codes.saltyvote.jooq.tables.pojos.VoteSubmissions
import java.util.UUID

class VoteSubmittedEvent(
    private val submitterId: UUID,
    private val voteId: Long,
    submission: VoteSubmissions,
    entries: List<VoteSubmissionEntryData>,
) : HistoryEvent(
        actionType = ActionType.VOTE_SUBMITTED,
        userId = submission.userId,
        eventData =
            VoteSubmittedEventData(
                submissionId = submission.id!!,
                voteId = submission.voteId!!,
                userId = submission.userId!!,
                entries = entries,
            ),
    ) {
    constructor(submission: VoteSubmissions, entries: List<VoteSubmissionEntryData>) : this(
        submitterId = submission.userId!!,
        voteId = submission.voteId!!,
        submission = submission,
        entries = entries,
    )

    override fun toLog() = "User $submitterId submitted vote for voteId $voteId"

    data class VoteSubmittedEventData(
        val submissionId: Long,
        val voteId: Long,
        val userId: UUID,
        val entries: List<VoteSubmissionEntryData>,
    )
}

data class VoteSubmissionEntryData(
    val optionId: Long,
    val rank: Int?,
    val selected: Boolean?,
)
