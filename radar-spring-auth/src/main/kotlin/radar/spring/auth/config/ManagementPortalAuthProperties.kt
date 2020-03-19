package radar.spring.auth.config

open class ManagementPortalAuthProperties @JvmOverloads constructor(
    val baseUrl: String,
    val resourceName: String,
    val publicKeyUrl: String = "${baseUrl}/oauth/token_key"
)