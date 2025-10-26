package er.codes.saltyvote.auth.model

data class UserDto(
    val id: String,
    val email: String,
    val username: String,
)

data class UserDetailDto(
    val id: String,
    val email: String,
    val username: String,
    val admin: Boolean,
    val enabled: Boolean,
)

data class UserCreateRequestDto(
    val email: String,
    val username: String,
    val password: String,
)

data class UserLoginRequestDto(
    val username: String,
    val password: String,
)

data class UserUpdateRequestDto(
    val email: String?,
    val username: String?,
    val password: String?,
)
