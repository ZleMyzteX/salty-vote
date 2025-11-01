package er.codes.saltyvote.scrape.scrapers

import er.codes.saltyvote.scrape.ScrapeStorageProperties
import er.codes.saltyvote.scrape.model.ScrapeDataEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.script
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Component
import java.io.File
import java.net.URL

@Component
class AirbnbScraper(
    private val storageProperties: ScrapeStorageProperties,
) : ScraperStrategy {
    private val log = KotlinLogging.logger { }

    override fun canHandle(url: String): Boolean =
        url.contains("airbnb.com", ignoreCase = true) || url.contains("airbnb.", ignoreCase = true)

    override fun scrape(event: ScrapeDataEvent): ScrapedData? {
        return try {
            skrape(HttpFetcher) {
                request {
                    url = event.targetUrl
                    userAgent =
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                }
                response {
                    htmlDocument {
                        val scripts =
                            script {
                                findAll {
                                    this.map { it.html }
                                }
                            }

                        // Search through scripts for StayEmbedData
                        for (scriptContent in scripts) {
                            if (scriptContent.contains("StayEmbedData") &&
                                scriptContent.contains("\"__typename\":\"StayEmbedData\"")
                            ) {
                                val stayEmbedDataMatch =
                                    Regex(
                                        """(\{"__typename":"StayEmbedData"[^}]+\})""",
                                    ).find(scriptContent)
                                if (stayEmbedDataMatch != null) {
                                    val jsonStr = stayEmbedDataMatch.value
                                    val json = Json { ignoreUnknownKeys = true }

                                    val stayEmbedData = json.decodeFromString<StayEmbedData>(jsonStr)
                                    return@htmlDocument ScrapedData(
                                        name = stayEmbedData.name,
                                        pictureUrl = stayEmbedData.pictureUrl,
                                        starRating = stayEmbedData.starRating,
                                        reviewCount = stayEmbedData.reviewCount,
                                    )
                                }
                            }
                        }

                        null
                    }
                }
            }
        } catch (e: Exception) {
            log.error(e) { "Error scraping Airbnb for target url ${event.targetUrl}" }
            null
        }
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
                log.info { "Created airbnbPic directory: ${downloadPath.absolutePath}" }
            }

            val fileName = "property_$voteOptionId.png"
            val file = File(downloadPath, fileName)

            log.info { "Downloading Airbnb image to: ${file.absolutePath}" }
            URL(pictureUrl).openStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            log.info { "Airbnb image downloaded successfully!" }
            file
        } catch (e: Exception) {
            log.error(e) { "Error downloading Airbnb picture for vote option $voteOptionId" }
            null
        }
}

@Serializable
private data class StayEmbedData(
    val __typename: String,
    val id: String,
    val name: String,
    val pictureUrl: String,
    val starRating: Double,
    val reviewCount: Int,
)
