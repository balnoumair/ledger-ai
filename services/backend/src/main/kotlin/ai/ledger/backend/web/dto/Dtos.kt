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
