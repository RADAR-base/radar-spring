package radar.spring.auth.common

import javax.servlet.http.HttpServletRequest

/** Abstract Authorization validator interface to be used with a custom a token [T] for
 * validating incoming requests.
 * See [RadarAuthValidator] and [radar.spring.auth.managementportal.ManagementPortalAuthValidator]
 * **/
interface AuthValidator<T> {

    fun verify(token: String, request: HttpServletRequest): T?

    fun getToken(request: HttpServletRequest): String?
}