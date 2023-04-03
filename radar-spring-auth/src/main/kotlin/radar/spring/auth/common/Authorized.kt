package radar.spring.auth.common

import org.radarbase.auth.authorization.Permission

@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class Authorized(
    /** To signify id this is enabled or not. **/
    val enabled: Boolean = true,

    /** The permission/operation to authorize for. Eg- READ **/
    val permission: Permission,

    val scopes: Array<String> = [],
    val role: String = "",
    val authorities: Array<String> = [],
    val audiences: Array<String> = [],
    val grantTypes: Array<String> = []
)
