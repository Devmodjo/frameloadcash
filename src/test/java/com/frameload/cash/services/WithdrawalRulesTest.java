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
 * Tests unitaires des règles du retrait (cahier des charges §10) : les repositories sont simulés (Mockito).
 */
@ExtendWith(MockitoExtension.class)
class WithdrawalRulesTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountOperationServicesImpl operationServices;

    private Account accountWithBalance(String balance) {
        Client client = new Client("Awa", "Nkoulou", "awa@example.test", "+237 670 00 00 01", "Yaoundé");
        Account account = new Account(client, AccountType.CURRENT, "XAF");
        account.setBalance(new BigDecimal(balance));
        return account;
    }

    private OperationRequest amount(String value) {
        return new OperationRequest(new BigDecimal(value), "Retrait de démonstration");
    }

    @Test
    void withdraw_decreasesTheBalanceAndSavesOneWithdrawalTransaction() {
        Account account = accountWithBalance("50000");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        OperationResponse response = operationServices.withdraw(1L, amount("10000"));

        assertThat(account.getBalance()).isEqualByComparingTo("40000");
        assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.data().get("newBalance")).isEqualTo(account.getBalance());
        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("10000");
    }

    @Test
    void withdraw_ofTheWholeBalance_isAllowed() {
        Account account = accountWithBalance("20000");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        operationServices.withdraw(1L, amount("20000"));

        assertThat(account.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void withdraw_moreThanTheBalance_isRejectedAndNothingChanges() {
        Account account = accountWithBalance("20000");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> operationServices.withdraw(1L, amount("20000.01")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("insuffisant");

        assertThat(account.getBalance()).isEqualByComparingTo("20000");
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void withdraw_withZeroOrNegativeAmount_isRejectedAndNothingIsSaved() {
        assertThatThrownBy(() -> operationServices.withdraw(1L, amount("0")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("supérieur à zéro");
        assertThatThrownBy(() -> operationServices.withdraw(1L, amount("-500")))
                .isInstanceOf(BusinessRuleException.class);

        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void withdraw_onABlockedOrClosedAccount_isRejectedAndLeavesTheBalanceUnchanged() {
        Account blocked = accountWithBalance("5000");
        blocked.setStatus(AccountStatus.BLOCKED);
        Account closed = accountWithBalance("0");
        closed.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(blocked));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> operationServices.withdraw(1L, amount("100")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pas actif");
        assertThatThrownBy(() -> operationServices.withdraw(2L, amount("100")))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(blocked.getBalance()).isEqualByComparingTo("5000");
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void withdraw_onAnUnknownAccount_throwsNotFound() {
        when(accountRepository.findByIdForUpdate(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operationServices.withdraw(9L, amount("100")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}