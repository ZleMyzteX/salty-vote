package er.codes.saltyvote.vote.model

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

data class UpdateVoteOptionDto(
    val label: String,
    val description: String,
    val benefits: String,
    val disadvantages: String,
    val travelTime: Float,
    val totalPrice: Float,
    val country: String,
    val flightNeeded: Boolean,
    val airbnbPrice: Float,
)
