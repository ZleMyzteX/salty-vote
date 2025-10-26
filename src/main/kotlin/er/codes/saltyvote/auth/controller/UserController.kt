package er.codes.saltyvote.auth.controller

import er.codes.saltyvote.auth.model.UserDetailDto
import er.codes.saltyvote.auth.model.UserDto
import er.codes.saltyvote.auth.model.UserEntity
import er.codes.saltyvote.auth.service.UserService
import er.codes.saltyvote.auth.util.checkIfAdminOrThrow
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/user")
class UserController(
    private val userService: UserService,
) {
    @GetMapping
    fun getAllUsers() = userService.getAllUsers()

    @GetMapping("/me")
    fun getCurrentUser(
        @AuthenticationPrincipal currentUser: UserEntity,
    ): UserDetailDto = currentUser.toDetailDto()

    @GetMapping("/admins")
    fun getAllAdmins(
        @AuthenticationPrincipal currentUser: UserEntity,
    ): List<UserDto> {
        checkIfAdminOrThrow(currentUser)

        return userService.getAllAdmins()
    }
}
