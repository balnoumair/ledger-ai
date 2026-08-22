package ai.ledger.backend.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.plaid.client.model.AccountBalance
import com.plaid.client.model.AccountBase
import com.plaid.client.model.AccountSubtype
import com.plaid.client.model.AccountType
import com.plaid.client.model.AccountsGetResponse
import com.plaid.client.model.ItemPublicTokenExchangeResponse
import com.plaid.client.model.LinkTokenCreateResponse
import com.plaid.client.request.PlaidApi
import org.junit.jupiter.api.Test
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
import org.mockito.Mockito.mock as mockOf

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EndpointSmokeTest {
    @Autowired private lateinit var mvc: MockMvc

    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockBean private lateinit var plaidApi: PlaidApi

    @Test
    fun `link-token returns token from plaid`() {
        val resp = LinkTokenCreateResponse().linkToken("link-sandbox-test").expiration(null)
        val call: Call<LinkTokenCreateResponse> = mockOf()
        whenever(call.execute()).thenReturn(Response.success(resp))
        whenever(plaidApi.linkTokenCreate(any())).thenReturn(call)

        mvc
            .perform(post("/internal/plaid/link-token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.link_token").value("link-sandbox-test"))
            // Critically: access_token must never appear in any response.
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("access_token"))))
    }

    @Test
    fun `exchange validates missing public_token`() {
        mvc
            .perform(
                post("/internal/plaid/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `exchange persists item and accounts and never leaks access_token`() {
        val exchanged =
            ItemPublicTokenExchangeResponse()
                .accessToken("access-sandbox-fake")
                .itemId("item-sandbox-fake")
        val exchangeCall: Call<ItemPublicTokenExchangeResponse> = mockOf()
        whenever(exchangeCall.execute()).thenReturn(Response.success(exchanged))
        whenever(plaidApi.itemPublicTokenExchange(any())).thenReturn(exchangeCall)

        val acct =
            AccountBase()
                .accountId("acc-1")
                .name("Plaid Checking")
                .officialName("Plaid Gold Standard 0% Interest Checking")
                .mask("0000")
                .type(AccountType.DEPOSITORY)
                .subtype(AccountSubtype.CHECKING)
                .balances(
                    AccountBalance()
                        .current(110.0)
                        .available(100.0)
                        .isoCurrencyCode("USD"),
                )
        val accountsResp = AccountsGetResponse().accounts(listOf(acct))
        val accountsCall: Call<AccountsGetResponse> = mockOf()
        whenever(accountsCall.execute()).thenReturn(Response.success(accountsResp))
        whenever(plaidApi.accountsGet(any())).thenReturn(accountsCall)

        mvc
            .perform(
                post("/internal/plaid/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "public_token": "public-sandbox-fake",
                          "institution": { "name": "First Platypus Bank", "institution_id": "ins_109508" }
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$[0].plaid_account_id").value("acc-1"))
            .andExpect(jsonPath("$[0].institution_name").value("First Platypus Bank"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("access_token"))))
    }

    @Test
    fun `accounts list is empty when nothing linked`() {
        mvc
            .perform(get("/internal/accounts"))
            .andExpect(status().isOk)
            .andExpect(content().json("[]"))
    }
}
