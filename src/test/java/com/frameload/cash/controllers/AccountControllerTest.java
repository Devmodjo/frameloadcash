package com.frameload.cash.controllers;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.frameload.cash.dto.AccountRequest;
import com.frameload.cash.dto.AccountResponse;
import com.frameload.cash.dto.ClientRequest;
import com.frameload.cash.enums.AccountStatus;
import com.frameload.cash.enums.AccountType;
import com.frameload.cash.enums.ClientStatus;
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

/**
 * Teste le contrat HTTP des comptes : codes de statut, format JSON et messages d'erreur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientServices clientServices;

    @Autowired
    private AccountServices accountServices;

    private long newClientId() {
        return clientServices.create(new ClientRequest("Awa", "Nkoulou",
                "awa-" + UUID.randomUUID() + "@example.test", "+237 670 00 00 01", "Yaoundé")).id();
    }

    private AccountResponse newAccount() {
        return accountServices.create(new AccountRequest(newClientId(), AccountType.CURRENT));
    }

    private String json(Object clientId, String accountType) {
        return "{\"clientId\":" + clientId + ",\"accountType\":" + accountType + "}";
    }

    @Test
    void createAccount_returns201WithZeroBalance() throws Exception {
        long clientId = newClientId();

        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content(json(clientId, "\"CURRENT\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber", matchesPattern("FC-\\d{6}")))
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.clientId").value(clientId));
    }

    @Test
    void createAccount_ignoresTheBalanceSentByTheCaller() throws Exception {
        long clientId = newClientId();

        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clientId + ",\"accountType\":\"CURRENT\",\"balance\":999999}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value(0.0));
    }

    @Test
    void createAccount_forUnknownClient_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content(json(999999, "\"CURRENT\"")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void createAccount_withoutType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content(json(newClientId(), "null")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void createAccount_withUnknownType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content(json(newClientId(), "\"BUSINESS\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void createAccount_forInactiveClient_returns409() throws Exception {
        long clientId = newClientId();
        clientServices.changeStatus(clientId, ClientStatus.INACTIVE);

        mockMvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content(json(clientId, "\"CURRENT\"")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CLIENT_INACTIVE"));
    }

    @Test
    void listAccounts_containsTheCreatedAccount() throws Exception {
        AccountResponse account = newAccount();

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(account.id().intValue())));
    }

    @Test
    void getAccount_returnsBalanceAndStatus() throws Exception {
        AccountResponse account = newAccount();

        mockMvc.perform(get("/api/v1/accounts/" + account.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(account.accountNumber()))
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getAccount_withUnknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/999999")).andExpect(status().isNotFound());
    }

    @Test
    void getAccountByNumber_returnsTheAccountOr404() throws Exception {
        AccountResponse account = newAccount();

        mockMvc.perform(get("/api/v1/accounts/number/" + account.accountNumber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(account.id()));
        mockMvc.perform(get("/api/v1/accounts/number/FC-999999")).andExpect(status().isNotFound());
    }

    @Test
    void listAccountsOfAClient_returnsOnlyHisAccounts() throws Exception {
        long clientId = newClientId();
        accountServices.create(new AccountRequest(clientId, AccountType.CURRENT));
        accountServices.create(new AccountRequest(clientId, AccountType.SAVING));
        newAccount();

        mockMvc.perform(get("/api/v1/clients/" + clientId + "/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].clientId").value(clientId));
    }

    @Test
    void listAccountsOfAClient_withUnknownClient_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/clients/999999/accounts")).andExpect(status().isNotFound());
    }

    @Test
    void changeStatus_blocksTheAccount() throws Exception {
        AccountResponse account = newAccount();

        mockMvc.perform(patch("/api/v1/accounts/" + account.id() + "/status").param("status", "BLOCKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void changeStatus_onAClosedAccount_returns409() throws Exception {
        AccountResponse account = newAccount();
        accountServices.changeStatus(account.id(), AccountStatus.CLOSED);

        mockMvc.perform(patch("/api/v1/accounts/" + account.id() + "/status").param("status", "ACTIVE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ACCOUNT_CLOSED"));
    }

    @Test
    void changeStatus_withUnknownStatus_returns400() throws Exception {
        AccountResponse account = newAccount();

        mockMvc.perform(patch("/api/v1/accounts/" + account.id() + "/status").param("status", "SUSPENDU"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void changeStatus_withUnknownAccount_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/accounts/999999/status").param("status", "BLOCKED"))
                .andExpect(status().isNotFound());
    }
}