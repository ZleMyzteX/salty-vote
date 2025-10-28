package er.codes.saltyvote.vote.model

import er.codes.saltyvote.jooq.enums.VoteType
import java.util.UUID

data class VoteSubmissionDto(
    val voteId: Long,
    val entries: List<VoteSubmissionEntryDto>,
)

data class VoteSubmissionEntryDto(
    val optionId: Long,
    val rank: Int? = null,
    val selected: Boolean? = null,
)

data class VoteSubmissionResponseDto(
    val id: Long,
    val voteId: Long,
    val userId: UUID,
    val entries: List<VoteSubmissionEntryDto>,
)

data class AddCollaboratorDto(
    val userId: UUID,
)

data class CollaboratorDto(
    val id: Long,
    val userId: UUID,
    val username: String,
    val email: String,
)

data class VoteWithAirbnbOptionsDto(
    val id: Long,
    val title: String,
    val description: String?,
    val voteType: VoteType,
    val allowMultiVote: Boolean,
    val allowAnonymousVote: Boolean,
    val done: Boolean,
    val isCreator: Boolean,
    val isCollaborator: Boolean,
    val options: List<AirbnbVoteOptionResponseDto>,
)

data class AirbnbVoteOptionResponseDto(
    val id: Long,
    val label: String,
    val data: AirbnbVoteOptionData?,
    val preDefinedPosition: Int? = null,
)

data class VoteResultsDto(
    val voteId: Long,
    val title: String,
    val totalSubmissions: Int,
    val results: List<VoteOptionResultDto>,
)

data class VoteOptionResultDto(
    val optionId: Long,
    val label: String,
    val voteCount: Int,
    val percentage: Double,
    val averageRank: Double?,
)

data class IndividualVoterDto(
    val userId: UUID,
    val email: String,
    val username: String,
    val submittedAt: java.time.OffsetDateTime,
    val rankings: List<VoterRankingDto>,
)

data class VoterRankingDto(
    val optionId: Long,
    val optionLabel: String,
    val rank: Int?,
    val selected: Boolean?,
)

data class VoteDetailedResultsDto(
    val voteId: Long,
    val title: String,
    val voteType: VoteType,
    val totalSubmissions: Int,
    val results: List<VoteOptionResultDto>,
    val individualVoters: List<IndividualVoterDto>,
)
