package com.frameload.cash.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.frameload.cash.dto.AccountRequest;
import com.frameload.cash.dto.AccountResponse;
import com.frameload.cash.dto.ClientRequest;
import com.frameload.cash.dto.ClientResponse;
import com.frameload.cash.enums.AccountStatus;
import com.frameload.cash.enums.AccountType;
import com.frameload.cash.enums.ClientStatus;
import com.frameload.cash.exception.BusinessRuleException;
import com.frameload.cash.exception.ResourceNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Teste les comptes avec la vraie persistance sur la base H2 du profil "test".
 */
@SpringBootTest
@ActiveProfiles("test")
class AccountServicesImplTest {

    @Autowired
    private AccountServices accountServices;

    @Autowired
    private ClientServices clientServices;

    private ClientResponse newClient() {
        return clientServices.create(new ClientRequest("Awa", "Nkoulou",
                "awa-" + UUID.randomUUID() + "@example.test", "+237 670 00 00 01", "Yaoundé"));
    }

    private AccountResponse newAccount(Long clientId) {
        return accountServices.create(new AccountRequest(clientId, AccountType.CURRENT));
    }

    @Test
    void create_opensAnEmptyActiveAccount() {
        ClientResponse client = newClient();

        AccountResponse account = accountServices.create(new AccountRequest(client.id(), AccountType.SAVING));

        assertThat(account.accountNumber()).isEqualTo(String.format("FC-%06d", account.id()));
        assertThat(account.balance()).isEqualByComparingTo("0");
        assertThat(account.currency()).isEqualTo("XAF");
        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.accountType()).isEqualTo(AccountType.SAVING);
        assertThat(account.clientId()).isEqualTo(client.id());
    }

    @Test
    void create_givesEveryAccountADifferentNumber() {
        ClientResponse client = newClient();

        AccountResponse first = newAccount(client.id());
        AccountResponse second = newAccount(client.id());

        assertThat(first.accountNumber()).isNotEqualTo(second.accountNumber());
    }

    @Test
    void create_forInactiveClient_isRejected() {
        ClientResponse client = newClient();
        clientServices.changeStatus(client.id(), ClientStatus.INACTIVE);

        assertThatThrownBy(() -> newAccount(client.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("désactivé");
    }

    @Test
    void create_forUnknownClient_throwsNotFound() {
        assertThatThrownBy(() -> newAccount(999_999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deactivatingTheClient_keepsItsExistingAccounts() {
        ClientResponse client = newClient();
        AccountResponse account = newAccount(client.id());

        clientServices.changeStatus(client.id(), ClientStatus.INACTIVE);

        assertThat(accountServices.findById(account.id()).status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void findAll_containsTheCreatedAccount() {
        AccountResponse account = newAccount(newClient().id());

        assertThat(accountServices.findAll()).extracting(AccountResponse::id).contains(account.id());
    }

    @Test
    void findById_returnsBalanceAndStatus() {
        AccountResponse created = newAccount(newClient().id());

        AccountResponse found = accountServices.findById(created.id());

        assertThat(found.balance()).isEqualByComparingTo("0");
        assertThat(found.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void findById_withUnknownId_throwsNotFound() {
        assertThatThrownBy(() -> accountServices.findById(999_999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByAccountNumber_returnsTheAccountOrThrowsNotFound() {
        AccountResponse created = newAccount(newClient().id());

        assertThat(accountServices.findByAccountNumber(created.accountNumber()).id()).isEqualTo(created.id());
        assertThatThrownBy(() -> accountServices.findByAccountNumber("FC-999999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByClientId_returnsOnlyTheAccountsOfThatClient() {
        ClientResponse client = newClient();
        newAccount(client.id());
        newAccount(client.id());
        newAccount(newClient().id());

        assertThat(accountServices.findByClientId(client.id())).hasSize(2);
    }

    @Test
    void findByClientId_withUnknownClient_throwsNotFound() {
        assertThatThrownBy(() -> accountServices.findByClientId(999_999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changeStatus_blocksTheAccount() {
        AccountResponse account = newAccount(newClient().id());

        accountServices.changeStatus(account.id(), AccountStatus.BLOCKED);

        assertThat(accountServices.findById(account.id()).status()).isEqualTo(AccountStatus.BLOCKED);
    }

    @Test
    void closing_keepsTheAccountConsultableAndFinal() {
        AccountResponse account = newAccount(newClient().id());

        accountServices.changeStatus(account.id(), AccountStatus.CLOSED);

        assertThat(accountServices.findById(account.id()).status()).isEqualTo(AccountStatus.CLOSED);
        assertThatThrownBy(() -> accountServices.changeStatus(account.id(), AccountStatus.ACTIVE))
                .isInstanceOf(BusinessRuleException.class);
    }
}