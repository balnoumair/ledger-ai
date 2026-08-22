package ai.ledger.backend.web

import ai.ledger.backend.service.TransactionService
import ai.ledger.backend.web.dto.TransactionResponse
import ai.ledger.backend.web.dto.TransactionsSyncResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/transactions")
class TransactionController(
    private val transactionService: TransactionService,
) {
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "50") limit: Int,
    ): List<TransactionResponse> = transactionService.list(limit)

    @PostMapping("/sync")
    fun sync(): TransactionsSyncResult = transactionService.sync()
}
