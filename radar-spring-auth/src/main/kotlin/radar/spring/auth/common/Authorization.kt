package radar.spring.auth.common

import org.radarbase.auth.authorization.EntityDetails
import org.radarbase.auth.authorization.Permission

/** Abstract Authorization interface to be used with a custom token [T].
 * See [radar.spring.auth.managementportal.ManagementPortalAuthorization]
 *  **/
interface Authorization<T> {

    fun authorize(
        token: T,
        permission: Permission,
        entity: EntityDetails,
        role: String? = null,
        scopes: Array<String> = emptyArray(),
        audiences: Array<String> = emptyArray(),
        grantTypes: Array<String> = emptyArray(),
    ): Boolean {
        return hasPermission(token, permission, entity) &&
            hasRole(token, project, role) &&
            hasScopes(token, scopes) &&
            hasAudiences(token, audiences) &&
            hasGrantTypes(token, grantTypes)
    }

    fun hasPermission(
        token: T,
        permission: Permission,
        entity: EntityDetails,
    ): Boolean

    fun hasRole(token: T, project: String?, role: String?): Boolean
    fun hasScopes(token: T, scopes: Array<String>): Boolean
    fun hasAudiences(token: T, audiences: Array<String>): Boolean
    fun hasGrantTypes(token: T, grantTypes: Array<String>): Boolean
}
