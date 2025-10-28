package er.codes.saltyvote.scrape.service

import com.fasterxml.jackson.databind.ObjectMapper
import er.codes.saltyvote.jooq.tables.daos.LocalPictureStoreDao
import er.codes.saltyvote.jooq.tables.daos.VoteOptionExternalDataDao
import er.codes.saltyvote.jooq.tables.pojos.LocalPictureStore
import er.codes.saltyvote.jooq.tables.pojos.VoteOptionExternalData
import er.codes.saltyvote.scrape.ScrapeStorageProperties
import er.codes.saltyvote.scrape.model.ScrapeDataEvent
import er.codes.saltyvote.scrape.model.ScrapeResult
import io.github.oshai.kotlinlogging.KotlinLogging
import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.html5.script
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Service
import java.io.File
import java.net.URL

@Service
class ScrapeService(
    private val objectMapper: ObjectMapper,
    private val voteOptionExternalDataDao: VoteOptionExternalDataDao,
    private val pictureStoreDao: LocalPictureStoreDao,
    private val storageProperties: ScrapeStorageProperties,
) {
    private val log = KotlinLogging.logger { }

    fun scrape(target: ScrapeDataEvent): ScrapeResult {
        val data =
            try {
                skrape(HttpFetcher) {
                    request {
                        url = target.targetUrl
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

                                        return@htmlDocument json.decodeFromString<StayEmbedData>(jsonStr)
                                    }
                                }
                            }

                            null
                        }
                    }
                }
            } catch (e: Exception) {
                log.error(e) { "Error scraping for target url ${target.targetUrl}" }
                return ScrapeResult.failure("Scraping failed: ${e.message}")
            }

        if (data == null) {
            log.warn { "No StayEmbedData found for target url ${target.targetUrl}" }
            return ScrapeResult.failure("No StayEmbedData found on page")
        }

        return try {
            val picture = downloadPicture(target.voteOptionId, data.pictureUrl)

            if (picture == null) {
                log.error { "Failed to download picture for vote option ${target.voteOptionId}" }
                return ScrapeResult.failure("Picture download failed")
            }

            val localPictureStore = LocalPictureStore(localPath = picture.absolutePath)
            pictureStoreDao.insert(localPictureStore)

            voteOptionExternalDataDao.insert(
                VoteOptionExternalData(
                    voteOptionId = target.voteOptionId,
                    sourceUrl = target.targetUrl,
                    airbnbTitle = data.name,
                    airbnbReviewCount = data.reviewCount.toBigDecimal(),
                    airbnbStarRating = data.starRating.toBigDecimal(),
                    airbnbPictureUrl = data.pictureUrl,
                    airbnbPictureLocalId = localPictureStore.id,
                    rawPayload = org.jooq.JSONB.valueOf(objectMapper.writeValueAsString(data)),
                ),
            )

            log.info { "Successfully scraped and stored data for vote option ${target.voteOptionId}" }
            ScrapeResult.success()
        } catch (e: Exception) {
            log.error(e) { "Error storing scraped data for vote option ${target.voteOptionId}" }
            ScrapeResult.failure("Data storage failed: ${e.message}")
        }
    }

    private fun downloadPicture(
        voteOptionId: Long,
        pictureUrl: String,
    ): File? {
        try {
            // Ensure upload directory exists
            val downloadPath = storageProperties.getDownloadPath().toFile()
            if (!downloadPath.exists()) {
                downloadPath.mkdirs()
                log.info { "Created airbnbPic directory: ${downloadPath.absolutePath}" }
            }

            val fileName = "property_$voteOptionId.png"
            val file = File(downloadPath, fileName)

            log.info { "Downloading image to: ${file.absolutePath}" }
            URL(pictureUrl).openStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            log.info { "Image downloaded successfully!" }
            return file
        } catch (e: Exception) {
            log.error { "Error downloading picture for vote option $voteOptionId" }
            return null
        }
    }
}

@Serializable
data class StayEmbedData(
    val __typename: String,
    val id: String,
    val name: String,
    val pictureUrl: String,
    val starRating: Double,
    val reviewCount: Int,
)
