package ai.ledger.backend.web

import com.plaid.client.model.AccountBalance
import com.plaid.client.model.AccountBase
import com.plaid.client.model.AccountSubtype
import com.plaid.client.model.AccountType
import com.plaid.client.model.AccountsGetResponse
import com.plaid.client.model.ItemPublicTokenExchangeResponse
import com.plaid.client.model.PersonalFinanceCategory
import com.plaid.client.model.Transaction
import com.plaid.client.model.TransactionsSyncResponse
import com.plaid.client.request.PlaidApi
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import retrofit2.Call
import retrofit2.Response
import java.time.LocalDate
import org.mockito.Mockito.mock as mockOf

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class FeatureEndpointsTest {
    @Autowired private lateinit var mvc: MockMvc

    @MockBean private lateinit var plaidApi: PlaidApi

    // Ordered: this class shares one in-memory DB, so the empty-state
    // assertions must run before the flow test links anything.
    @Test
    @Order(1)
    fun `summary and transactions are empty when nothing is linked`() {
        mvc
            .perform(get("/internal/transactions"))
            .andExpect(status().isOk)
            .andExpect(content().json("[]"))

        mvc
            .perform(get("/internal/summary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.net_worth").value(0))
            .andExpect(jsonPath("$.account_count").value(0))
            .andExpect(jsonPath("$.spending_by_category").isEmpty)
    }

    @Test
    @Order(2)
    fun `link then sync then refresh full flow`() {
        // -- 1. Exchange: persists one item with one checking account.
        val exchanged =
            ItemPublicTokenExchangeResponse()
                .accessToken("access-sandbox-features")
                .itemId("item-sandbox-features")
        val exchangeCall: Call<ItemPublicTokenExchangeResponse> = mockOf()
        whenever(exchangeCall.execute()).thenReturn(Response.success(exchanged))
        whenever(plaidApi.itemPublicTokenExchange(any())).thenReturn(exchangeCall)

        val acct =
            AccountBase()
                .accountId("acc-features-1")
                .name("Plaid Checking")
                .mask("0000")
                .type(AccountType.DEPOSITORY)
                .subtype(AccountSubtype.CHECKING)
                .balances(AccountBalance().current(110.0).available(100.0).isoCurrencyCode("USD"))
        val accountsResp = AccountsGetResponse().accounts(listOf(acct))
        val accountsCall: Call<AccountsGetResponse> = mockOf()
        whenever(accountsCall.execute()).thenReturn(Response.success(accountsResp))
        whenever(plaidApi.accountsGet(any())).thenReturn(accountsCall)

        mvc
            .perform(
                post("/internal/plaid/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{ "public_token": "public-sandbox-features", "institution": { "name": "Test Bank", "institution_id": "ins_1" } }""",
                    ),
            ).andExpect(status().isOk)

        // -- 2. Sync: Plaid returns one spending transaction this month.
        val tx =
            Transaction()
                .transactionId("tx-features-1")
                .accountId("acc-features-1")
                .amount(42.5)
                .isoCurrencyCode("USD")
                .date(LocalDate.now())
                .name("Blue Bottle Coffee")
                .merchantName("Blue Bottle")
                .pending(false)
                .personalFinanceCategory(
                    PersonalFinanceCategory()
                        .primary("FOOD_AND_DRINK")
                        .detailed("FOOD_AND_DRINK_COFFEE"),
                )
        val syncResp =
            TransactionsSyncResponse()
                .added(listOf(tx))
                .modified(emptyList())
                .removed(emptyList())
                .nextCursor("cursor-1")
                .hasMore(false)
        val syncCall: Call<TransactionsSyncResponse> = mockOf()
        whenever(syncCall.execute()).thenReturn(Response.success(syncResp))
        whenever(plaidApi.transactionsSync(any())).thenReturn(syncCall)

        mvc
            .perform(post("/internal/transactions/sync"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.added").value(1))
            .andExpect(jsonPath("$.removed").value(0))
            .andExpect(jsonPath("$.total").value(1))

        // Re-syncing upserts instead of duplicating (unique plaid_transaction_id).
        mvc
            .perform(post("/internal/transactions/sync"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))

        mvc
            .perform(get("/internal/transactions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Blue Bottle Coffee"))
            .andExpect(jsonPath("$[0].amount").value(42.5))
            .andExpect(jsonPath("$[0].category").value("FOOD_AND_DRINK"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("access_token"))))

        // -- 3. Summary reflects the account balance and month spending.
        mvc
            .perform(get("/internal/summary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.net_worth").value(110.0))
            .andExpect(jsonPath("$.total_assets").value(110.0))
            .andExpect(jsonPath("$.month_spending").value(42.5))
            .andExpect(jsonPath("$.spending_by_category[0].category").value("FOOD_AND_DRINK"))

        // -- 4. Balance refresh pulls new numbers from Plaid.
        val refreshedAcct =
            AccountBase()
                .accountId("acc-features-1")
                .name("Plaid Checking")
                .type(AccountType.DEPOSITORY)
                .balances(AccountBalance().current(90.0).available(80.0).isoCurrencyCode("USD"))
        val refreshResp = AccountsGetResponse().accounts(listOf(refreshedAcct))
        val refreshCall: Call<AccountsGetResponse> = mockOf()
        whenever(refreshCall.execute()).thenReturn(Response.success(refreshResp))
        whenever(plaidApi.accountsBalanceGet(any())).thenReturn(refreshCall)

        mvc
            .perform(post("/internal/accounts/refresh"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].current_balance").value(90.0))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("access_token"))))

        mvc
            .perform(get("/internal/summary"))
            .andExpect(jsonPath("$.net_worth").value(90.0))
    }
}
