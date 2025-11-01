package er.codes.saltyvote.scrape.scrapers

import er.codes.saltyvote.scrape.ScrapeStorageProperties
import er.codes.saltyvote.scrape.model.ScrapeDataEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import org.springframework.stereotype.Component
import java.io.File

@Component
class NovasolScraper(
    private val storageProperties: ScrapeStorageProperties,
) : ScraperStrategy {
    private val log = KotlinLogging.logger { }

    override fun canHandle(url: String): Boolean =
        url.contains("novasol.com", ignoreCase = true) ||
            url.contains("novasol.", ignoreCase = true)

    override fun scrape(event: ScrapeDataEvent): ScrapedData? =
        try {
            skrape(HttpFetcher) {
                request {
                    url = event.targetUrl
                    userAgent =
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36" +
                        " (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                }
                response {
                    htmlDocument {
                        // Extract property name from h1 (with fallback)
                        val propertyName =
                            try {
                                findFirst("h1").text.trim()
                            } catch (e: Exception) {
                                log.warn { "Could not find h1, trying title..." }
                                try {
                                    findFirst("title").text.trim()
                                } catch (e: Exception) {
                                    "Unknown Property"
                                }
                            }

                        // Extract first picture from hero carousel using data-testid
                        val pictureUrl =
                            try {
                                findFirst("[data-testid='hero-carousel-responsive-gallery'] img").attribute("src")
                            } catch (e: Exception) {
                                log.warn { "Could not find hero carousel image, trying fallback selectors" }
                                try {
                                    findFirst("img[data-src]").attribute("data-src")
                                } catch (_: Exception) {
                                    try {
                                        findFirst(".gallery img, .property-image img, img[alt*='property']")
                                            .attribute("src")
                                    } catch (_: Exception) {
                                        log.warn { "Using first image as fallback" }
                                        try {
                                            findFirst("img").attribute("src")
                                        } catch (_: Exception) {
                                            ""
                                        }
                                    }
                                }
                            }

                        // Extract star rating - try multiple selectors
                        val starRating =
                            try {
                                // Try method 1: Look for data-testid="star-rating" with aria-label
                                try {
                                    val ariaLabel =
                                        findFirst("[data-testid='star-rating']")
                                            .attribute("aria-label")
                                    log.debug { "Found aria-label on star-rating: $ariaLabel" }
                                    // Extract number from "X out of 5 stars" or "X von 5 Sternen"
                                    val ratingMatch =
                                        Regex("""(\d+(?:\.\d+)?)\s+(?:out of|von)\s+5""")
                                            .find(ariaLabel)
                                    val rating = ratingMatch?.groupValues?.get(1)?.toDoubleOrNull()
                                    if (rating != null) {
                                        log.debug { "Extracted rating from aria-label: $rating" }
                                        rating
                                    } else {
                                        null
                                    }
                                } catch (e: Exception) {
                                    log.debug { "Method 1 failed (data-testid='star-rating')" }
                                    null
                                }
                                    ?: // Try method 2: Look for .top-line-stars-score-text class
                                    try {
                                        val text = findFirst(".top-line-stars-score-text").text.trim()
                                        log.debug { "Found .top-line-stars-score-text: $text" }
                                        text.toDoubleOrNull()
                                    } catch (e: Exception) {
                                        log.debug { "Method 2 failed (.top-line-stars-score-text)" }
                                        null
                                    }
                                    ?: // Try method 3: Look for any element with rating in aria-label
                                    try {
                                        val ratingElements = findAll("[aria-label*='rating']")
                                        log.debug {
                                            "Found ${ratingElements.size} elements with 'rating' in aria-label"
                                        }
                                        ratingElements.firstOrNull()?.let { element ->
                                            val label = element.attribute("aria-label")
                                            log.debug { "First rating aria-label: $label" }
                                            Regex("""(\d+(?:\.\d+)?)\s+(?:out of|von)\s+5""")
                                                .find(label)
                                                ?.groupValues
                                                ?.get(1)
                                                ?.toDoubleOrNull()
                                        }
                                    } catch (e: Exception) {
                                        log.debug { "Method 3 failed (aria-label*='rating')" }
                                        null
                                    }
                                    ?: // Try method 4: Look for rating/review text patterns
                                    try {
                                        val ratingText =
                                            findFirst(
                                                ".rating, [class*='rating'], [itemprop='ratingValue']",
                                            ).text
                                        ratingText.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
                                    } catch (e: Exception) {
                                        log.debug { "Method 4 failed (standard rating selectors)" }
                                        null
                                    } ?: run {
                                    log.warn {
                                        "Could not find star rating with any method for ${event.targetUrl}"
                                    }
                                    0.0
                                }
                            } catch (e: Exception) {
                                log.warn(e) {
                                    "Error extracting star rating for ${event.targetUrl}"
                                }
                                0.0
                            }

                        // Extract review count - looking for text like "2 reviews"
                        val reviewCount =
                            try {
                                // Try specific selectors first
                                try {
                                    val reviewText =
                                        findFirst(
                                            ".reviews, [class*='review'], [itemprop='reviewCount']",
                                        ).text
                                    reviewText.replace(Regex("[^0-9]"), "").toIntOrNull()
                                } catch (e: Exception) {
                                    log.debug { "Could not find review count with specific selectors" }
                                    null
                                }
                                    ?: // Fallback: search all text for "X reviews" pattern
                                    try {
                                        val allText = findAll("*").map { it.text }.joinToString(" ")
                                        Regex("""(\d+)\s+reviews?""", RegexOption.IGNORE_CASE)
                                            .find(allText)
                                            ?.groupValues
                                            ?.getOrNull(1)
                                            ?.toIntOrNull()
                                    } catch (e: Exception) {
                                        log.debug { "Could not extract review count from text search" }
                                        null
                                    } ?: 0
                            } catch (e: Exception) {
                                log.warn { "Could not extract review count for ${event.targetUrl}" }
                                0
                            }

                        log.info {
                            "Extracted data for ${event.targetUrl}: name=$propertyName, rating=$starRating, reviews=$reviewCount"
                        }

                        ScrapedData(
                            name = propertyName,
                            pictureUrl = pictureUrl,
                            starRating = starRating,
                            reviewCount = reviewCount,
                        )
                    }
                }
            }
        } catch (e: Exception) {
            log.error(e) { "Error scraping Novasol for target url ${event.targetUrl}" }
            null
        }

    override fun retryPictureDownload(
        voteOptionId: Long,
        pictureUrl: String,
    ): File? =
        try {
            // Ensure upload directory exists
            val downloadPath = storageProperties.getDownloadPath().toFile()
            if (!downloadPath.exists()) {
                downloadPath.mkdirs()
                log.info { "Created download directory: ${downloadPath.absolutePath}" }
            }

            val fileName = "property_$voteOptionId.png"
            val file = File(downloadPath, fileName)

            log.info { "Downloading Novasol image to: ${file.absolutePath}" }

            // Create connection with proper headers to avoid 403
            val url = java.net.URL(pictureUrl)
            val connection = url.openConnection()
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)" +
                    " AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            )
            connection.setRequestProperty("Referer", "https://www.novasol.at/")
            connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")

            connection.getInputStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            log.info { "Novasol image downloaded successfully!" }
            file
        } catch (e: Exception) {
            log.error(e) { "Error downloading Novasol picture for vote option $voteOptionId" }
            null
        }
}
