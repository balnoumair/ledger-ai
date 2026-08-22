package ai.ledger.backend.service

import ai.ledger.backend.config.LedgerProperties
import ai.ledger.backend.repo.PlaidAccountRepository
import ai.ledger.backend.repo.PlaidItemRepository
import ai.ledger.backend.repo.PlaidTransactionRepository
import ai.ledger.backend.web.dto.CategorySpend
import ai.ledger.backend.web.dto.SummaryResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.YearMonth

/** Account types Plaid reports as money owed rather than money held. */
private val LIABILITY_TYPES = setOf("credit", "loan")

@Service
class SummaryService(
    private val itemRepository: PlaidItemRepository,
    private val accountRepository: PlaidAccountRepository,
    private val transactionRepository: PlaidTransactionRepository,
    private val ledgerProperties: LedgerProperties,
) {
    @Transactional(readOnly = true)
    fun summary(): SummaryResponse {
        // localUserId is read inside the body (not as a default argument)
        // to stay safe under CGLIB transactional proxies.
        val items = itemRepository.findAllByUserId(ledgerProperties.localUserId)
        val accounts =
            if (items.isEmpty()) {
                emptyList()
            } else {
                accountRepository.findAllByItemIdIn(items.map { it.id }.toSet()).filter { it.included }
            }

        var assets = BigDecimal.ZERO
        var liabilities = BigDecimal.ZERO
        for (account in accounts) {
            val balance = account.currentBalance ?: continue
            if (account.type.lowercase() in LIABILITY_TYPES) {
                liabilities = liabilities.add(balance)
            } else {
                assets = assets.add(balance)
            }
        }

        val month = YearMonth.now()
        val monthTransactions =
            if (accounts.isEmpty()) {
                emptyList()
            } else {
                transactionRepository.findAllByAccountIdInAndDateGreaterThanEqual(
                    accounts.map { it.id }.toSet(),
                    month.atDay(1),
                )
            }

        // Plaid convention: positive amount = money out, negative = money in.
        val outflows = monthTransactions.filter { it.amount.signum() > 0 }
        val monthSpending = outflows.sumOf { it.amount }
        val monthIncome = monthTransactions.filter { it.amount.signum() < 0 }.sumOf { it.amount.negate() }
        val spendingByCategory =
            outflows
                .groupBy { it.categoryPrimary ?: "OTHER" }
                .map { (category, txs) -> CategorySpend(category = category, amount = txs.sumOf { tx -> tx.amount }) }
                .sortedByDescending { it.amount }

        return SummaryResponse(
            netWorth = assets.subtract(liabilities),
            totalAssets = assets,
            totalLiabilities = liabilities,
            accountCount = accounts.size,
            monthSpending = monthSpending,
            monthIncome = monthIncome,
            spendingByCategory = spendingByCategory,
            month = month.toString(),
        )
    }
}
