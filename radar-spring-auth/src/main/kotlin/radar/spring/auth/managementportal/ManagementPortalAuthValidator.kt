package radar.spring.auth.managementportal

import jakarta.servlet.http.HttpServletRequest
import org.radarbase.auth.authentication.TokenValidator
import org.radarbase.auth.authentication.TokenVerifierLoader
import org.radarbase.auth.exception.TokenValidationException
import org.radarbase.auth.jwks.JwkAlgorithmParser
import org.radarbase.auth.jwks.JwksTokenVerifierLoader
import org.radarbase.auth.token.RadarToken
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import radar.spring.auth.common.RadarAuthValidator
import radar.spring.auth.config.ManagementPortalAuthProperties

/** The [radar.spring.auth.common.AuthValidator] for Management Portal tokens. */
@Component
class ManagementPortalAuthValidator
@JvmOverloads
constructor(
    @Autowired private val managementPortalProperties: ManagementPortalAuthProperties,
    private val tokenVerifiers: List<TokenVerifierLoader> =
        managementPortalProperties.publicKeyEndpoints.map {
            JwksTokenVerifierLoader(
                it.toString(),
                managementPortalProperties.resourceName,
                JwkAlgorithmParser()
            )
        } +
            listOf(
                JwksTokenVerifierLoader(
                    managementPortalProperties.publicKeyUrl,
                    managementPortalProperties.resourceName,
                    JwkAlgorithmParser()
                )
            ),
    private val tokenValidator: TokenValidator = TokenValidator(tokenVerifiers)
) : RadarAuthValidator {
    init {
        try {
            this.tokenValidator.refresh()
            logger.debug("Refreshed Token Validator keys")
        } catch (ex: Exception) {
            logger.error(
                "Failed to immediately initialize token validator, will try again later: {}",
                ex.toString()
            )
        }
    }

    @Throws(TokenValidationException::class)
    override fun verify(
        token: String,
        request: HttpServletRequest
    ): RadarToken? {
        return tokenValidator.validateBlocking(token)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ManagementPortalAuthValidator::class.java)
    }
}
