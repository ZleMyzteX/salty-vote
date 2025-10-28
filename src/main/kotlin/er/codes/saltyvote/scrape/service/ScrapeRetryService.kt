package er.codes.saltyvote.scrape.service

import er.codes.saltyvote.scrape.model.FailedScrapeEvent
import er.codes.saltyvote.scrape.model.ScrapeDataEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class ScrapeRetryService {
    private val log = KotlinLogging.logger { }

    // Using ConcurrentHashMap for thread-safe operations
    // Key: voteOptionId to prevent duplicates
    private val failedScrapes = ConcurrentHashMap<Long, FailedScrapeEvent>()

    /**
     * Add a failed scrape event to the retry queue
     */
    fun addFailedScrape(
        event: ScrapeDataEvent,
        error: String?,
    ) {
        val voteOptionId = event.voteOptionId

        failedScrapes.compute(voteOptionId) { _, existing ->
            if (existing == null) {
                log.warn { "Adding scrape event to retry queue for vote option $voteOptionId (first failure)" }
                FailedScrapeEvent(
                    scrapeDataEvent = event,
                    failureCount = 1,
                    lastAttempt = LocalDateTime.now(),
                    lastError = error,
                )
            } else {
                log.warn {
                    "Incrementing failure count for vote option $voteOptionId (attempt ${existing.failureCount + 1})"
                }
                existing.copy(
                    failureCount = existing.failureCount + 1,
                    lastAttempt = LocalDateTime.now(),
                    lastError = error,
                )
            }
        }

        log.info { "Failed scrapes queue size: ${failedScrapes.size}" }
    }

    /**
     * Remove a successfully scraped event from the retry queue
     */
    fun removeSuccessfulScrape(voteOptionId: Long) {
        failedScrapes.remove(voteOptionId)?.let {
            log.info {
                "Removed vote option $voteOptionId from retry queue after successful scrape (had ${it.failureCount} previous failures)"
            }
        }
    }

    /**
     * Get all failed scrapes that are ready for retry
     */
    fun getFailedScrapesForRetry(): List<FailedScrapeEvent> = failedScrapes.values.toList()

    /**
     * Get the current size of the failed scrapes queue
     */
    fun getQueueSize(): Int = failedScrapes.size

    /**
     * Get statistics about failed scrapes
     */
    fun getStatistics(): Map<String, Any> {
        val events = failedScrapes.values.toList()
        return mapOf(
            "totalFailed" to events.size,
            "maxRetries" to (events.maxOfOrNull { it.failureCount } ?: 0),
            "avgRetries" to (events.map { it.failureCount }.average().takeIf { !it.isNaN() } ?: 0.0),
        )
    }
}
