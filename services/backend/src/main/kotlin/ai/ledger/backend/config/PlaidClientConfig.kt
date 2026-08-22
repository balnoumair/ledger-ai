package ai.ledger.backend.config

import com.plaid.client.ApiClient
import com.plaid.client.request.PlaidApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Wires the Plaid Java SDK with the configured environment. The Plaid `client_id`
 * and `secret` only exist here — they are loaded from environment via
 * [PlaidProperties] and never exposed to the BFF or desktop app.
 */
@Configuration
class PlaidClientConfig {
    @Bean
    fun plaidApiClient(properties: PlaidProperties): ApiClient {
        val keys =
            hashMapOf(
                "clientId" to properties.clientId,
                "secret" to properties.secret,
                "plaidVersion" to "2020-09-14",
            )
        val client = ApiClient(keys)
        // Plaid retired the Development environment; only Production and
        // Sandbox exist in plaid-java 27.
        when (properties.env.lowercase()) {
            "production" -> client.setPlaidAdapter(ApiClient.Production)
            else -> client.setPlaidAdapter(ApiClient.Sandbox)
        }
        return client
    }

    @Bean
    fun plaidApi(apiClient: ApiClient): PlaidApi = apiClient.createService(PlaidApi::class.java)
}
