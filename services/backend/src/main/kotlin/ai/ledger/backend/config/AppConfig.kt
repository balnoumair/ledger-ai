package ai.ledger.backend.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(PlaidProperties::class, LedgerProperties::class)
class AppConfig
