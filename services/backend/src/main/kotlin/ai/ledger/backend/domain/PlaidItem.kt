package ai.ledger.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "plaid_items")
class PlaidItem(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "item_id", nullable = false, unique = true)
    var itemId: String,
    @Column(name = "access_token", nullable = false)
    var accessToken: String,
    @Column(name = "institution_name", nullable = false)
    var institutionName: String,
    @Column(name = "institution_id")
    var institutionId: String? = null,
    @Column(name = "user_id", nullable = false)
    var userId: UUID,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)
