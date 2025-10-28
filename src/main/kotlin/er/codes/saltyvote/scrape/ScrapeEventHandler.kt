package er.codes.saltyvote.scrape

import er.codes.saltyvote.scrape.model.ScrapeDataEvent
import er.codes.saltyvote.scrape.service.ScrapeRetryService
import er.codes.saltyvote.scrape.service.ScrapeService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class ScrapeEventHandler(
    private val scrapeService: ScrapeService,
    private val retryService: ScrapeRetryService,
) {
    private val log = KotlinLogging.logger { }

    @Async("scrapeTaskExecutor")
    @EventListener
    fun handleScrapeEvent(event: ScrapeDataEvent): CompletableFuture<Void> {
        log.info {
            "Got a scrape event for vote option id ${event.voteOptionId}" +
                "\n Target Url: ${event.targetUrl}"
        }

        try {
            val result = scrapeService.scrape(event)

            if (result.success) {
                log.info { "Successfully scraped vote option ${event.voteOptionId}" }
                // Remove from retry queue if it was there from a previous failure
                retryService.removeSuccessfulScrape(event.voteOptionId)
            } else {
                log.warn { "Scrape failed for vote option ${event.voteOptionId}: ${result.error}" }
                retryService.addFailedScrape(event, result.error)
            }
        } catch (e: Exception) {
            log.error(e) { "Exception while handling scrape event for vote option ${event.voteOptionId}" }
            retryService.addFailedScrape(event, e.message)
        }

        return CompletableFuture.completedFuture(null)
    }
}
