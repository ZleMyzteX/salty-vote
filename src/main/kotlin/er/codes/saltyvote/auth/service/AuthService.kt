package er.codes.saltyvote.auth.service

import er.codes.saltyvote.auth.jwt.JwtUtil
import er.codes.saltyvote.auth.model.UserCreateRequestDto
import er.codes.saltyvote.auth.model.UserEntity
import er.codes.saltyvote.auth.model.UserLoginRequestDto
import er.codes.saltyvote.auth.repository.UserDao
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userDao: UserDao,
    private val userValidator: UserValidator,
    private val jwtUtil: JwtUtil,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    private val passwordEncoder = BCryptPasswordEncoder()

    fun createUser(userCreateRequest: UserCreateRequestDto): UserEntity {
        userValidator.validateNewUserCreateRequest(userCreateRequest)

        val passwordHash = passwordEncoder.encode(userCreateRequest.password)
        val user = userDao.createUser(userCreateRequest.username, passwordHash, userCreateRequest.email)
        return user
    }

    fun login(userLoginRequest: UserLoginRequestDto): UserEntity? {
        val user = userDao.findByUsername(userLoginRequest.username) ?: return null

        return if (passwordMatches(userLoginRequest.password, user.password!!)) {
            return user
        } else {
            null
        }
    }

    private fun passwordMatches(
        raw: String,
        hashed: String,
    ) = passwordEncoder.matches(raw, hashed)

    fun generateToken(user: UserEntity) = jwtUtil.generateToken(user)
}
