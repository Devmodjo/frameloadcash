package com.frameload.cash.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.frameload.cash.dto.AccountRequest;
import com.frameload.cash.dto.AccountResponse;
import com.frameload.cash.dto.CardRequest;
import com.frameload.cash.dto.CardResponse;
import com.frameload.cash.dto.ClientRequest;
import com.frameload.cash.enums.AccountType;
import com.frameload.cash.enums.CardStatus;
import com.frameload.cash.enums.CardType;
import com.frameload.cash.exception.BusinessRuleException;
import com.frameload.cash.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Teste la gestion des cartes fictives avec la vraie persistance (H2) du profil "test".
 */
@SpringBootTest
@ActiveProfiles("test")
class CardServicesImplTest {

    @Autowired
    private CardServices cardServices;

    @Autowired
    private AccountServices accountServices;

    @Autowired
    private ClientServices clientServices;

    private AccountResponse newAccount() {
        Long clientId = clientServices.create(new ClientRequest("Awa", "Nkoulou",
                "awa-" + UUID.randomUUID() + "@example.test", "+237 670 00 00 01", "Yaoundé")).id();
        return accountServices.create(new AccountRequest(clientId, AccountType.CURRENT));
    }

    private CardResponse newCard(Long accountId) {
        return cardServices.create(accountId, new CardRequest(CardType.DEBIT));
    }

    @Test
    void create_opensAnActiveCardWithAFictiveReferenceAndExpiration() {
        AccountResponse account = newAccount();

        CardResponse card = cardServices.create(account.id(), new CardRequest(CardType.PREPAID));

        assertThat(card.cardReference()).isEqualTo(String.format("CD-%06d", card.id()));
        assertThat(card.status()).isEqualTo(CardStatus.ACTIVE);
        assertThat(card.cardType()).isEqualTo(CardType.PREPAID);
        assertThat(card.accountId()).isEqualTo(account.id());
        assertThat(card.expirationDate()).isAfter(LocalDate.now());
    }

    @Test
    void create_givesEveryCardADifferentReference() {
        AccountResponse account = newAccount();

        assertThat(newCard(account.id()).cardReference()).isNotEqualTo(newCard(account.id()).cardReference());
    }

    @Test
    void create_forAnUnknownAccount_throwsNotFound() {
        assertThatThrownBy(() -> newCard(999_999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByAccountId_returnsOnlyTheCardsOfThatAccount() {
        AccountResponse account = newAccount();
        newCard(account.id());
        newCard(account.id());
        newCard(newAccount().id());

        assertThat(cardServices.findByAccountId(account.id())).hasSize(2);
    }

    @Test
    void findByAccountId_forAnUnknownAccount_throwsNotFound() {
        assertThatThrownBy(() -> cardServices.findByAccountId(999_999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findById_returnsTheStatusOrThrowsNotFound() {
        CardResponse created = newCard(newAccount().id());

        assertThat(cardServices.findById(created.id()).status()).isEqualTo(CardStatus.ACTIVE);
        assertThatThrownBy(() -> cardServices.findById(999_999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changeStatus_blocksThenReactivatesACard() {
        CardResponse card = newCard(newAccount().id());

        assertThat(cardServices.changeStatus(card.id(), CardStatus.BLOCKED).status()).isEqualTo(CardStatus.BLOCKED);
        assertThat(cardServices.changeStatus(card.id(), CardStatus.ACTIVE).status()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void changeStatus_anExpiredCardCanNeverBeReactivated() {
        CardResponse card = newCard(newAccount().id());
        cardServices.changeStatus(card.id(), CardStatus.EXPIRED);

        assertThatThrownBy(() -> cardServices.changeStatus(card.id(), CardStatus.ACTIVE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expirée");
        assertThat(cardServices.findById(card.id()).status()).isEqualTo(CardStatus.EXPIRED);
    }

    @Test
    void changeStatus_forAnUnknownCard_throwsNotFound() {
        assertThatThrownBy(() -> cardServices.changeStatus(999_999L, CardStatus.BLOCKED))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}