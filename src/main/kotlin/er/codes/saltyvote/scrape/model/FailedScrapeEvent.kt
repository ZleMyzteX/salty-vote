package er.codes.saltyvote.scrape.model

import java.time.LocalDateTime

enum class FailureType {
    /** Full scraping failed - need to re-scrape all data */
    FULL_SCRAPE_FAILED,

    /** Only picture download failed - data exists but picture is missing */
    PICTURE_DOWNLOAD_FAILED,
}

data class FailedScrapeEvent(
    val scrapeDataEvent: ScrapeDataEvent,
    val failureCount: Int = 0,
    val lastAttempt: LocalDateTime = LocalDateTime.now(),
    val lastError: String? = null,
    val failureType: FailureType = FailureType.FULL_SCRAPE_FAILED,
)
