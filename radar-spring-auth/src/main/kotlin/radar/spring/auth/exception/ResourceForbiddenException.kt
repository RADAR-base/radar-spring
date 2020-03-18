package radar.spring.auth.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.FORBIDDEN)
class ResourceForbiddenException @JvmOverloads constructor(
    override val message: String, override val cause: Throwable? =
        null
) :
    RuntimeException(message, cause)
