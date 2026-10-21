package com.frameload.cash.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.frameload.cash.dto.AccountRequest;
import com.frameload.cash.dto.AccountResponse;
import com.frameload.cash.dto.ClientRequest;
import com.frameload.cash.dto.OperationRequest;
import com.frameload.cash.enums.AccountStatus;
import com.frameload.cash.enums.AccountType;
import com.frameload.cash.services.AccountOperationServices;
import com.frameload.cash.services.AccountServices;
import com.frameload.cash.services.ClientServices;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Teste le contrat HTTP des retraits : codes de statut, format de réponse et messages d'erreur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WithdrawalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientServices clientServices;

    @Autowired
    private AccountServices accountServices;

    @Autowired
    private AccountOperationServices operationServices;

    private AccountResponse fundedAccount(String amount) {
        Long clientId = clientServices.create(new ClientRequest("Awa", "Nkoulou",
                "awa-" + UUID.randomUUID() + "@example.test", "+237 670 00 00 01", "Yaoundé")).id();
        AccountResponse account = accountServices.create(new AccountRequest(clientId, AccountType.CURRENT));
        operationServices.deposit(account.id(), new OperationRequest(new BigDecimal(amount), "Provision"));
        return account;
    }

    private ResultActions withdraw(Object accountId, String amountJson) throws Exception {
        return mockMvc.perform(post("/api/v1/accounts/" + accountId + "/withdrawals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":" + amountJson + ",\"description\":\"Retrait de démonstration\"}"));
    }

    @Test
    void withdraw_returns201WithReferenceAndNewBalance() throws Exception {
        AccountResponse account = fundedAccount("125000");

        withdraw(account.id(), "10000")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Retrait effectué avec succès."))
                .andExpect(jsonPath("$.reference", matchesPattern("TX-\\d{6}")))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.accountId").value(account.id()))
                .andExpect(jsonPath("$.data.newBalance").value(115000.0));
    }

    @Test
    void withdraw_withInsufficientBalance_returns409AndKeepsTheBalance() throws Exception {
        AccountResponse account = fundedAccount("5000");

        withdraw(account.id(), "5000.01")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_FUNDS"))
                .andExpect(jsonPath("$.message", containsString("insuffisant")));

        assertThat(accountServices.findById(account.id()).balance()).isEqualByComparingTo("5000");
    }

    @Test
    void withdraw_withZeroOrNegativeAmount_returns400() throws Exception {
        long id = fundedAccount("5000").id();

        withdraw(id, "0").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        withdraw(id, "-1").andExpect(status().isBadRequest());
    }

    @Test
    void withdraw_withNonNumericAmount_returns400() throws Exception {
        withdraw(fundedAccount("5000").id(), "\"dix\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void withdraw_withTooManyDecimals_returns400() throws Exception {
        withdraw(fundedAccount("5000").id(), "10.123").andExpect(status().isBadRequest());
    }

    @Test
    void withdraw_onABlockedAccount_returns409() throws Exception {
        AccountResponse account = fundedAccount("5000");
        accountServices.changeStatus(account.id(), AccountStatus.BLOCKED);

        withdraw(account.id(), "100")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void withdraw_onAnUnknownAccount_returns404() throws Exception {
        withdraw(999999, "100").andExpect(status().isNotFound());
    }
}