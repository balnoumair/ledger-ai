package ai.ledger.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "plaid_transactions")
class PlaidTransaction(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(name = "account_id", nullable = false)
    var accountId: UUID,
    @Column(name = "plaid_transaction_id", nullable = false, unique = true)
    var plaidTransactionId: String,
    @Column(nullable = false)
    var name: String,
    @Column(name = "merchant_name")
    var merchantName: String? = null,
    /** Plaid convention: positive = money out, negative = money in. */
    @Column(nullable = false)
    var amount: BigDecimal,
    @Column(name = "iso_currency_code")
    var isoCurrencyCode: String? = null,
    @Column(nullable = false)
    var date: LocalDate,
    @Column(nullable = false)
    var pending: Boolean = false,
    @Column(name = "category_primary")
    var categoryPrimary: String? = null,
    @Column(name = "category_detailed")
    var categoryDetailed: String? = null,
    @Column(name = "payment_channel")
    var paymentChannel: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)
