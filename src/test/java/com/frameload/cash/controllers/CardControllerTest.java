package com.frameload.cash.controllers;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.frameload.cash.dto.AccountRequest;
import com.frameload.cash.dto.AccountResponse;
import com.frameload.cash.dto.CardRequest;
import com.frameload.cash.dto.CardResponse;
import com.frameload.cash.dto.ClientRequest;
import com.frameload.cash.enums.AccountType;
import com.frameload.cash.enums.CardType;
import com.frameload.cash.services.AccountServices;
import com.frameload.cash.services.CardServices;
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
 * Teste le contrat HTTP des cartes : codes de statut, format JSON, messages d'erreur et absence de données de paiement.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientServices clientServices;

    @Autowired
    private AccountServices accountServices;

    @Autowired
    private CardServices cardServices;

    private AccountResponse newAccount() {
        Long clientId = clientServices.create(new ClientRequest("Awa", "Nkoulou",
                "awa-" + UUID.randomUUID() + "@example.test", "+237 670 00 00 01", "Yaoundé")).id();
        return accountServices.create(new AccountRequest(clientId, AccountType.CURRENT));
    }

    private CardResponse newCard() {
        return cardServices.create(newAccount().id(), new CardRequest(CardType.DEBIT));
    }

    @Test
    void createCard_returns201WithReferenceStatusAndExpiration() throws Exception {
        AccountResponse account = newAccount();

        mockMvc.perform(post("/api/v1/accounts/" + account.id() + "/cards")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cardType\":\"DEBIT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardReference", matchesPattern("CD-\\d{6}")))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.accountId").value(account.id()))
                .andExpect(jsonPath("$.expirationDate").exists());
    }

    @Test
    void createCard_neverExposesPaymentData() throws Exception {
        AccountResponse account = newAccount();

        mockMvc.perform(post("/api/v1/accounts/" + account.id() + "/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardType\":\"DEBIT\",\"cardNumber\":\"4111111111111111\",\"cvv\":\"123\",\"pin\":\"0000\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardNumber").doesNotExist())
                .andExpect(jsonPath("$.cvv").doesNotExist())
                .andExpect(jsonPath("$.pin").doesNotExist());
    }

    @Test
    void createCard_withoutOrWithUnknownType_returns400() throws Exception {
        long accountId = newAccount().id();

        mockMvc.perform(post("/api/v1/accounts/" + accountId + "/cards")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/v1/accounts/" + accountId + "/cards")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cardType\":\"GOLD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void createCard_forAnUnknownAccount_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/999999/cards")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cardType\":\"DEBIT\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listCardsOfAnAccount_returnsTheCardsOr404() throws Exception {
        AccountResponse account = newAccount();
        cardServices.create(account.id(), new CardRequest(CardType.DEBIT));
        cardServices.create(account.id(), new CardRequest(CardType.PREPAID));

        mockMvc.perform(get("/api/v1/accounts/" + account.id() + "/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/v1/accounts/999999/cards")).andExpect(status().isNotFound());
    }

    @Test
    void getCard_returnsTheStatusOr404() throws Exception {
        CardResponse card = newCard();

        mockMvc.perform(get("/api/v1/cards/" + card.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(get("/api/v1/cards/999999")).andExpect(status().isNotFound());
    }

    @Test
    void changeStatus_blocksThenReactivatesTheCard() throws Exception {
        CardResponse card = newCard();

        mockMvc.perform(patch("/api/v1/cards/" + card.id() + "/status").param("status", "BLOCKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
        mockMvc.perform(patch("/api/v1/cards/" + card.id() + "/status").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void changeStatus_withAnUnknownStatus_returns400() throws Exception {
        CardResponse card = newCard();

        mockMvc.perform(patch("/api/v1/cards/" + card.id() + "/status").param("status", "SUSPENDUE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void changeStatus_onAnExpiredCard_returns409() throws Exception {
        CardResponse card = newCard();
        mockMvc.perform(patch("/api/v1/cards/" + card.id() + "/status").param("status", "EXPIRED"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/cards/" + card.id() + "/status").param("status", "ACTIVE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CARD_EXPIRED"));
    }
}