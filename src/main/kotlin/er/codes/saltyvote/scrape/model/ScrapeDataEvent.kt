package er.codes.saltyvote.scrape.model

data class ScrapeDataEvent(
    val targetUrl: String,
    val voteOptionId: Long,
)
