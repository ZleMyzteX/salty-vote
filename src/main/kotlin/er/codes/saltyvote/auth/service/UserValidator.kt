package er.codes.saltyvote.auth.service

import er.codes.saltyvote.auth.model.UserCreateRequestDto
import er.codes.saltyvote.auth.repository.UserDao
import org.springframework.stereotype.Component

@Component
class UserValidator(
    private val userDao: UserDao,
) {
    fun validateNewUserCreateRequest(userCreateRequest: UserCreateRequestDto) {
        val username = userCreateRequest.username
        require(username.isNotBlank()) { "Username must not be blank!" }
        require(username.length !in MAX_USERNAME_LENGTH..MIN_USERNAME_LENGTH) {
            "Username must be between $MIN_USERNAME_LENGTH and $MAX_USERNAME_LENGTH characters!"
        }
        userDao.findByUsername(username)?.let {
            throw IllegalArgumentException("Username already exists!")
        }

        val password = userCreateRequest.password
        require(password.isNotBlank()) { "Password must not be blank!" }
        require(password.length < 500) { "Password must be less than 500 characters!" }
    }

    companion object {
        private const val MIN_USERNAME_LENGTH = 3
        private const val MAX_USERNAME_LENGTH = 50
    }
}
