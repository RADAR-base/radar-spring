package radar.spring.auth.config

open class ManagementPortalAuthProperties @JvmOverloads constructor(
    val baseUrl: String,
    val resourceName: String,
    val issuer: String? = null,
    val publicKeyUrl: String = "$baseUrl/oauth/token_key",
    val publicKeyEndpoints: List<String> = emptyList(),
    val ecdsaKeys: List<String> = emptyList(),
    val rsaKeys: List<String> = emptyList(),
)
