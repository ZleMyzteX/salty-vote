package er.codes.saltyvote.vote.controller

import er.codes.saltyvote.scrape.ScrapeRetryScheduler
import er.codes.saltyvote.vote.model.VoteWithEnrichedAirbnbOptionsDto
import er.codes.saltyvote.vote.service.VoteExternalDataService
import er.codes.saltyvote.vote.service.VoteReadService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.io.File

@RestController
@RequestMapping("/api/v1/votes/airbnb/enriched")
class EnrichedVoteController(
    private val voteReadService: VoteReadService,
    private val voteExternalDataService: VoteExternalDataService,
    private val scrapeRetryScheduler: ScrapeRetryScheduler,
) {
    private val log = KotlinLogging.logger { }

    /**
     * Get an Airbnb vote with enriched external data (scraped Airbnb information)
     * Returns empty externalData fields if no data has been scraped yet
     */
    @GetMapping("/{voteId}")
    fun getEnrichedAirbnbVote(
        @PathVariable voteId: Long,
    ): ResponseEntity<VoteWithEnrichedAirbnbOptionsDto> {
        val vote = voteReadService.getEnrichedAirbnbVoteById(voteId)
        return ResponseEntity.ok(vote)
    }

    /**
     * Get the downloaded picture for a specific vote option
     * Returns 404 if no picture is available yet
     */
    @GetMapping("/options/{optionId}/picture")
    fun getVoteOptionPicture(
        @PathVariable optionId: Long,
    ): ResponseEntity<Resource> {
        val picturePath =
            voteExternalDataService.getPicturePathForOption(optionId)
                ?: throw ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No picture available for vote option $optionId",
                )

        val pictureFile = File(picturePath)

        if (!pictureFile.exists() || !pictureFile.isFile) {
            log.warn { "Picture file not found at path: $picturePath for option $optionId" }
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Picture file not found",
            )
        }

        log.debug { "Serving picture for option $optionId from path: $picturePath" }

        val resource: Resource = FileSystemResource(pictureFile)

        return ResponseEntity
            .ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(resource)
    }

    /**
     * Check if external data is available for a specific vote option
     * Returns a simple status object
     */
    @GetMapping("/options/{optionId}/status")
    fun getVoteOptionExternalDataStatus(
        @PathVariable optionId: Long,
    ): ResponseEntity<Map<String, Boolean>> {
        val externalData = voteExternalDataService.getExternalDataForOption(optionId)
        val hasData = externalData != null
        val hasPicture = externalData?.hasPictureDownloaded ?: false

        return ResponseEntity.ok(
            mapOf(
                "hasExternalData" to hasData,
                "hasPictureDownloaded" to hasPicture,
            ),
        )
    }

    /**
     * Manually trigger the retry of failed scrapes
     * This runs the same logic as the scheduled job
     */
    @GetMapping("/retry-failed-scrapes")
    fun triggerRetryFailedScrapes(): ResponseEntity<Map<String, Any>> {
        log.info { "Manual retry of failed scrapes triggered via API endpoint" }

        scrapeRetryScheduler.retryFailedScrapes()

        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "message" to "Failed scrapes retry triggered successfully",
            ),
        )
    }
}
