package er.codes.saltyvote.vote.model

import er.codes.saltyvote.jooq.enums.VoteType

data class VoteCreationDto(
    val title: String,
    val description: String?,
    val voteType: VoteType,
    val allowMultiVote: Boolean,
    val allowAnonymousVote: Boolean,
    val done: Boolean,
)

data class VoteDetailDto(
    val id: Long,
    val title: String,
    val description: String?,
    val voteType: VoteType,
    val allowMultiVote: Boolean,
    val allowAnonymousVote: Boolean,
    val done: Boolean,
    val options: List<VoteOptionDto>,
)

data class VoteListDto(
    val id: Long,
    val title: String,
    val description: String?,
)
