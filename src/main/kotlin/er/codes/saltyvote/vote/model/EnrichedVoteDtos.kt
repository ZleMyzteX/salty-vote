package er.codes.saltyvote.vote.model

import java.math.BigDecimal

/**
 * DTO for scraped external Airbnb data
 */
data class AirbnbExternalDataDto(
    val title: String?,
    val reviewCount: BigDecimal?,
    val starRating: BigDecimal?,
    val pictureUrl: String?,
    val hasPictureDownloaded: Boolean = false,
    val sourceUrl: String?,
)

/**
 * Enhanced vote option response that includes both user-provided data and scraped external data
 */
data class AirbnbVoteOptionEnrichedDto(
    val id: Long,
    val label: String,
    val data: AirbnbVoteOptionData?,
    val externalData: AirbnbExternalDataDto?,
    val preDefinedPosition: Int? = null,
)

/**
 * Enhanced vote response with external Airbnb data
 */
data class VoteWithEnrichedAirbnbOptionsDto(
    val id: Long,
    val title: String,
    val description: String?,
    val voteType: er.codes.saltyvote.jooq.enums.VoteType,
    val allowMultiVote: Boolean,
    val allowAnonymousVote: Boolean,
    val done: Boolean,
    val isCreator: Boolean,
    val isCollaborator: Boolean,
    val options: List<AirbnbVoteOptionEnrichedDto>,
)
