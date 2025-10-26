package er.codes.saltyvote.auth.jwt

import er.codes.saltyvote.auth.repository.UserDao
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

@Configuration
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil,
    private val userDao: UserDao,
) : OncePerRequestFilter() {
    private val log = KotlinLogging.logger { }

    override fun doFilterInternal(
        req: HttpServletRequest,
        res: HttpServletResponse,
        filter: FilterChain,
    ) {
        val authHeader = req.getHeader("Authorization")
        val securityContext = SecurityContextHolder.getContext()

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                val token = extractToken(authHeader)
                val id = jwtUtil.extractId(token)

                if (!id.isNullOrBlank() && securityContext.authentication == null) {
                    val userDetails = userDao.findById(id)
                    if (userDetails != null && jwtUtil.validateToken(token, userDetails)) {
                        val authentication =
                            UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.authorities,
                            )
                        authentication.details = WebAuthenticationDetailsSource().buildDetails(req)
                        securityContext.authentication = authentication
                    }
                }
            } catch (e: Exception) {
                log.debug { "JWT authentication failed: ${e.message}" }
            }
        }

        filter.doFilter(req, res)
    }

    /** extracts token from "Bearer <token>" string*/
    private fun extractToken(header: String) = header.substring(7)
}
