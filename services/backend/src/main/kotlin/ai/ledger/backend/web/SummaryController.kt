package ai.ledger.backend.web

import ai.ledger.backend.service.SummaryService
import ai.ledger.backend.web.dto.SummaryResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/summary")
class SummaryController(
    private val summaryService: SummaryService,
) {
    @GetMapping
    fun summary(): SummaryResponse = summaryService.summary()
}
