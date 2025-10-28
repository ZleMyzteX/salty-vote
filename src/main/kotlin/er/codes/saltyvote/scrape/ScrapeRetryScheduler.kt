package er.codes.saltyvote.scrape

import er.codes.saltyvote.scrape.service.ScrapeRetryService
import er.codes.saltyvote.scrape.service.ScrapeService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class ScrapeRetryScheduler(
    private val scrapeService: ScrapeService,
    private val retryService: ScrapeRetryService,
) {
    private val log = KotlinLogging.logger { }

    /**
     * Retry failed scrapes every 5 minutes
     */
    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    fun retryFailedScrapes() {
        val failedEvents = retryService.getFailedScrapesForRetry()

        if (failedEvents.isEmpty()) {
            log.debug { "No failed scrapes to retry" }
            return
        }

        log.info { "Starting retry of ${failedEvents.size} failed scrape(s)" }

        failedEvents.forEach { failedEvent ->
            val event = failedEvent.scrapeDataEvent
            log.info {
                "Retrying scrape for vote option ${event.voteOptionId} (attempt ${failedEvent.failureCount + 1})"
            }

            try {
                val result = scrapeService.scrape(event)
                if (result.success) {
                    retryService.removeSuccessfulScrape(event.voteOptionId)
                    log.info { "Successfully scraped vote option ${event.voteOptionId} on retry" }
                } else {
                    retryService.addFailedScrape(event, result.error)
                    log.warn { "Retry failed for vote option ${event.voteOptionId}: ${result.error}" }
                }
            } catch (e: Exception) {
                retryService.addFailedScrape(event, e.message)
                log.error(e) { "Exception during retry for vote option ${event.voteOptionId}" }
            }
        }

        val stats = retryService.getStatistics()
        log.info {
            "Retry complete. Failed scrapes still pending: ${stats["totalFailed"]}, Max retries: ${stats["maxRetries"]}, Avg retries: ${stats["avgRetries"]}"
        }
    }
}
