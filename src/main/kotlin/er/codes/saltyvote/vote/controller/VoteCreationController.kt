package er.codes.saltyvote.vote.controller

import er.codes.saltyvote.vote.model.AirbnbVoteOptionDto
import er.codes.saltyvote.vote.model.VoteCreationDto
import er.codes.saltyvote.vote.model.VoteDetailDto
import er.codes.saltyvote.vote.model.VoteOptionDto
import er.codes.saltyvote.vote.service.VoteCreationService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/vote")
class VoteCreationController(
    private val voteCreationService: VoteCreationService,
) {
    @PostMapping
    fun createNewVote(req: VoteCreationDto): ResponseEntity<Unit> {
        voteCreationService.createVote(req)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{voteId}/option")
    fun createVoteOption(
        // TODO: UUID? Friendly ID?
        @PathVariable("voteId") voteId: Long,
        req: VoteOptionDto,
    ): ResponseEntity<Unit> {
        voteCreationService.createVoteOption(voteId, req)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{voteId}/option/airbnb")
    fun createVoteOption(
        // TODO: UUID? Friendly ID?
        @PathVariable("voteId") voteId: Long,
        req: AirbnbVoteOptionDto,
    ): ResponseEntity<Unit> {
        voteCreationService.createVoteOption(voteId, req)
        return ResponseEntity.ok().build()
    }

    @GetMapping
    fun getAllVotes(): ResponseEntity<List<VoteDetailDto>> =
        voteCreationService.getAllVotes().let { ResponseEntity.ok(it) }
}
