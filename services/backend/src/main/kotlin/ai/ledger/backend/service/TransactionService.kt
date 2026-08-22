package ai.ledger.backend.service

import ai.ledger.backend.config.LedgerProperties
import ai.ledger.backend.domain.PlaidAccount
import ai.ledger.backend.domain.PlaidTransaction
import ai.ledger.backend.repo.PlaidAccountRepository
import ai.ledger.backend.repo.PlaidItemRepository
import ai.ledger.backend.repo.PlaidTransactionRepository
import ai.ledger.backend.web.dto.TransactionResponse
import ai.ledger.backend.web.dto.TransactionsSyncResult
import com.plaid.client.model.Transaction
import com.plaid.client.model.TransactionsSyncRequest
import com.plaid.client.request.PlaidApi
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class TransactionService(
    private val plaidApi: PlaidApi,
    private val itemRepository: PlaidItemRepository,
    private val accountRepository: PlaidAccountRepository,
    private val transactionRepository: PlaidTransactionRepository,
    private val ledgerProperties: LedgerProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Incremental sync via Plaid /transactions/sync. Each item keeps its own
     * cursor, so repeated calls only pull the delta since the last sync.
     */
    @Transactional
    fun sync(): TransactionsSyncResult {
        // localUserId is read inside the body (not as a default argument)
        // to stay safe under CGLIB transactional proxies.
        val userId = ledgerProperties.localUserId
        val items = itemRepository.findAllByUserId(userId)
        var added = 0
        var modified = 0
        var removed = 0

        for (item in items) {
            val accountsByPlaidId =
                accountRepository
                    .findAllByItemIdIn(listOf(item.id))
                    .associateBy { it.plaidAccountId }
            var cursor = item.transactionsCursor
            var hasMore = true
            while (hasMore) {
                val request = TransactionsSyncRequest().accessToken(item.accessToken).count(500)
                cursor?.let { request.cursor(it) }
                val response = plaidApi.transactionsSync(request).execute()
                if (!response.isSuccessful || response.body() == null) {
                    val errBody = response.errorBody()?.string()
                    log.error("Plaid transactionsSync failed: code={}, body={}", response.code(), errBody)
                    error("Failed to sync Plaid transactions (code=${response.code()})")
                }
                val body = response.body()!!
                added += upsert(body.added, accountsByPlaidId)
                modified += upsert(body.modified, accountsByPlaidId)
                val removedIds = body.removed.mapNotNull { it.transactionId }
                if (removedIds.isNotEmpty()) {
                    transactionRepository.deleteAllByPlaidTransactionIdIn(removedIds)
                    removed += removedIds.size
                }
                cursor = body.nextCursor
                hasMore = body.hasMore
            }
            item.transactionsCursor = cursor
            item.lastSyncedAt = Instant.now()
            itemRepository.save(item)
        }

        val accountIds = accountIdsForUser(userId)
        val total = if (accountIds.isEmpty()) 0L else transactionRepository.countByAccountIdIn(accountIds)
        return TransactionsSyncResult(added = added, modified = modified, removed = removed, total = total)
    }

    @Transactional(readOnly = true)
    fun list(limit: Int): List<TransactionResponse> {
        val accounts = includedAccountsForUser(ledgerProperties.localUserId)
        if (accounts.isEmpty()) return emptyList()
        val accountById = accounts.associateBy { it.id }
        val safeLimit = limit.coerceIn(1, 500)
        return transactionRepository
            .findAllByAccountIdInOrderByDateDescCreatedAtDesc(accountById.keys, PageRequest.of(0, safeLimit))
            .map { it.toResponse(accountById[it.accountId]?.name ?: "Unknown account") }
    }

    private fun upsert(
        transactions: List<Transaction>,
        accountsByPlaidId: Map<String, PlaidAccount>,
    ): Int {
        if (transactions.isEmpty()) return 0
        val existingByPlaidId =
            transactionRepository
                .findAllByPlaidTransactionIdIn(transactions.map { it.transactionId })
                .associateBy { it.plaidTransactionId }
        var written = 0
        for (tx in transactions) {
            val account = accountsByPlaidId[tx.accountId] ?: continue
            val entity =
                existingByPlaidId[tx.transactionId]
                    ?: PlaidTransaction(
                        accountId = account.id,
                        plaidTransactionId = tx.transactionId,
                        name = tx.name ?: "Unknown",
                        amount = BigDecimal.valueOf(tx.amount),
                        date = tx.date,
                    )
            entity.name = tx.name ?: entity.name
            entity.merchantName = tx.merchantName
            entity.amount = BigDecimal.valueOf(tx.amount)
            entity.isoCurrencyCode = tx.isoCurrencyCode
            entity.date = tx.date
            entity.pending = tx.pending
            entity.categoryPrimary = tx.personalFinanceCategory?.primary
            entity.categoryDetailed = tx.personalFinanceCategory?.detailed
            entity.paymentChannel = tx.paymentChannel?.value
            transactionRepository.save(entity)
            written++
        }
        return written
    }

    private fun includedAccountsForUser(userId: UUID): List<PlaidAccount> {
        val items = itemRepository.findAllByUserId(userId)
        if (items.isEmpty()) return emptyList()
        return accountRepository.findAllByItemIdIn(items.map { it.id }.toSet()).filter { it.included }
    }

    private fun accountIdsForUser(userId: UUID): Set<UUID> {
        val items = itemRepository.findAllByUserId(userId)
        if (items.isEmpty()) return emptySet()
        return accountRepository.findAllByItemIdIn(items.map { it.id }.toSet()).map { it.id }.toSet()
    }
}

internal fun PlaidTransaction.toResponse(accountName: String): TransactionResponse =
    TransactionResponse(
        id = id,
        accountId = accountId,
        accountName = accountName,
        name = name,
        merchantName = merchantName,
        amount = amount,
        isoCurrencyCode = isoCurrencyCode,
        date = date.toString(),
        pending = pending,
        category = categoryPrimary,
        categoryDetailed = categoryDetailed,
        paymentChannel = paymentChannel,
    )
