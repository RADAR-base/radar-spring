package radar.spring.auth.common

/** Abstract Authorization interface to be used with a custom token [T].
 * See [radar.spring.auth.managementportal.ManagementPortalAuthorization]
 *  **/
interface Authorization<T> {

    fun authorize(
        token: T,
        permission: String,
        entity: String,
        permissionOn: PermissionOn? = PermissionOn.DEFAULT,
        role: String? = null,
        scopes: Array<String> = emptyArray(),
        authorities: Array<String> = emptyArray(),
        audiences: Array<String> = emptyArray(),
        grantTypes: Array<String> = emptyArray(),
        project: String? = null,
        user: String? = null,
        source: String? = null
    ): Boolean {
        return hasPermission(token, permission, entity, permissionOn, project, user, source) &&
            hasRole(token, project, role) &&
            hasScopes(token, scopes) &&
            hasAuthorities(token, authorities) &&
            hasAudiences(token, audiences) &&
            hasGrantTypes(token, grantTypes)
    }

    fun hasPermission(
        token: T,
        permission: String,
        entity: String,
        permissionOn: PermissionOn?,
        project: String?,
        user: String?,
        source: String?
    ): Boolean

    fun hasRole(token: T, project: String?, role: String?): Boolean
    fun hasScopes(token: T, scopes: Array<String>): Boolean
    fun hasAuthorities(token: T, authorities: Array<String>): Boolean
    fun hasAudiences(token: T, audiences: Array<String>): Boolean
    fun hasGrantTypes(token: T, grantTypes: Array<String>): Boolean
}
