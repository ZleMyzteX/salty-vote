package er.codes.saltyvote.auth.controller

import er.codes.saltyvote.auth.model.GatePasswordDto
import er.codes.saltyvote.auth.model.UserCreateRequestDto
import er.codes.saltyvote.auth.model.UserEntity
import er.codes.saltyvote.auth.model.UserLoginRequestDto
import er.codes.saltyvote.auth.service.AuthService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    @param:Value("\${gate.password}") private val gatePassword: String,
) {
    @PostMapping("/register")
    fun registerNewUser(
        @RequestBody userCreateRequest: UserCreateRequestDto,
    ): ResponseEntity<String> {
        val user = authService.createUser(userCreateRequest)

        return ResponseEntity.ok(authService.generateToken(user))
    }

    @PostMapping("/login")
    fun login(
        @RequestBody userLoginRequest: UserLoginRequestDto,
    ): ResponseEntity<String> {
        val user = authService.login(userLoginRequest)

        return if (user != null) {
            ResponseEntity.ok(authService.generateToken(user))
        } else {
            ResponseEntity.status(401).body("Invalid credentials")
        }
    }

    @PostMapping("/refresh")
    fun refreshToken(
        @AuthenticationPrincipal user: UserEntity,
    ): ResponseEntity<String> = ResponseEntity.ok().body(authService.generateToken(user))

    @PostMapping("/gate")
    fun passGate(
        @RequestBody providedPass: GatePasswordDto,
    ): ResponseEntity<String> =
        if (gatePassword != providedPass.password) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid.")
        } else {
            ResponseEntity.ok("Gate passed")
        }
}
