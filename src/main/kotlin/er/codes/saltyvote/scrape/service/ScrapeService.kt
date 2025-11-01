package er.codes.saltyvote.scrape.service

import com.fasterxml.jackson.databind.ObjectMapper
import er.codes.saltyvote.jooq.tables.daos.LocalPictureStoreDao
import er.codes.saltyvote.jooq.tables.daos.VoteOptionExternalDataDao
import er.codes.saltyvote.jooq.tables.pojos.LocalPictureStore
import er.codes.saltyvote.jooq.tables.pojos.VoteOptionExternalData
import er.codes.saltyvote.scrape.model.ScrapeDataEvent
import er.codes.saltyvote.scrape.model.ScrapeResult
import er.codes.saltyvote.scrape.scrapers.ScraperStrategy
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jooq.JSONB
import org.springframework.stereotype.Service

@Service
class ScrapeService(
    private val objectMapper: ObjectMapper,
    private val voteOptionExternalDataDao: VoteOptionExternalDataDao,
    private val pictureStoreDao: LocalPictureStoreDao,
    private val scrapers: List<ScraperStrategy>,
) {
    private val log = KotlinLogging.logger { }

    fun scrape(target: ScrapeDataEvent): ScrapeResult {
        // Select the appropriate scraper based on URL
        val scraper = scrapers.firstOrNull { it.canHandle(target.targetUrl) }

        if (scraper == null) {
            log.warn { "No scraper found for target url ${target.targetUrl}" }
            return ScrapeResult.failure("No scraper available for this URL")
        }

        log.info { "Using ${scraper.javaClass.simpleName} for ${target.targetUrl}" }

        val data = scraper.scrape(target)

        if (data == null) {
            log.warn { "Scraper failed to extract data for target url ${target.targetUrl}" }
            return ScrapeResult.failure("Scraper could not extract data from page")
        }

        return try {
            // Try to download picture using scraper's method
            val picture = scraper.retryPictureDownload(target.voteOptionId, data.pictureUrl)
            val pictureLocalId =
                if (picture != null) {
                    val localPictureStore = LocalPictureStore(localPath = picture.absolutePath)
                    pictureStoreDao.insert(localPictureStore)
                    localPictureStore.id
                } else {
                    log.warn {
                        "Failed to download picture for vote option ${target.voteOptionId}, saving data without picture"
                    }
                    null
                }

            voteOptionExternalDataDao.insert(
                VoteOptionExternalData(
                    voteOptionId = target.voteOptionId,
                    sourceUrl = target.targetUrl,
                    airbnbTitle = data.name,
                    airbnbReviewCount = data.reviewCount.toBigDecimal(),
                    airbnbStarRating = data.starRating.toBigDecimal(),
                    airbnbPictureUrl = data.pictureUrl,
                    airbnbPictureLocalId = pictureLocalId,
                    rawPayload = JSONB.valueOf(objectMapper.writeValueAsString(data)),
                ),
            )

            log.info { "Successfully scraped and stored data for vote option ${target.voteOptionId}" }

            // Return success but indicate if picture is missing
            if (pictureLocalId == null) {
                ScrapeResult.failure("Data saved but picture download failed")
            } else {
                ScrapeResult.success()
            }
        } catch (e: Exception) {
            log.error(e) { "Error storing scraped data for vote option ${target.voteOptionId}" }
            ScrapeResult.failure("Data storage failed: ${e.message}")
        }
    }

    fun retryPictureDownload(voteOptionId: Long): ScrapeResult {
        try {
            // Fetch existing external data
            val externalData = voteOptionExternalDataDao.fetchByVoteOptionId(voteOptionId).firstOrNull()

            if (externalData == null) {
                log.warn { "No external data found for vote option $voteOptionId" }
                return ScrapeResult.failure("No external data found for this vote option")
            }

            if (externalData.airbnbPictureLocalId != null) {
                log.info {
                    "Vote option $voteOptionId already has a picture (ID: ${externalData.airbnbPictureLocalId})"
                }
                return ScrapeResult.success()
            }

            val pictureUrl = externalData.airbnbPictureUrl
            if (pictureUrl.isNullOrBlank()) {
                log.warn { "No picture URL available for vote option $voteOptionId" }
                return ScrapeResult.failure("No picture URL available")
            }

            // Find the appropriate scraper based on source URL
            val sourceUrl = externalData.sourceUrl ?: ""
            val scraper = scrapers.firstOrNull { it.canHandle(sourceUrl) }

            if (scraper == null) {
                log.warn { "No scraper found for source URL: $sourceUrl" }
                return ScrapeResult.failure("No scraper available for this URL")
            }

            log.info { "Using ${scraper.javaClass.simpleName} to retry picture download for vote option $voteOptionId" }

            // Try to download the picture using scraper's method
            val picture = scraper.retryPictureDownload(voteOptionId, pictureUrl)

            if (picture == null) {
                log.error { "Failed to download picture for vote option $voteOptionId" }
                return ScrapeResult.failure("Picture download failed")
            }

            // Store the picture
            val localPictureStore = LocalPictureStore(localPath = picture.absolutePath)
            pictureStoreDao.insert(localPictureStore)

            // Update the external data with the picture ID
            externalData.airbnbPictureLocalId = localPictureStore.id
            voteOptionExternalDataDao.update(externalData)

            log.info { "Successfully downloaded and stored picture for vote option $voteOptionId" }
            return ScrapeResult.success()
        } catch (e: Exception) {
            log.error(e) { "Error retrying picture download for vote option $voteOptionId" }
            return ScrapeResult.failure("Picture download retry failed: ${e.message}")
        }
    }
}
