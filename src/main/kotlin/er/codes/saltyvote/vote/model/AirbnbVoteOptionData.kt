package er.codes.saltyvote.vote.model

data class AirbnbVoteOptionData(
    val description: String,
    val benefits: String,
    val disadvantages: String,
    val travelTime: Float,
    val totalPrice: Float,
    val country: String,
    val flightNeeded: Boolean,
    val airbnbPrice: Float,
    val airbnbLink: String,
)
