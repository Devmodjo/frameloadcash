package com.frameload.cash.controllers;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.frameload.cash.dto.AccountRequest;
import com.frameload.cash.dto.AccountResponse;
import com.frameload.cash.dto.ClientRequest;
import com.frameload.cash.dto.OperationRequest;
import com.frameload.cash.dto.OperationResponse;
import com.frameload.cash.dto.TransferRequest;
import com.frameload.cash.enums.AccountType;
import com.frameload.cash.services.AccountOperationServices;
import com.frameload.cash.services.AccountServices;
import com.frameload.cash.services.ClientServices;
import com.frameload.cash.services.TransferServices;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Teste le contrat HTTP de l'historique : codes de statut, format JSON, filtres et messages d'erreur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientServices clientServices;

    @Autowired
    private AccountServices accountServices;

    @Autowired
    private AccountOperationServices operationServices;

    @Autowired
    private TransferServices transferServices;

    private AccountResponse newAccount() {
        Long clientId = clientServices.create(new ClientRequest("Awa", "Nkoulou",
                "awa-" + UUID.randomUUID() + "@example.test", "+237 670 00 00 01", "Yaoundé")).id();
        return accountServices.create(new AccountRequest(clientId, AccountType.CURRENT));
    }

    private OperationRequest amount(String value) {
        return new OperationRequest(new BigDecimal(value), "Opération de démonstration");
    }

    @Test
    void accountHistory_listsTheOperationsMostRecentFirst() throws Exception {
        AccountResponse account = newAccount();
        operationServices.deposit(account.id(), amount("50000"));
        operationServices.withdraw(account.id(), amount("1000"));

        mockMvc.perform(get("/api/v1/accounts/" + account.id() + "/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$[1].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[0].reference", matchesPattern("TX-\\d{6}")))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void accountHistory_canBeFilteredByTypeAndPeriod() throws Exception {
        AccountResponse account = newAccount();
        operationServices.deposit(account.id(), amount("50000"));
        operationServices.withdraw(account.id(), amount("1000"));
        String today = LocalDate.now().toString();

        mockMvc.perform(get("/api/v1/accounts/" + account.id() + "/transactions")
                        .param("type", "DEPOSIT").param("startDate", today).param("endDate", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"));
    }

    @Test
    void history_filteredByAccountId_returnsOnlyThatAccount() throws Exception {
        AccountResponse account = newAccount();
        operationServices.deposit(account.id(), amount("1000"));
        AccountResponse other = newAccount();
        operationServices.deposit(other.id(), amount("2000"));

        mockMvc.perform(get("/api/v1/transactions").param("accountId", account.id().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accountId").value(account.id()));
    }

    @Test
    void accountHistory_withAnUnknownAccount_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/999999/transactions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void history_withAnInvalidTypeOrDate_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/transactions").param("type", "GIFT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
        mockMvc.perform(get("/api/v1/transactions").param("startDate", "hier"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void history_withAnInvertedPeriod_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/transactions")
                        .param("startDate", "2026-12-31").param("endDate", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    void transactionByReference_returnsTheDetailsOr404() throws Exception {
        AccountResponse account = newAccount();
        OperationResponse deposit = operationServices.deposit(account.id(), amount("1000"));

        mockMvc.perform(get("/api/v1/transactions/" + deposit.reference()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.accountId").value(account.id()))
                .andExpect(jsonPath("$.amount").value(1000.0));
        mockMvc.perform(get("/api/v1/transactions/TX-999999")).andExpect(status().isNotFound());
    }

    @Test
    void aTransferLeg_carriesTheTransferReference() throws Exception {
        AccountResponse source = newAccount();
        operationServices.deposit(source.id(), amount("100000"));
        AccountResponse destination = newAccount();
        OperationResponse transfer = transferServices.transfer(
                new TransferRequest(source.id(), destination.id(), new BigDecimal("30000"), "Virement"));

        mockMvc.perform(get("/api/v1/accounts/" + source.id() + "/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$[0].transferReference").value(transfer.reference()));
        mockMvc.perform(get("/api/v1/accounts/" + destination.id() + "/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("TRANSFER_IN"))
                .andExpect(jsonPath("$[0].transferReference").value(transfer.reference()));
    }
}