package ai.ledger.backend.repo

import ai.ledger.backend.domain.PlaidTransaction
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface PlaidTransactionRepository : JpaRepository<PlaidTransaction, UUID> {
    fun findAllByAccountIdInOrderByDateDescCreatedAtDesc(
        accountIds: Collection<UUID>,
        pageable: Pageable,
    ): List<PlaidTransaction>

    fun findAllByAccountIdInAndDateGreaterThanEqual(
        accountIds: Collection<UUID>,
        from: LocalDate,
    ): List<PlaidTransaction>

    fun findAllByPlaidTransactionIdIn(plaidTransactionIds: Collection<String>): List<PlaidTransaction>

    fun deleteAllByPlaidTransactionIdIn(plaidTransactionIds: Collection<String>)

    fun countByAccountIdIn(accountIds: Collection<UUID>): Long
}
