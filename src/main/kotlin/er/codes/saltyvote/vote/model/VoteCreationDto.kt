package er.codes.saltyvote.vote.model

import er.codes.saltyvote.jooq.enums.VoteType

data class CreateAirbnbVoteDto(
    val title: String,
    val description: String?,
    val voteType: VoteType,
    val allowMultiVote: Boolean,
    val allowAnonymousVote: Boolean,
    val done: Boolean,
    val options: List<AirbnbVoteOptionDto>,
)

data class VoteListDto(
    val id: Long,
    val title: String,
    val description: String?,
)

data class VoteListWithRelationshipDto(
    val id: Long,
    val title: String,
    val description: String?,
    val isCreator: Boolean,
    val isCollaborator: Boolean,
)
