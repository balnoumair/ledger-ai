package ai.ledger.backend.repo

import ai.ledger.backend.domain.PlaidItem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlaidItemRepository : JpaRepository<PlaidItem, UUID> {
    fun findByItemId(itemId: String): PlaidItem?

    fun findAllByUserId(userId: UUID): List<PlaidItem>
}
