package radar.spring.auth.managementportal

import jakarta.servlet.http.HttpServletRequest
import org.radarbase.auth.authentication.StaticTokenVerifierLoader
import org.radarbase.auth.authentication.TokenValidator
import org.radarbase.auth.authentication.TokenVerifierLoader
import org.radarbase.auth.exception.TokenValidationException
import org.radarbase.auth.jwks.*
import org.radarbase.auth.jwks.JwksTokenVerifierLoader.Companion.toTokenVerifier
import org.radarbase.auth.token.RadarToken
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import radar.spring.auth.common.RadarAuthValidator
import radar.spring.auth.config.ManagementPortalAuthProperties
import java.time.Duration

/** The [radar.spring.auth.common.AuthValidator] for Management Portal tokens. **/
@Component
class ManagementPortalAuthValidator(
    @Autowired private val managementPortalProperties: ManagementPortalAuthProperties,
) : RadarAuthValidator {

    val tokenValidator: TokenValidator = createTokenValidator(managementPortalProperties)

    @Throws(TokenValidationException::class)
    override fun verify(token: String, request: HttpServletRequest): RadarToken
        = tokenValidator.validateBlocking(token)

    companion object {
        private val logger = LoggerFactory.getLogger(ManagementPortalAuthValidator::class.java)

        fun createTokenValidator(config: ManagementPortalAuthProperties): TokenValidator {
            val tokenVerifierLoaders = buildList {
                add(config.publicKeyUrl)
                addAll(config.publicKeyEndpoints)
            }.mapTo(ArrayList<TokenVerifierLoader>()) {
                JwksTokenVerifierLoader(it, config.resourceName, JwkAlgorithmParser())
            }

            val algorithms = buildList {
                if (config.ecdsaKeys.isNotEmpty()) {
                    val parser = ECPEMCertificateParser()
                    config.ecdsaKeys.mapTo(this) { key ->
                        parser.parseAlgorithm(key)
                    }
                }
                if (config.rsaKeys.isNotEmpty()) {
                    val parser = RSAPEMCertificateParser()
                    config.rsaKeys.mapTo(this) { key ->
                        parser.parseAlgorithm(key)
                    }
                }
            }

            if (algorithms.isNotEmpty()) {
                tokenVerifierLoaders += StaticTokenVerifierLoader(
                    algorithms.map { algorithm ->
                        algorithm.toTokenVerifier(config.resourceName) {
                            config.issuer?.let {
                                withIssuer(it)
                            }
                        }
                    },
                )
            }

            if (tokenVerifierLoaders.isEmpty()) {
                throw TokenValidationException("No verification algorithms given")
            }

            logger.info("Verifying JWTs with ${tokenVerifierLoaders.size} token verifiers")

            return TokenValidator(
                verifierLoaders = tokenVerifierLoaders,
                fetchTimeout = Duration.ofMinutes(5),
                maxAge = Duration.ofHours(3),
            )
        }
    }
}
