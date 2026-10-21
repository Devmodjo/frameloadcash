package com.frameload.cash.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.jayway.jsonpath.JsonPath;
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
 * Teste le contrat HTTP des virements : codes de statut, format de réponse et messages d'erreur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransferControllerTest {

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

    private ResultActions transfer(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/transfers").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String json(Object source, Object destination, String amount) {
        return "{\"sourceAccountId\":" + source + ",\"destinationAccountId\":" + destination
                + ",\"amount\":" + amount + ",\"description\":\"Virement de démonstration\"}";
    }

    @Test
    void transfer_returns201WithReferenceAndBothBalances() throws Exception {
        AccountResponse source = fundedAccount("100000");
        AccountResponse destination = fundedAccount("20000");

        transfer(json(source.id(), destination.id(), "30000"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Virement effectué avec succès."))
                .andExpect(jsonPath("$.reference", matchesPattern("TR-\\d{6}")))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.sourceNewBalance").value(70000.0))
                .andExpect(jsonPath("$.data.destinationNewBalance").value(50000.0))
                .andExpect(jsonPath("$.data.debitReference", matchesPattern("TX-\\d{6}")))
                .andExpect(jsonPath("$.data.creditReference", matchesPattern("TX-\\d{6}")));
    }

    @Test
    void transfer_toTheSameAccount_returns409() throws Exception {
        AccountResponse account = fundedAccount("10000");

        transfer(json(account.id(), account.id(), "100"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SAME_ACCOUNT"));
    }

    @Test
    void transfer_withInsufficientBalance_returns409AndKeepsBothBalances() throws Exception {
        AccountResponse source = fundedAccount("100");
        AccountResponse destination = fundedAccount("50");

        transfer(json(source.id(), destination.id(), "100.01"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_FUNDS"));

        assertThat(accountServices.findById(source.id()).balance()).isEqualByComparingTo("100");
        assertThat(accountServices.findById(destination.id()).balance()).isEqualByComparingTo("50");
    }

    @Test
    void transfer_fromABlockedAccount_returns409() throws Exception {
        AccountResponse source = fundedAccount("1000");
        AccountResponse destination = fundedAccount("1");
        accountServices.changeStatus(source.id(), AccountStatus.BLOCKED);

        transfer(json(source.id(), destination.id(), "100"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void transfer_withInvalidAmount_returns400() throws Exception {
        AccountResponse source = fundedAccount("100");
        AccountResponse destination = fundedAccount("1");

        transfer(json(source.id(), destination.id(), "0")).andExpect(status().isBadRequest());
        transfer(json(source.id(), destination.id(), "-5")).andExpect(status().isBadRequest());
        transfer(json(source.id(), destination.id(), "\"beaucoup\"")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void transfer_withoutDestination_returns400() throws Exception {
        transfer("{\"sourceAccountId\":1,\"amount\":100}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void transfer_withAnUnknownAccount_returns404() throws Exception {
        AccountResponse source = fundedAccount("100");

        transfer(json(source.id(), 999999, "10")).andExpect(status().isNotFound());
    }

    @Test
    void getTransfer_returnsTheDetailsOr404() throws Exception {
        AccountResponse source = fundedAccount("100000");
        AccountResponse destination = fundedAccount("1");
        String body = transfer(json(source.id(), destination.id(), "30000"))
                .andReturn().getResponse().getContentAsString();
        String reference = JsonPath.read(body, "$.reference");

        mockMvc.perform(get("/api/v1/transfers/" + reference))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceAccountId").value(source.id()))
                .andExpect(jsonPath("$.destinationAccountId").value(destination.id()))
                .andExpect(jsonPath("$.amount").value(30000.0));
        mockMvc.perform(get("/api/v1/transfers/TR-999999")).andExpect(status().isNotFound());
    }
}