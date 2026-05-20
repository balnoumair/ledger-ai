package ai.ledger.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.UUID

@ConfigurationProperties(prefix = "ledger")
data class LedgerProperties(
    val localUserId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001"),
    val localUserEmail: String = "me@localhost",
)
