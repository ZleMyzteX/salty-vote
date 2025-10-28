package er.codes.saltyvote.vote.controller

import er.codes.saltyvote.vote.model.VoteDetailedResultsDto
import er.codes.saltyvote.vote.model.VoteResultsDto
import er.codes.saltyvote.vote.model.VoteSubmissionDto
import er.codes.saltyvote.vote.model.VoteSubmissionResponseDto
import er.codes.saltyvote.vote.service.VoteSubmissionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/votes")
class VoteSubmissionController(
    private val voteSubmissionService: VoteSubmissionService,
) {
    @PostMapping("/submissions")
    fun submitVote(
        @RequestBody request: VoteSubmissionDto,
    ): ResponseEntity<Map<String, Long>> {
        val submissionId = voteSubmissionService.submitVote(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("submissionId" to submissionId))
    }

    @GetMapping("/{voteId}/submissions/my")
    fun getMySubmission(
        @PathVariable voteId: Long,
    ): ResponseEntity<VoteSubmissionResponseDto> {
        val submission =
            voteSubmissionService.getMySubmission(voteId)
                ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(submission)
    }

    @DeleteMapping("/{voteId}/submissions/my")
    fun deleteMySubmission(
        @PathVariable voteId: Long,
    ): ResponseEntity<Unit> {
        voteSubmissionService.deleteMySubmission(voteId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{voteId}/results")
    fun getVoteResults(
        @PathVariable voteId: Long,
    ): ResponseEntity<VoteResultsDto> {
        val results = voteSubmissionService.getVoteResults(voteId)
        return ResponseEntity.ok(results)
    }

    @GetMapping("/{voteId}/results/detailed")
    fun getDetailedVoteResults(
        @PathVariable voteId: Long,
    ): ResponseEntity<VoteDetailedResultsDto> {
        val results = voteSubmissionService.getDetailedVoteResults(voteId)
        return ResponseEntity.ok(results)
    }
}
