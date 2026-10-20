package com.frameload.cash.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.frameload.cash.dto.AccountRequest;
import com.frameload.cash.entity.Account;
import com.frameload.cash.entity.Client;
import com.frameload.cash.enums.AccountStatus;
import com.frameload.cash.enums.AccountType;
import com.frameload.cash.enums.ClientStatus;
import com.frameload.cash.exception.BusinessRuleException;
import com.frameload.cash.exception.ResourceNotFoundException;
import com.frameload.cash.repositories.AccountRepository;
import com.frameload.cash.repositories.ClientRepository;
import com.frameload.cash.services.impl.AccountServicesImpl;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitaires des règles métier des comptes : les repositories sont simulés (Mockito),
 * aucun contexte Spring et aucune base ne sont nécessaires.
 */
@ExtendWith(MockitoExtension.class)
class AccountServicesImplRulesTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private AccountServicesImpl accountServices;

    private Client newClient() {
        return new Client("Awa", "Nkoulou", "awa@example.test", "+237 670 00 00 01", "Yaoundé");
    }

    private Account newAccount() {
        return new Account(newClient(), AccountType.CURRENT, "XAF");
    }

    @Test
    void create_forInactiveClient_isRejectedAndNothingIsSaved() {
        Client client = newClient();
        client.setStatus(ClientStatus.INACTIVE);
        when(clientRepository.findById(5L)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> accountServices.create(new AccountRequest(5L, AccountType.CURRENT)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("désactivé");

        verify(accountRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_forUnknownClient_throwsNotFound() {
        when(clientRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountServices.create(new AccountRequest(5L, AccountType.CURRENT)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changeStatus_blocksThenReactivatesAnAccount() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(newAccount()));

        assertThat(accountServices.changeStatus(1L, AccountStatus.BLOCKED).status())
                .isEqualTo(AccountStatus.BLOCKED);
        assertThat(accountServices.changeStatus(1L, AccountStatus.ACTIVE).status())
                .isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void changeStatus_closesAnEmptyAccount() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(newAccount()));

        assertThat(accountServices.changeStatus(1L, AccountStatus.CLOSED).status())
                .isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void changeStatus_refusesToCloseAnAccountThatStillHoldsMoney() {
        Account account = newAccount();
        account.setBalance(new BigDecimal("1000"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountServices.changeStatus(1L, AccountStatus.CLOSED))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("solde");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void changeStatus_aClosedAccountCanNeverBeReopened() {
        Account account = newAccount();
        account.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountServices.changeStatus(1L, AccountStatus.ACTIVE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("clôturé");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void changeStatus_withUnknownAccount_throwsNotFound() {
        when(accountRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountServices.changeStatus(9L, AccountStatus.BLOCKED))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}