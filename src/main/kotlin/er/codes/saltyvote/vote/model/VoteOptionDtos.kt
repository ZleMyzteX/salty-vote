package er.codes.saltyvote.vote.model

// TODO: for later generic voting capabilities
data class VoteOptionDto(
    val id: Long,
    val label: String,
    val preDefinedPosition: Int? = null,
)

data class AirbnbVoteOptionDto(
    val label: String,
    val description: String,
    val benefits: String,
    val disadvantages: String,
    val travelTime: Float,
    val totalPrice: Float,
    val country: String,
    val flightNeeded: Boolean,
    val airbnbPrice: Float,
    val airbnbLink: String,
    val predefinedPosition: Int? = null,
) {
    fun getAirbnbData() =
        AirbnbVoteOptionData(
            description,
            benefits,
            disadvantages,
            travelTime,
            totalPrice,
            country,
            flightNeeded,
            airbnbPrice,
            airbnbLink,
        )
}
