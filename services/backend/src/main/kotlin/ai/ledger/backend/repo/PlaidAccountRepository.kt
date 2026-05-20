package ai.ledger.backend.repo

import ai.ledger.backend.domain.PlaidAccount
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlaidAccountRepository : JpaRepository<PlaidAccount, UUID> {
    fun findAllByItemIdIn(itemIds: Collection<UUID>): List<PlaidAccount>
}
