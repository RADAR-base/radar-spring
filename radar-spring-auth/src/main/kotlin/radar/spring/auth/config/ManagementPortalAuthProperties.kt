package radar.spring.auth.config

import java.net.URI

open class ManagementPortalAuthProperties @JvmOverloads constructor(
    val baseUrl: String,
    val resourceName: String,
    val publicKeyEndpoints: List<URI> = emptyList(),
    val publicKeyUrl: String = "$baseUrl/oauth/token_key",
)
