package com.frameload.cash.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.frameload.cash.dto.AccountRequest;
import com.frameload.cash.dto.AccountResponse;
import com.frameload.cash.dto.ClientRequest;
import com.frameload.cash.dto.OperationRequest;
import com.frameload.cash.dto.OperationResponse;
import com.frameload.cash.dto.TransactionResponse;
import com.frameload.cash.dto.TransferRequest;
import com.frameload.cash.enums.AccountStatus;
import com.frameload.cash.enums.AccountType;
import com.frameload.cash.enums.ClientStatus;
import com.frameload.cash.enums.TransactionType;
import com.frameload.cash.exception.InvalidRequestException;
import com.frameload.cash.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Teste l'historique des transactions avec la vraie persistance (H2) : filtres, tri, bornes de période
 * et cohérence des historiques d'un virement.
 */
@SpringBootTest
@ActiveProfiles("test")
class TransactionServicesImplTest {

    @Autowired
    private TransactionServices transactionServices;

    @Autowired
    private AccountOperationServices operationServices;

    @Autowired
    private TransferServices transferServices;

    @Autowired
    private AccountServices accountServices;

    @Autowired
    private ClientServices clientServices;

    private AccountResponse newAccount() {
        Long clientId = clientServices.create(new ClientRequest("Awa", "Nkoulou",
                "awa-" + UUID.randomUUID() + "@example.test", "+237 670 00 00 01", "Yaoundé")).id();
        return accountServices.create(new AccountRequest(clientId, AccountType.CURRENT));
    }

    private AccountResponse fundedAccount(String amount) {
        AccountResponse account = newAccount();
        operationServices.deposit(account.id(), amount(amount));
        return account;
    }

    private OperationRequest amount(String value) {
        return new OperationRequest(new BigDecimal(value), "Opération de démonstration");
    }

    @Test
    void findAll_forAnAccount_returnsTheMostRecentFirst() {
        AccountResponse account = fundedAccount("50000");
        operationServices.withdraw(account.id(), amount("1000"));

        List<TransactionResponse> history = transactionServices.findAll(account.id(), null, null, null);

        assertThat(history).extracting(TransactionResponse::type)
                .containsExactly(TransactionType.WITHDRAWAL, TransactionType.DEPOSIT);
    }

    @Test
    void findAll_withoutFilter_containsTheTransaction() {
        AccountResponse account = fundedAccount("1000");

        assertThat(transactionServices.findAll(null, null, null, null))
                .extracting(TransactionResponse::accountId).contains(account.id());
    }

    @Test
    void findAll_returnsOnlyTheTransactionsOfTheRequestedAccount() {
        AccountResponse first = fundedAccount("1000");
        fundedAccount("2000");

        assertThat(transactionServices.findAll(first.id(), null, null, null))
                .hasSize(1)
                .allSatisfy(transaction -> assertThat(transaction.accountId()).isEqualTo(first.id()));
    }

    @Test
    void findAll_filtersByType() {
        AccountResponse account = fundedAccount("50000");
        operationServices.withdraw(account.id(), amount("1000"));

        List<TransactionResponse> deposits =
                transactionServices.findAll(account.id(), TransactionType.DEPOSIT, null, null);

        assertThat(deposits).hasSize(1);
        assertThat(deposits.get(0).type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(transactionServices.findAll(account.id(), TransactionType.TRANSFER_IN, null, null)).isEmpty();
    }

    @Test
    void findAll_periodBoundsAreInclusive() {
        AccountResponse account = fundedAccount("1000");
        LocalDate today = LocalDate.now();

        assertThat(transactionServices.findAll(account.id(), null, today, today)).hasSize(1);
        assertThat(transactionServices.findAll(account.id(), null, today, null)).hasSize(1);
        assertThat(transactionServices.findAll(account.id(), null, null, today)).hasSize(1);
        assertThat(transactionServices.findAll(account.id(), null, today.minusDays(3), today.minusDays(1))).isEmpty();
        assertThat(transactionServices.findAll(account.id(), null, today.plusDays(1), null)).isEmpty();
    }

    @Test
    void findAll_combinesTypeAndPeriod() {
        AccountResponse account = fundedAccount("50000");
        operationServices.withdraw(account.id(), amount("1000"));
        LocalDate today = LocalDate.now();

        List<TransactionResponse> result =
                transactionServices.findAll(account.id(), TransactionType.WITHDRAWAL, today, today);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo(TransactionType.WITHDRAWAL);
    }

    @Test
    void findAll_withAnInvertedPeriod_isRejected() {
        assertThatThrownBy(() -> transactionServices.findAll(null, null, LocalDate.now(), LocalDate.now().minusDays(1)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void findAll_forAnUnknownAccount_throwsNotFound() {
        assertThatThrownBy(() -> transactionServices.findAll(999_999L, null, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByReference_returnsTheTransactionOrThrowsNotFound() {
        AccountResponse account = newAccount();
        OperationResponse deposit = operationServices.deposit(account.id(), amount("1000"));

        TransactionResponse found = transactionServices.findByReference(deposit.reference());

        assertThat(found.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(found.accountId()).isEqualTo(account.id());
        assertThat(found.amount()).isEqualByComparingTo("1000");
        assertThatThrownBy(() -> transactionServices.findByReference("TX-999999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void aTransfer_appearsConsistentlyInTheHistoryOfBothAccounts() {
        AccountResponse source = fundedAccount("100000");
        AccountResponse destination = newAccount();

        OperationResponse transfer = transferServices.transfer(
                new TransferRequest(source.id(), destination.id(), new BigDecimal("30000"), "Virement"));

        TransactionResponse out = transactionServices.findAll(source.id(), null, null, null).get(0);
        TransactionResponse in = transactionServices.findAll(destination.id(), null, null, null).get(0);
        assertThat(out.type()).isEqualTo(TransactionType.TRANSFER_OUT);
        assertThat(in.type()).isEqualTo(TransactionType.TRANSFER_IN);
        assertThat(out.transferReference()).isEqualTo(transfer.reference());
        assertThat(in.transferReference()).isEqualTo(transfer.reference());
        assertThat(out.amount()).isEqualByComparingTo(in.amount());
    }

    @Test
    void theHistoryIsKeptWhenTheAccountIsBlockedAndTheClientDeactivated() {
        AccountResponse account = fundedAccount("1000");

        accountServices.changeStatus(account.id(), AccountStatus.BLOCKED);
        clientServices.changeStatus(account.clientId(), ClientStatus.INACTIVE);

        assertThat(transactionServices.findAll(account.id(), null, null, null)).hasSize(1);
    }

    @Test
    void theHistoryIsKeptWhenTheAccountIsClosed() {
        AccountResponse account = fundedAccount("1000");
        operationServices.withdraw(account.id(), amount("1000"));

        accountServices.changeStatus(account.id(), AccountStatus.CLOSED);

        assertThat(transactionServices.findAll(account.id(), null, null, null)).hasSize(2);
    }
}