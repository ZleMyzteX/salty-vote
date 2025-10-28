package er.codes.saltyvote.scrape.model

data class ScrapeResult(
    val success: Boolean,
    val error: String? = null,
) {
    companion object {
        fun success() = ScrapeResult(success = true)

        fun failure(error: String?) = ScrapeResult(success = false, error = error)
    }
}
