package com.frameload.cash.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.frameload.cash.dto.OperationRequest;
import com.frameload.cash.dto.OperationResponse;
import com.frameload.cash.entity.Account;
import com.frameload.cash.entity.Client;
import com.frameload.cash.entity.Transaction;
import com.frameload.cash.enums.AccountStatus;
import com.frameload.cash.enums.AccountType;
import com.frameload.cash.enums.TransactionStatus;
import com.frameload.cash.enums.TransactionType;
import com.frameload.cash.exception.BusinessRuleException;
import com.frameload.cash.exception.ResourceNotFoundException;
import com.frameload.cash.repositories.AccountRepository;
import com.frameload.cash.repositories.TransactionRepository;
import com.frameload.cash.services.impl.AccountOperationServicesImpl;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitaires des règles du dépôt : les repositories sont simulés (Mockito).
 */
@ExtendWith(MockitoExtension.class)
class AccountOperationServicesImplRulesTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountOperationServicesImpl operationServices;

    private Account newAccount() {
        Client client = new Client("Awa", "Nkoulou", "awa@example.test", "+237 670 00 00 01", "Yaoundé");
        return new Account(client, AccountType.CURRENT, "XAF");
    }

    private OperationRequest amount(String value) {
        return new OperationRequest(new BigDecimal(value), "Dépôt de démonstration");
    }

    @Test
    void deposit_increasesTheBalanceAndSavesOneDepositTransaction() {
        Account account = newAccount();
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        OperationResponse response = operationServices.deposit(1L, amount("25000"));

        assertThat(account.getBalance()).isEqualByComparingTo("25000");
        assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.data().get("newBalance")).isEqualTo(account.getBalance());
        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("25000");
    }

    @Test
    void deposit_withZeroOrNegativeAmount_isRejectedAndNothingIsSaved() {
        assertThatThrownBy(() -> operationServices.deposit(1L, amount("0")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("supérieur à zéro");
        assertThatThrownBy(() -> operationServices.deposit(1L, amount("-500")))
                .isInstanceOf(BusinessRuleException.class);

        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void deposit_onABlockedAccount_isRejectedAndLeavesTheBalanceUnchanged() {
        Account account = newAccount();
        account.setStatus(AccountStatus.BLOCKED);
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> operationServices.deposit(1L, amount("1000")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pas actif");

        assertThat(account.getBalance()).isEqualByComparingTo("0");
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void deposit_onAClosedAccount_isRejected() {
        Account account = newAccount();
        account.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> operationServices.deposit(1L, amount("1000")))
                .isInstanceOf(BusinessRuleException.class);

        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void deposit_onAnUnknownAccount_throwsNotFound() {
        when(accountRepository.findByIdForUpdate(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operationServices.deposit(9L, amount("1000")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}