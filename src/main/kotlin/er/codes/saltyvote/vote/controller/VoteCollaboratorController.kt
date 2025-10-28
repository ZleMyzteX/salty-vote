package er.codes.saltyvote.vote.controller

import er.codes.saltyvote.auth.model.UserEntity
import er.codes.saltyvote.vote.model.AddCollaboratorDto
import er.codes.saltyvote.vote.model.CollaboratorDto
import er.codes.saltyvote.vote.service.VoteCollaboratorService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/votes/{voteId}/collaborators")
class VoteCollaboratorController(
    private val voteCollaboratorService: VoteCollaboratorService,
) {
    @PostMapping
    fun addCollaborator(
        @PathVariable voteId: Long,
        @RequestBody request: AddCollaboratorDto,
        @AuthenticationPrincipal user: UserEntity,
    ): ResponseEntity<CollaboratorDto> {
        val collaborator = voteCollaboratorService.addCollaborator(voteId, user.id, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(collaborator)
    }

    @DeleteMapping("/{collaboratorId}")
    fun removeCollaborator(
        @PathVariable voteId: Long,
        @PathVariable collaboratorId: Long,
        @AuthenticationPrincipal user: UserEntity,
    ): ResponseEntity<Unit> {
        voteCollaboratorService.removeCollaborator(voteId, user.id, collaboratorId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    fun getCollaborators(
        @PathVariable voteId: Long,
    ): ResponseEntity<List<CollaboratorDto>> {
        val collaborators = voteCollaboratorService.getCollaborators(voteId)
        return ResponseEntity.ok(collaborators)
    }
}
