package radar.spring.auth.common

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.CodeSignature
import org.slf4j.LoggerFactory
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import radar.spring.auth.exception.AuthorizationFailedException
import radar.spring.auth.exception.ResourceForbiddenException
import javax.servlet.http.HttpServletRequest


@Aspect
open class AuthAspect<T> @JvmOverloads constructor(
    private val authValidator: AuthValidator<T>,
    private val authorization: Authorization<T>,
    val projectIdParamNames: Array<String> = arrayOf(PROJECT_ID_PARAMETER_NAME),
    val subjectIdParamNames: Array<String> = arrayOf(SUBJECT_ID_PARAMETER_NAME),
    val sourceIdParamNames: Array<String> = arrayOf(SOURCE_ID_PARAMETER_NAME)
) {

    @Before("@annotation(authorized) && execution(* *(..))")
    fun before(joinPoint: JoinPoint, authorized: Authorized) {

        // Get request from current spring context
        val reqAttr = RequestContextHolder.getRequestAttributes()
        val req = (reqAttr as ServletRequestAttributes).request

        val args = joinPoint.args
        val codeSignature = joinPoint.signature as CodeSignature

        var projectId: String? = null
        var subjectId: String? = null
        var sourceId: String? = null

        for ((index, arg) in args.withIndex()) {
            if (codeSignature.parameterTypes[index] == String::class.java) {
                when (codeSignature.parameterNames[index]) {
                    in projectIdParamNames -> projectId = arg as String
                    in subjectIdParamNames -> subjectId = arg as String
                    in sourceIdParamNames -> sourceId = arg as String
                }
            }
        }
        authorize(authorized, req, projectId, subjectId, sourceId)
    }

    fun authorize(
        authorized: Authorized, request: HttpServletRequest, projectId:
        String?, subjectId: String?, sourceId: String?
    ) {
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
                user = subjectId,
                project = projectId,
                source = sourceId,
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
                "The token is missing from the request. No bearer " +
                        "token provided in the request"
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
                "Cannot verify token. It may have been rendered invalid.", exc
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
        const val PROJECT_ID_PARAMETER_NAME = "projectId"
        const val SUBJECT_ID_PARAMETER_NAME = "subjectId"
        const val SOURCE_ID_PARAMETER_NAME = "sourceId"
        private val logger = LoggerFactory.getLogger(AuthAspect::class.java)
    }
}
