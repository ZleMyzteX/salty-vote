package er.codes.saltyvote.scrape.scrapers

import er.codes.saltyvote.scrape.model.ScrapeDataEvent

/**
 * Common interface for all scraper implementations.
 * Each scraper extracts property data from a specific website.
 */
interface ScraperStrategy {
    /**
     * Scrapes property data from the given URL.
     * @return ScrapedData containing the extracted information, or null if scraping failed
     */
    fun scrape(event: ScrapeDataEvent): ScrapedData?

    /**
     * Checks if this scraper can handle the given URL.
     */
    fun canHandle(url: String): Boolean

    /**
     * Retry downloading the picture for a vote option that already has external data.
     * This is called when the initial scrape succeeded but picture download failed.
     * @return the downloaded file, or null if download failed
     */
    fun retryPictureDownload(
        voteOptionId: Long,
        pictureUrl: String,
    ): java.io.File?
}

/**
 * Common data structure for scraped property information
 */
data class ScrapedData(
    val name: String,
    val pictureUrl: String,
    val starRating: Double,
    val reviewCount: Int,
)
