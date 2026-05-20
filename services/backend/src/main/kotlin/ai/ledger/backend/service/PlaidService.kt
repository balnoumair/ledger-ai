package ai.ledger.backend.service

import ai.ledger.backend.config.LedgerProperties
import ai.ledger.backend.domain.PlaidAccount
import ai.ledger.backend.domain.PlaidItem
import ai.ledger.backend.repo.PlaidAccountRepository
import ai.ledger.backend.repo.PlaidItemRepository
import ai.ledger.backend.web.dto.AccountResponse
import ai.ledger.backend.web.dto.InstitutionMeta
import ai.ledger.backend.web.dto.LinkTokenResponse
import com.plaid.client.model.AccountsGetRequest
import com.plaid.client.model.CountryCode
import com.plaid.client.model.ItemPublicTokenExchangeRequest
import com.plaid.client.model.LinkTokenCreateRequest
import com.plaid.client.model.LinkTokenCreateRequestUser
import com.plaid.client.model.Products
import com.plaid.client.request.PlaidApi
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class PlaidService(
    private val plaidApi: PlaidApi,
    private val itemRepository: PlaidItemRepository,
    private val accountRepository: PlaidAccountRepository,
    private val ledgerProperties: LedgerProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun createLinkToken(userId: UUID = ledgerProperties.localUserId): LinkTokenResponse {
        val user = LinkTokenCreateRequestUser().clientUserId(userId.toString())
        val request =
            LinkTokenCreateRequest()
                .user(user)
                .clientName("Ledger AI")
                .products(listOf(Products.TRANSACTIONS))
                .countryCodes(listOf(CountryCode.US))
                .language("en")
        val response = plaidApi.linkTokenCreate(request).execute()
        if (!response.isSuccessful || response.body() == null) {
            val body = response.errorBody()?.string()
            log.error("Plaid linkTokenCreate failed: code={}, body={}", response.code(), body)
            error("Failed to create Plaid link token (code=${response.code()})")
        }
        val body = response.body()!!
        return LinkTokenResponse(
            linkToken = body.linkToken,
            expiration = body.expiration?.toString(),
        )
    }

    @Transactional
    fun exchangePublicToken(
        publicToken: String,
        institution: InstitutionMeta?,
        userId: UUID = ledgerProperties.localUserId,
    ): List<AccountResponse> {
        val exchangeReq = ItemPublicTokenExchangeRequest().publicToken(publicToken)
        val exchangeResp = plaidApi.itemPublicTokenExchange(exchangeReq).execute()
        if (!exchangeResp.isSuccessful || exchangeResp.body() == null) {
            val errBody = exchangeResp.errorBody()?.string()
            log.error("Plaid publicTokenExchange failed: code={}, body={}", exchangeResp.code(), errBody)
            error("Failed to exchange Plaid public token (code=${exchangeResp.code()})")
        }
        val exchanged = exchangeResp.body()!!
        val accessToken = exchanged.accessToken
        val plaidItemId = exchanged.itemId

        val institutionName = institution?.name ?: "Unknown Institution"
        val institutionId = institution?.institutionId

        val item =
            itemRepository.save(
                PlaidItem(
                    itemId = plaidItemId,
                    accessToken = accessToken,
                    institutionName = institutionName,
                    institutionId = institutionId,
                    userId = userId,
                ),
            )

        val accountsReq = AccountsGetRequest().accessToken(accessToken)
        val accountsResp = plaidApi.accountsGet(accountsReq).execute()
        if (!accountsResp.isSuccessful || accountsResp.body() == null) {
            val errBody = accountsResp.errorBody()?.string()
            log.error("Plaid accountsGet failed: code={}, body={}", accountsResp.code(), errBody)
            error("Failed to fetch Plaid accounts (code=${accountsResp.code()})")
        }

        val persisted =
            accountsResp.body()!!.accounts.map { acct ->
                val balances = acct.balances
                accountRepository.save(
                    PlaidAccount(
                        itemId = item.id,
                        plaidAccountId = acct.accountId,
                        name = acct.name,
                        officialName = acct.officialName,
                        mask = acct.mask,
                        type = acct.type?.value ?: "unknown",
                        subtype = acct.subtype?.value,
                        currentBalance = balances?.current?.let { BigDecimal.valueOf(it) },
                        availableBalance = balances?.available?.let { BigDecimal.valueOf(it) },
                        isoCurrencyCode = balances?.isoCurrencyCode,
                        included = true,
                    ),
                )
            }

        return persisted.map { it.toResponse(item.institutionName) }
    }

    @Transactional(readOnly = true)
    fun listAccountsForUser(userId: UUID = ledgerProperties.localUserId): List<AccountResponse> {
        val items = itemRepository.findAllByUserId(userId)
        if (items.isEmpty()) return emptyList()
        val itemById = items.associateBy { it.id }
        val accounts = accountRepository.findAllByItemIdIn(itemById.keys)
        return accounts.map { acct ->
            val inst = itemById[acct.itemId]
            acct.toResponse(inst?.institutionName ?: "Unknown Institution")
        }
    }
}

internal fun PlaidAccount.toResponse(institutionName: String): AccountResponse =
    AccountResponse(
        id = id,
        plaidAccountId = plaidAccountId,
        name = name,
        officialName = officialName,
        mask = mask,
        type = type,
        subtype = subtype,
        currentBalance = currentBalance,
        availableBalance = availableBalance,
        isoCurrencyCode = isoCurrencyCode,
        included = included,
        institutionName = institutionName,
    )
