package ai.ledger.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "plaid_accounts")
class PlaidAccount(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "item_id", nullable = false)
    var itemId: UUID,
    @Column(name = "plaid_account_id", nullable = false, unique = true)
    var plaidAccountId: String,
    @Column(nullable = false)
    var name: String,
    @Column(name = "official_name")
    var officialName: String? = null,
    @Column
    var mask: String? = null,
    @Column(nullable = false)
    var type: String,
    @Column
    var subtype: String? = null,
    @Column(name = "current_balance")
    var currentBalance: BigDecimal? = null,
    @Column(name = "available_balance")
    var availableBalance: BigDecimal? = null,
    @Column(name = "iso_currency_code")
    var isoCurrencyCode: String? = null,
    @Column(nullable = false)
    var included: Boolean = true,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)
