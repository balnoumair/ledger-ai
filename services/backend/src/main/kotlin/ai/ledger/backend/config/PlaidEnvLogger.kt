package ai.ledger.backend.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Logs the resolved Plaid environment at startup so it's obvious whether the
 * backend is talking to sandbox / development / production. Required by the
 * design's risk-mitigation step: protects against accidentally pointing
 * `PLAID_ENV` at a billed environment.
 */
@Component
class PlaidEnvLogger(
    private val plaidProperties: PlaidProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun logPlaidEnv() {
        val env = plaidProperties.env.lowercase()
        val configured = plaidProperties.clientId.isNotBlank() && plaidProperties.secret.isNotBlank()
        log.info("Plaid environment resolved: PLAID_ENV={} (credentials configured: {})", env, configured)
        if (env != "sandbox") {
            log.warn(
                "PLAID_ENV is '{}', not 'sandbox'. This will hit billed Plaid endpoints. " +
                    "Set PLAID_ENV=sandbox unless you really mean it.",
                env,
            )
        }
    }
}
