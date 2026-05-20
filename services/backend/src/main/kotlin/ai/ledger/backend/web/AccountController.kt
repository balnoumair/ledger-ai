package ai.ledger.backend.web

import ai.ledger.backend.service.AccountService
import ai.ledger.backend.service.PlaidService
import ai.ledger.backend.web.dto.AccountResponse
import ai.ledger.backend.web.dto.UpdateAccountIncludedRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/internal/accounts")
class AccountController(
    private val plaidService: PlaidService,
    private val accountService: AccountService,
) {
    @GetMapping
    fun list(): List<AccountResponse> = plaidService.listAccountsForUser()

    @PatchMapping("/{id}")
    fun updateIncluded(
        @PathVariable id: UUID,
        @RequestBody body: UpdateAccountIncludedRequest,
    ): AccountResponse = accountService.updateIncluded(id, body.included)
}
