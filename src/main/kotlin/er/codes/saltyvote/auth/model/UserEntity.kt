package er.codes.saltyvote.auth.model

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

data class UserEntity(
    val id: UUID,
    val email: String,
    // has to be private, since UserDetails wants it that way
    private val username: String,
    val passwordHash: String,
    val admin: Boolean,
    val enabled: Boolean,
) : UserDetails {
    fun toDto() =
        UserDto(
            id = id.toString(),
            email = email,
            username = username,
        )

    fun toDetailDto() =
        UserDetailDto(
            id = id.toString(),
            email = email,
            username = username,
            admin = admin,
            enabled = enabled,
        )

    fun email() = email.lowercase()

    fun isAdmin() = admin

    override fun getAuthorities(): Collection<GrantedAuthority> {
        val authorities = mutableListOf<GrantedAuthority>()
        authorities.add(SimpleGrantedAuthority("ROLE_USER"))
        if (admin) {
            authorities.add(SimpleGrantedAuthority("ROLE_ADMIN"))
        }
        return authorities
    }

    override fun getPassword(): String? = passwordHash

    override fun getUsername(): String = username

    override fun isAccountNonExpired(): Boolean = enabled

    override fun isAccountNonLocked(): Boolean = enabled

    override fun isCredentialsNonExpired(): Boolean = enabled

    override fun isEnabled(): Boolean = enabled
}
