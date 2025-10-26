package er.codes.saltyvote.vote.model

data class VoteOptionDto(
    val label: String,
    val preDefinedPosition: Int? = null,
)

data class AirbnbVoteOptionDto(
    val label: String,
    val description: String,
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
            totalPrice,
            country,
            flightNeeded,
            airbnbPrice,
            airbnbLink,
        )
}
