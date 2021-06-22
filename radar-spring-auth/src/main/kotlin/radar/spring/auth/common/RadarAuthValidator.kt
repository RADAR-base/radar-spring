package radar.spring.auth.common

import org.radarbase.auth.exception.TokenValidationException
import org.radarbase.auth.token.RadarToken
import javax.servlet.http.HttpServletRequest

interface RadarAuthValidator : AuthValidator<RadarToken> {

    @Throws(TokenValidationException::class)
    override fun verify(token: String, request: HttpServletRequest): RadarToken?

    override fun getToken(request: HttpServletRequest): String? {
        val authorizationHeader = request.getHeader("Authorization")

        // Check if the HTTP Authorization header is present and formatted correctly
        if (authorizationHeader != null
            && authorizationHeader.startsWith(BEARER, ignoreCase = true)
        ) {
            // Extract the token from the HTTP Authorization header
            return authorizationHeader.substring(BEARER.length).trim { it <= ' ' }
        }

        // Extract the token from the Authorization cookie
        val authorizationCookie = request.cookies?.find { it.name == "authorizationBearer" }
        if (authorizationCookie != null) {
            return authorizationCookie.value
        }

        return null
    }

    companion object {
        const val BEARER = "Bearer "
    }
}
