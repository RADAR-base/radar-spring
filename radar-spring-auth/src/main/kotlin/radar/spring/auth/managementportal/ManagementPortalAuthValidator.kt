package radar.spring.auth.managementportal

import org.radarcns.auth.authentication.TokenValidator
import org.radarcns.auth.config.TokenValidatorConfig
import org.radarcns.auth.config.TokenVerifierPublicKeyConfig
import org.radarcns.auth.exception.TokenValidationException
import org.radarcns.auth.token.RadarToken
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import radar.spring.auth.common.RadarAuthValidator
import radar.spring.auth.config.ManagementPortalAuthProperties
import java.net.URI
import javax.servlet.http.HttpServletRequest

/** The [radar.spring.auth.common.AuthValidator] for Management Portal tokens. **/
@Component
class ManagementPortalAuthValidator @JvmOverloads constructor(
    @Autowired private val managementPortalProperties: ManagementPortalAuthProperties,
    private val tokenValidatorConfig: TokenValidatorConfig = TokenVerifierPublicKeyConfig().apply {
        publicKeyEndpoints = listOf(URI(managementPortalProperties.publicKeyUrl))
        resourceName = managementPortalProperties.resourceName
    },
    private val tokenValidator: TokenValidator = TokenValidator(tokenValidatorConfig)
) :
    RadarAuthValidator {

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
    override fun verify(token: String, request: HttpServletRequest): RadarToken? {
        return tokenValidator.validateAccessToken(token)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ManagementPortalAuthValidator::class.java)
    }
}