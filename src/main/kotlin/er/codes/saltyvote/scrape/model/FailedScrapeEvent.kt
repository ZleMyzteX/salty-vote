package er.codes.saltyvote.scrape.model

import java.time.LocalDateTime

data class FailedScrapeEvent(
    val scrapeDataEvent: ScrapeDataEvent,
    val failureCount: Int = 0,
    val lastAttempt: LocalDateTime = LocalDateTime.now(),
    val lastError: String? = null,
)
