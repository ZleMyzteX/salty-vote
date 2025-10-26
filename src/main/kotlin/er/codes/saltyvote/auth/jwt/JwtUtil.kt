package er.codes.saltyvote.auth.jwt

import er.codes.saltyvote.auth.model.UserEntity
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtUtil(
    @param:Value("\${jwt.secret}") private val secret: String,
    @param:Value("\${jwt.expiration}") private val expiration: Long = 1000 * 60 * 60 * 4, // Default to 4 hours
) {
    private var secretKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateToken(user: UserEntity): String =
        Jwts
            .builder()
            .claim(CLAIM_ID, user.id)
            .claim(CLAIM_USERNAME, user.username)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(secretKey)
            .compact()

    private fun extractClaims(token: String) =
        Jwts
            .parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

    fun extractUsername(token: String): String? = extractClaims(token)[CLAIM_USERNAME, String::class.java]

    fun extractId(token: String): String? = extractClaims(token)[CLAIM_ID, String::class.java]

    fun validateToken(
        token: String,
        user: UserEntity?,
    ) = extractId(token) == user?.id.toString() && !isTokenExpired(token)

    fun isTokenExpired(token: String): Boolean {
        val expiration =
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload.expiration
        return expiration.before(Date())
    }

    companion object {
        private const val CLAIM_ID = "id"
        private const val CLAIM_USERNAME = "username"
    }
}
