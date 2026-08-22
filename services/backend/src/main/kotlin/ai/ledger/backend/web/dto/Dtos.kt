package ai.ledger.backend.web.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.util.UUID

data class LinkTokenResponse(
    @JsonProperty("link_token") val linkToken: String,
    @JsonProperty("expiration") val expiration: String?,
)

data class ExchangeRequest(
    @field:NotBlank @JsonProperty("public_token") val publicToken: String,
    @JsonProperty("institution") val institution: InstitutionMeta?,
)

data class InstitutionMeta(
    @JsonProperty("name") val name: String?,
    @JsonProperty("institution_id") val institutionId: String?,
)

/**
 * Public account shape — note that `access_token` and the internal Plaid
 * `item_id` are deliberately omitted. See spec requirement
 * "Backend serves the user's linked accounts".
 */
data class AccountResponse(
    @JsonProperty("id") val id: UUID,
    @JsonProperty("plaid_account_id") val plaidAccountId: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("official_name") val officialName: String?,
    @JsonProperty("mask") val mask: String?,
    @JsonProperty("type") val type: String,
    @JsonProperty("subtype") val subtype: String?,
    @JsonProperty("current_balance") val currentBalance: BigDecimal?,
    @JsonProperty("available_balance") val availableBalance: BigDecimal?,
    @JsonProperty("iso_currency_code") val isoCurrencyCode: String?,
    @JsonProperty("included") val included: Boolean,
    @JsonProperty("institution_name") val institutionName: String,
)

data class UpdateAccountIncludedRequest(
    @JsonProperty("included") val included: Boolean,
)

data class TransactionResponse(
    @JsonProperty("id") val id: UUID,
    @JsonProperty("account_id") val accountId: UUID,
    @JsonProperty("account_name") val accountName: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("merchant_name") val merchantName: String?,
    /** Plaid convention: positive = money out, negative = money in. */
    @JsonProperty("amount") val amount: BigDecimal,
    @JsonProperty("iso_currency_code") val isoCurrencyCode: String?,
    @JsonProperty("date") val date: String,
    @JsonProperty("pending") val pending: Boolean,
    @JsonProperty("category") val category: String?,
    @JsonProperty("category_detailed") val categoryDetailed: String?,
    @JsonProperty("payment_channel") val paymentChannel: String?,
)

data class TransactionsSyncResult(
    @JsonProperty("added") val added: Int,
    @JsonProperty("modified") val modified: Int,
    @JsonProperty("removed") val removed: Int,
    @JsonProperty("total") val total: Long,
)

data class CategorySpend(
    @JsonProperty("category") val category: String,
    @JsonProperty("amount") val amount: BigDecimal,
)

data class SummaryResponse(
    @JsonProperty("net_worth") val netWorth: BigDecimal,
    @JsonProperty("total_assets") val totalAssets: BigDecimal,
    @JsonProperty("total_liabilities") val totalLiabilities: BigDecimal,
    @JsonProperty("account_count") val accountCount: Int,
    @JsonProperty("month_spending") val monthSpending: BigDecimal,
    @JsonProperty("month_income") val monthIncome: BigDecimal,
    @JsonProperty("spending_by_category") val spendingByCategory: List<CategorySpend>,
    @JsonProperty("month") val month: String,
)
