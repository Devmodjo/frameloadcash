package com.frameload.cash.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.frameload.cash.dto.AccountRequest;
import com.frameload.cash.dto.AccountResponse;
import com.frameload.cash.dto.ClientRequest;
import com.frameload.cash.enums.AccountStatus;
import com.frameload.cash.enums.AccountType;
import com.frameload.cash.services.AccountServices;
import com.frameload.cash.services.ClientServices;
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
 * Teste le contrat HTTP des dépôts : codes de statut, format de réponse et messages d'erreur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountOperationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientServices clientServices;

    @Autowired
    private AccountServices accountServices;

    private AccountResponse newAccount() {
        Long clientId = clientServices.create(new ClientRequest("Awa", "Nkoulou",
                "awa-" + UUID.randomUUID() + "@example.test", "+237 670 00 00 01", "Yaoundé")).id();
        return accountServices.create(new AccountRequest(clientId, AccountType.CURRENT));
    }

    private ResultActions deposit(Object accountId, String amountJson) throws Exception {
        return mockMvc.perform(post("/api/v1/accounts/" + accountId + "/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":" + amountJson + ",\"description\":\"Dépôt de démonstration\"}"));
    }

    @Test
    void deposit_returns201WithReferenceAndNewBalance() throws Exception {
        AccountResponse account = newAccount();

        deposit(account.id(), "25000")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Dépôt effectué avec succès."))
                .andExpect(jsonPath("$.reference", matchesPattern("TX-\\d{6}")))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.accountId").value(account.id()))
                .andExpect(jsonPath("$.data.amount").value(25000))
                .andExpect(jsonPath("$.data.newBalance").value(25000.0));
    }

    @Test
    void deposit_withZeroOrNegativeAmount_returns400AndKeepsTheBalance() throws Exception {
        AccountResponse account = newAccount();

        deposit(account.id(), "0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("supérieur à zéro")));
        deposit(account.id(), "-500").andExpect(status().isBadRequest());

        assertThat(accountServices.findById(account.id()).balance()).isEqualByComparingTo("0");
    }

    @Test
    void deposit_withNonNumericAmount_returns400() throws Exception {
        deposit(newAccount().id(), "\"abc\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void deposit_withTooManyDecimals_returns400() throws Exception {
        deposit(newAccount().id(), "10.123").andExpect(status().isBadRequest());
    }

    @Test
    void deposit_withoutAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/" + newAccount().id() + "/deposits")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deposit_onAnUnknownAccount_returns404() throws Exception {
        deposit(999999, "1000").andExpect(status().isNotFound());
    }

    @Test
    void deposit_onABlockedAccount_returns409() throws Exception {
        AccountResponse account = newAccount();
        accountServices.changeStatus(account.id(), AccountStatus.BLOCKED);

        deposit(account.id(), "1000")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_ACTIVE"));
    }
}