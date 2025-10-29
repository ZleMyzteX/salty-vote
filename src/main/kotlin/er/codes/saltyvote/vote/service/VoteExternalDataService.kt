package er.codes.saltyvote.vote.service

import er.codes.saltyvote.jooq.tables.daos.LocalPictureStoreDao
import er.codes.saltyvote.jooq.tables.daos.VoteOptionExternalDataDao
import er.codes.saltyvote.vote.model.AirbnbExternalDataDto
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

@Service
class VoteExternalDataService(
    private val voteOptionExternalDataDao: VoteOptionExternalDataDao,
    private val localPictureStoreDao: LocalPictureStoreDao,
) {
    private val log = KotlinLogging.logger { }

    /**
     * Get external data for a specific vote option
     * Returns null if no external data is available
     */
    fun getExternalDataForOption(voteOptionId: Long): AirbnbExternalDataDto? {
        val externalData =
            voteOptionExternalDataDao.fetchByVoteOptionId(voteOptionId).firstOrNull()
                ?: return null

        log.debug { "Found external data for vote option $voteOptionId" }

        return AirbnbExternalDataDto(
            title = externalData.airbnbTitle,
            reviewCount = externalData.airbnbReviewCount,
            starRating = externalData.airbnbStarRating,
            pictureUrl = externalData.airbnbPictureUrl,
            hasPictureDownloaded = externalData.airbnbPictureLocalId != null,
            sourceUrl = externalData.sourceUrl,
        )
    }

    /**
     * Get external data for multiple vote options
     * Returns a map of voteOptionId to external data
     */
    fun getExternalDataForOptions(voteOptionIds: List<Long>): Map<Long, AirbnbExternalDataDto> {
        if (voteOptionIds.isEmpty()) {
            return emptyMap()
        }

        log.debug { "Fetching external data for ${voteOptionIds.size} vote options" }

        // Fetch all external data in one query
        val allExternalData =
            voteOptionIds.flatMap { optionId ->
                voteOptionExternalDataDao.fetchByVoteOptionId(optionId)
            }

        return allExternalData
            .mapNotNull { externalData ->
                val voteOptionId = externalData.voteOptionId ?: return@mapNotNull null

                voteOptionId to
                    AirbnbExternalDataDto(
                        title = externalData.airbnbTitle,
                        reviewCount = externalData.airbnbReviewCount,
                        starRating = externalData.airbnbStarRating,
                        pictureUrl = externalData.airbnbPictureUrl,
                        hasPictureDownloaded = externalData.airbnbPictureLocalId != null,
                        sourceUrl = externalData.sourceUrl,
                    )
            }.toMap()
    }

    /**
     * Get the local file path for a downloaded picture
     * Returns null if no picture is available
     */
    fun getPicturePathForOption(voteOptionId: Long): String? {
        val externalData =
            voteOptionExternalDataDao.fetchByVoteOptionId(voteOptionId).firstOrNull()
                ?: return null

        val pictureId = externalData.airbnbPictureLocalId ?: return null

        val picture =
            localPictureStoreDao.fetchOneById(pictureId)
                ?: return null

        return picture.localPath
    }
}
