package ai.ledger.backend.bootstrap

import ai.ledger.backend.config.LedgerProperties
import ai.ledger.backend.domain.User
import ai.ledger.backend.repo.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

/**
 * Ensures the single local `me` user row exists at startup. This change has no
 * auth model — every endpoint operates on this user implicitly. See design.md
 * Decision 4.
 */
@Component
class LocalUserSeeder(
    private val userRepository: UserRepository,
    private val ledgerProperties: LedgerProperties,
) : CommandLineRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String) {
        val id = ledgerProperties.localUserId
        if (userRepository.existsById(id)) {
            log.info("Local 'me' user already present (id={})", id)
            return
        }
        userRepository.save(
            User(
                id = id,
                email = ledgerProperties.localUserEmail,
            ),
        )
        log.info("Seeded local 'me' user (id={}, email={})", id, ledgerProperties.localUserEmail)
    }
}
