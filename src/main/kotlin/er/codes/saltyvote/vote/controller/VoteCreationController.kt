package er.codes.saltyvote.vote.controller

import er.codes.saltyvote.vote.model.AirbnbVoteOptionDto
import er.codes.saltyvote.vote.model.CreateAirbnbVoteDto
import er.codes.saltyvote.vote.model.VoteListDto
import er.codes.saltyvote.vote.model.VoteListWithRelationshipDto
import er.codes.saltyvote.vote.model.VoteWithAirbnbOptionsDto
import er.codes.saltyvote.vote.service.VoteManagementService
import er.codes.saltyvote.vote.service.VoteReadService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/votes/airbnb")
class VoteCreationController(
    private val voteManagementService: VoteManagementService,
    private val voteReadService: VoteReadService,
) {
    @PostMapping
    fun createNewAirbnbVote(
        @RequestBody req: CreateAirbnbVoteDto,
    ): ResponseEntity<Map<String, Long>> {
        val voteId = voteManagementService.createAirbnbVote(req)
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("voteId" to voteId))
    }

    @GetMapping("/{voteId}")
    fun getAirbnbVote(
        @PathVariable voteId: Long,
    ): ResponseEntity<VoteWithAirbnbOptionsDto> {
        val vote = voteReadService.getAirbnbVoteById(voteId)
        return ResponseEntity.ok(vote)
    }

    @PutMapping("/{voteId}")
    fun updateAirbnbVote(
        @PathVariable voteId: Long,
        @RequestBody req: CreateAirbnbVoteDto,
    ): ResponseEntity<Unit> {
        voteManagementService.updateVote(voteId, req)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{voteId}")
    fun deleteVote(
        @PathVariable voteId: Long,
    ): ResponseEntity<Unit> {
        voteManagementService.deleteVote(voteId)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{voteId}/status")
    fun markVoteAsDone(
        @PathVariable voteId: Long,
        @RequestParam done: Boolean,
    ): ResponseEntity<Unit> {
        voteManagementService.markVoteAsDone(voteId, done)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{voteId}/options")
    fun addVoteOption(
        @PathVariable voteId: Long,
        @RequestBody req: AirbnbVoteOptionDto,
    ): ResponseEntity<Map<String, Long>> {
        val optionId = voteManagementService.addVoteOption(voteId, req)
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("optionId" to optionId))
    }

    @DeleteMapping("/{voteId}/options/{optionId}")
    fun deleteVoteOption(
        @PathVariable voteId: Long,
        @PathVariable optionId: Long,
    ): ResponseEntity<Unit> {
        voteManagementService.deleteVoteOption(voteId, optionId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    fun getAllVotes(): ResponseEntity<List<VoteListDto>> {
        val votes = voteReadService.getAllVotes()
        return ResponseEntity.ok(votes)
    }

    @GetMapping("/with-relationship")
    fun getAllVotesWithRelationship(): ResponseEntity<List<VoteListWithRelationshipDto>> {
        val votes = voteReadService.getAllVotesWithRelationship()
        return ResponseEntity.ok(votes)
    }

    @GetMapping("/my")
    fun getMyVotes(): ResponseEntity<List<VoteListDto>> {
        val votes = voteReadService.getMyVotes()
        return ResponseEntity.ok(votes)
    }
}
