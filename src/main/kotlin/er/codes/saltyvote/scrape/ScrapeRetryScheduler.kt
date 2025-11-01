package er.codes.saltyvote.scrape

import er.codes.saltyvote.scrape.model.FailureType
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
                "Retrying scrape for vote option ${event.voteOptionId} (attempt ${failedEvent.failureCount + 1}, type: ${failedEvent.failureType})"
            }

            try {
                val result =
                    when (failedEvent.failureType) {
                        FailureType.PICTURE_DOWNLOAD_FAILED -> {
                            // Only retry picture download, don't re-scrape all data
                            log.info { "Retrying picture download only for vote option ${event.voteOptionId}" }
                            scrapeService.retryPictureDownload(event.voteOptionId)
                        }
                        FailureType.FULL_SCRAPE_FAILED -> {
                            // Full scrape retry needed
                            log.info { "Retrying full scrape for vote option ${event.voteOptionId}" }
                            scrapeService.scrape(event)
                        }
                    }

                if (result.success) {
                    retryService.removeSuccessfulScrape(event.voteOptionId)
                    log.info { "Successfully processed vote option ${event.voteOptionId} on retry" }
                } else {
                    // Determine new failure type based on result
                    val newFailureType =
                        if (result.error?.contains("picture", ignoreCase = true) == true) {
                            FailureType.PICTURE_DOWNLOAD_FAILED
                        } else {
                            FailureType.FULL_SCRAPE_FAILED
                        }
                    retryService.addFailedScrape(event, result.error, newFailureType)
                    log.warn { "Retry failed for vote option ${event.voteOptionId}: ${result.error}" }
                }
            } catch (e: Exception) {
                retryService.addFailedScrape(event, e.message, failedEvent.failureType)
                log.error(e) { "Exception during retry for vote option ${event.voteOptionId}" }
            }
        }

        val stats = retryService.getStatistics()
        log.info {
            "Retry complete. Failed scrapes still pending: ${stats["totalFailed"]}, Max retries: ${stats["maxRetries"]}, Avg retries: ${stats["avgRetries"]}"
        }
    }
}
