package ai.ledger.backend.service

import ai.ledger.backend.repo.PlaidAccountRepository
import ai.ledger.backend.repo.PlaidItemRepository
import ai.ledger.backend.web.dto.AccountResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AccountService(
    private val accountRepository: PlaidAccountRepository,
    private val itemRepository: PlaidItemRepository,
) {
    @Transactional
    fun updateIncluded(
        id: UUID,
        included: Boolean,
    ): AccountResponse {
        val account =
            accountRepository.findById(id).orElseThrow {
                NoSuchElementException("Account $id not found")
            }
        account.included = included
        val saved = accountRepository.save(account)
        val item =
            itemRepository.findById(saved.itemId).orElseThrow {
                IllegalStateException("Orphan account $id (no owning item)")
            }
        return saved.toResponse(item.institutionName)
    }
}
