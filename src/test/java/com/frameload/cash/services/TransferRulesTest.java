package com.frameload.cash.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.frameload.cash.dto.OperationResponse;
import com.frameload.cash.dto.TransferRequest;
import com.frameload.cash.entity.Account;
import com.frameload.cash.entity.Client;
import com.frameload.cash.entity.Transaction;
import com.frameload.cash.entity.Transfer;
import com.frameload.cash.enums.AccountStatus;
import com.frameload.cash.enums.AccountType;
import com.frameload.cash.enums.TransactionStatus;
import com.frameload.cash.enums.TransactionType;
import com.frameload.cash.exception.BusinessRuleException;
import com.frameload.cash.exception.ResourceNotFoundException;
import com.frameload.cash.repositories.AccountRepository;
import com.frameload.cash.repositories.TransactionRepository;
import com.frameload.cash.repositories.TransferRepository;
import com.frameload.cash.services.impl.TransferServicesImpl;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitaires des règles du virement (cahier des charges §11) : les repositories sont simulés (Mockito).
 */
@ExtendWith(MockitoExtension.class)
class TransferRulesTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransferServicesImpl transferServices;

    private Account accountWithBalance(String balance) {
        Client client = new Client("Awa", "Nkoulou", "awa@example.test", "+237 670 00 00 01", "Yaoundé");
        Account account = new Account(client, AccountType.CURRENT, "XAF");
        account.setBalance(new BigDecimal(balance));
        return account;
    }

    private TransferRequest request(long sourceId, long destinationId, String amount) {
        return new TransferRequest(sourceId, destinationId, new BigDecimal(amount), "Virement de démonstration");
    }

    @Test
    void transfer_debitsTheSourceCreditsTheDestinationAndSavesBothLegs() {
        Account source = accountWithBalance("100000");
        Account destination = accountWithBalance("20000");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(source));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(destination));

        OperationResponse response = transferServices.transfer(request(1, 2, "30000"));

        assertThat(source.getBalance()).isEqualByComparingTo("70000");
        assertThat(destination.getBalance()).isEqualByComparingTo("50000");
        assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.data().get("sourceNewBalance")).isEqualTo(source.getBalance());
        assertThat(response.data().get("destinationNewBalance")).isEqualTo(destination.getBalance());
        verify(transferRepository).saveAndFlush(any(Transfer.class));
        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).saveAndFlush(saved.capture());
        assertThat(saved.getAllValues()).extracting(Transaction::getType)
                .containsExactly(TransactionType.TRANSFER_OUT, TransactionType.TRANSFER_IN);
    }

    @Test
    void transfer_fromTheHighestIdToTheLowestId_locksTheLowestIdFirst() {
        Account lowest = accountWithBalance("10000");
        Account highest = accountWithBalance("80000");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(lowest));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(highest));

        transferServices.transfer(request(2, 1, "5000"));

        InOrder lockOrder = inOrder(accountRepository);
        lockOrder.verify(accountRepository).findByIdForUpdate(1L);
        lockOrder.verify(accountRepository).findByIdForUpdate(2L);
        assertThat(highest.getBalance()).isEqualByComparingTo("75000");
        assertThat(lowest.getBalance()).isEqualByComparingTo("15000");
    }

    @Test
    void transfer_toTheSameAccount_isRejectedBeforeAnyAccessToTheDatabase() {
        assertThatThrownBy(() -> transferServices.transfer(request(1, 1, "1000")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("différents");

        verify(accountRepository, never()).findByIdForUpdate(any());
        verify(transferRepository, never()).saveAndFlush(any());
    }

    @Test
    void transfer_withZeroOrNegativeAmount_isRejected() {
        assertThatThrownBy(() -> transferServices.transfer(request(1, 2, "0")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("supérieur à zéro");
        assertThatThrownBy(() -> transferServices.transfer(request(1, 2, "-5")))
                .isInstanceOf(BusinessRuleException.class);

        verify(transferRepository, never()).saveAndFlush(any());
    }

    @Test
    void transfer_withInsufficientBalance_changesNothing() {
        Account source = accountWithBalance("10000");
        Account destination = accountWithBalance("500");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(source));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(destination));

        assertThatThrownBy(() -> transferServices.transfer(request(1, 2, "10000.01")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("insuffisant");

        assertThat(source.getBalance()).isEqualByComparingTo("10000");
        assertThat(destination.getBalance()).isEqualByComparingTo("500");
        verify(transferRepository, never()).saveAndFlush(any());
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void transfer_fromAnInactiveSource_isRejectedAndNothingChanges() {
        Account source = accountWithBalance("10000");
        source.setStatus(AccountStatus.BLOCKED);
        Account destination = accountWithBalance("0");
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(source));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(destination));

        assertThatThrownBy(() -> transferServices.transfer(request(1, 2, "100")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("source");

        assertThat(source.getBalance()).isEqualByComparingTo("10000");
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void transfer_toAnInactiveDestination_leavesTheSourceUntouched() {
        Account source = accountWithBalance("10000");
        Account destination = accountWithBalance("0");
        destination.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(source));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(destination));

        assertThatThrownBy(() -> transferServices.transfer(request(1, 2, "100")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("destinataire");

        assertThat(source.getBalance()).isEqualByComparingTo("10000");
        assertThat(destination.getBalance()).isEqualByComparingTo("0");
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void transfer_withAnUnknownAccount_throwsNotFound() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferServices.transfer(request(1, 2, "100")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transferRepository, never()).saveAndFlush(any());
    }
}