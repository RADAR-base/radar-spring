package radar.spring.auth.common

import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.CodeSignature
import org.radarbase.auth.authorization.EntityDetails
import org.radarbase.auth.authorization.entityDetails
import org.slf4j.LoggerFactory
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import radar.spring.auth.exception.AuthorizationFailedException
import radar.spring.auth.exception.ResourceForbiddenException

@Aspect
open class AuthAspect<T> @JvmOverloads constructor(
    private val authValidator: AuthValidator<T>,
    private val authorization: Authorization<T>,
    val organizationIdParamNames: Set<String> = setOf(ORGANIZATION_ID_PARAMETER_NAME),
    val projectIdParamNames: Set<String> = setOf(PROJECT_ID_PARAMETER_NAME),
    val subjectIdParamNames: Set<String> = setOf(SUBJECT_ID_PARAMETER_NAME),
    val sourceIdParamNames: Set<String> = setOf(SOURCE_ID_PARAMETER_NAME),
    val userIdParamNames: Set<String> = setOf(USER_ID_PARAMETER_NAME),
) {

    @Before("@annotation(authorized) && execution(* *(..))")
    fun before(joinPoint: JoinPoint, authorized: Authorized) {
        // Get request from current spring context
        val reqAttr = RequestContextHolder.getRequestAttributes()
        val req = (reqAttr as ServletRequestAttributes).request

        val args = joinPoint.args
        val codeSignature = joinPoint.signature as CodeSignature

        val entity = entityDetails {
            args.indices
                .asSequence()
                .filter { codeSignature.parameterTypes[it] == String::class.java }
                .forEach { index ->
                    when (codeSignature.parameterNames[index]) {
                        in organizationIdParamNames -> organization = args[index] as String
                        in projectIdParamNames -> project = args[index] as String
                        in subjectIdParamNames -> subject = args[index] as String
                        in sourceIdParamNames -> source = args[index] as String
                        in userIdParamNames -> user = args[index] as String
                    }
                }
        }
        authorize(authorized, req, entity)
    }

    fun authorize(
        authorized: Authorized,
        request: HttpServletRequest,
        entity: EntityDetails,
    ) {
        if (!authorized.enabled) {
            return
        }

        logger.debug("Authorizing request...")
        val token = ensureToken(request)

        if (this.authorization.authorize(
                token = token,
                permission = authorized.permission,
                entity = authorized.entity,
                permissionOn = authorized.permissionOn,
                role = authorized.role,
                scopes = authorized.scopes,
                authorities = authorized.authorities,
                entity = entity,
                grantTypes = authorized.grantTypes,
                audiences = authorized.audiences
            )
        ) {
            logger.debug("Setting the token in the request: {}", token)
            request.setAttribute(TOKEN_KEY, token)
        } else {
            throw ResourceForbiddenException("The requested resource is forbidden.")
        }
    }

    private fun ensureToken(request: HttpServletRequest): T {
        val tokenString = authValidator.getToken(request)

        if (tokenString == null) {
            logger.warn(
                "[401] {}: No token bearer header provided in the request",
                request.requestURI
            )
            throw AuthorizationFailedException(
                "The token is missing from the request. No bearer token provided in the request"
            )
        }
        val token: T? = try {
            this.authValidator.verify(
                token = tokenString,
                request = request
            )
        } catch (exc: Exception) {
            logger.warn("[401] {}: {}", request.requestURI, exc.toString())
            throw AuthorizationFailedException(
                "Cannot verify token. It may have been rendered invalid.",
                exc
            )
        }

        if (token == null) {
            logger.warn(
                "[401] {}: Bearer token invalid",
                request.requestURI
            )
            throw AuthorizationFailedException("Bearer token is not a valid JWT.")
        }

        return token
    }

    companion object {
        const val TOKEN_KEY = "radar_token"
        const val ORGANIZATION_ID_PARAMETER_NAME = "organizationId"
        const val PROJECT_ID_PARAMETER_NAME = "projectId"
        const val SUBJECT_ID_PARAMETER_NAME = "subjectId"
        const val SOURCE_ID_PARAMETER_NAME = "sourceId"
        const val USER_ID_PARAMETER_NAME = "userId"
        private val logger = LoggerFactory.getLogger(AuthAspect::class.java)
    }
}
