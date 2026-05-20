package ai.ledger.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "plaid")
data class PlaidProperties(
    val clientId: String = "",
    val secret: String = "",
    val env: String = "sandbox",
)
