package radar.spring.auth.common

@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class AuthorizedAfter(
    /** To signify id this is enabled or not. **/
    val enabled: Boolean = true,

    /** The permission/operation to authorize for. Eg- READ **/
    val permission: String,

    /** The Entity on which the permission to be authorised for. Example- MEASUREMENT **/
    val entity: String,

    /** The Entity on which the above permission and entity is to be check for. Eg - Project **/
    val permissionOn: PermissionOn = PermissionOn.DEFAULT,

    val scopes: Array<String> = [],
    val role: String = "",
    val authorities: Array<String> = [],
    val audiences: Array<String> = [],
    val grantTypes: Array<String> = []
)