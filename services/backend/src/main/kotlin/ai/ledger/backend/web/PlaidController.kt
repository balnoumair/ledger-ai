package ai.ledger.backend.web

import ai.ledger.backend.service.PlaidService
import ai.ledger.backend.web.dto.AccountResponse
import ai.ledger.backend.web.dto.ExchangeRequest
import ai.ledger.backend.web.dto.LinkTokenResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/plaid")
class PlaidController(
    private val plaidService: PlaidService,
) {
    @PostMapping("/link-token")
    fun createLinkToken(): LinkTokenResponse = plaidService.createLinkToken()

    @PostMapping("/exchange")
    fun exchange(
        @Valid @RequestBody body: ExchangeRequest,
    ): List<AccountResponse> = plaidService.exchangePublicToken(body.publicToken, body.institution)
}
