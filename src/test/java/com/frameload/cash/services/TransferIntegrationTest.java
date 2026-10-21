package com.frameload.cash.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.frameload.cash.dto.AccountRequest;
import com.frameload.cash.dto.AccountResponse;
import com.frameload.cash.dto.ClientRequest;
import com.frameload.cash.dto.OperationRequest;
import com.frameload.cash.dto.OperationResponse;
import com.frameload.cash.dto.TransferRequest;
import com.frameload.cash.dto.TransferResponse;
import com.frameload.cash.entity.Transaction;
import com.frameload.cash.enums.AccountStatus;
import com.frameload.cash.enums.AccountType;
import com.frameload.cash.enums.TransactionType;
import com.frameload.cash.exception.BusinessRuleException;
import com.frameload.cash.exception.ResourceNotFoundException;
import com.frameload.cash.repositories.TransactionRepository;
import com.frameload.cash.repositories.TransferRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Un test de virement ne se contente pas du message retourné (cahier des charges §22.3) :
 * il vérifie les deux soldes et les écritures enregistrées, y compris après un refus.
 */
@SpringBootTest
@ActiveProfiles("test")
class TransferIntegrationTest {

    @Autowired
    private TransferServices transferServices;

    @Autowired
    private AccountOperationServices operationServices;

    @Autowired
    private AccountServices accountServices;

    @Autowired
    private ClientServices clientServices;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransferRepository transferRepository;

    private AccountResponse fundedAccount(String amount) {
        Long clientId = clientServices.create(new ClientRequest("Awa", "Nkoulou",
                "awa-" + UUID.randomUUID() + "@example.test", "+237 670 00 00 01", "Yaoundé")).id();
        AccountResponse account = accountServices.create(new AccountRequest(clientId, AccountType.CURRENT));
        operationServices.deposit(account.id(), new OperationRequest(new BigDecimal(amount), "Provision"));
        return account;
    }

    private TransferRequest request(AccountResponse source, AccountResponse destination, String amount) {
        return new TransferRequest(source.id(), destination.id(), new BigDecimal(amount), "Virement de démonstration");
    }

    private BigDecimal balanceOf(AccountResponse account) {
        return accountServices.findById(account.id()).balance();
    }

    @Test
    void transfer_debitsTheSourceCreditsTheDestinationAndRecordsBothLegs() {
        AccountResponse source = fundedAccount("100000");
        AccountResponse destination = fundedAccount("20000");

        OperationResponse response = transferServices.transfer(request(source, destination, "30000"));

        assertThat(balanceOf(source)).isEqualByComparingTo("70000");
        assertThat(balanceOf(destination)).isEqualByComparingTo("50000");
        assertThat(balanceOf(source).add(balanceOf(destination))).isEqualByComparingTo("120000");
        assertThat(response.reference()).matches("TR-\\d{6}");

        List<Transaction> legs = transactionRepository.findByTransferReference(response.reference());
        assertThat(legs).hasSize(2);
        assertThat(legs).extracting(Transaction::getType)
                .containsExactlyInAnyOrder(TransactionType.TRANSFER_OUT, TransactionType.TRANSFER_IN);
        assertThat(legs).allSatisfy(leg -> assertThat(leg.getAmount()).isEqualByComparingTo("30000"));

        assertThat(transactionRepository.findByAccountIdOrderByIdDesc(source.id()).get(0).getType())
                .isEqualTo(TransactionType.TRANSFER_OUT);
        assertThat(transactionRepository.findByAccountIdOrderByIdDesc(destination.id()).get(0).getType())
                .isEqualTo(TransactionType.TRANSFER_IN);
    }

    @Test
    void transfer_fromTheMostRecentAccountToTheOldestOne_works() {
        AccountResponse oldest = fundedAccount("10000");
        AccountResponse newest = fundedAccount("80000");

        transferServices.transfer(request(newest, oldest, "5000"));

        assertThat(balanceOf(newest)).isEqualByComparingTo("75000");
        assertThat(balanceOf(oldest)).isEqualByComparingTo("15000");
    }

    @Test
    void transfer_toTheSameAccount_isRejectedWithoutAnyWrite() {
        AccountResponse account = fundedAccount("10000");
        long transfersBefore = transferRepository.count();

        assertThatThrownBy(() -> transferServices.transfer(request(account, account, "1000")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("différents");

        assertThat(balanceOf(account)).isEqualByComparingTo("10000");
        assertThat(transactionRepository.countByAccountId(account.id())).isEqualTo(1);
        assertThat(transferRepository.count()).isEqualTo(transfersBefore);
    }

    @Test
    void transfer_withInsufficientBalance_changesNothing() {
        AccountResponse source = fundedAccount("10000");
        AccountResponse destination = fundedAccount("500");
        long transfersBefore = transferRepository.count();

        assertThatThrownBy(() -> transferServices.transfer(request(source, destination, "10000.01")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("insuffisant");

        assertThat(balanceOf(source)).isEqualByComparingTo("10000");
        assertThat(balanceOf(destination)).isEqualByComparingTo("500");
        assertThat(transferRepository.count()).isEqualTo(transfersBefore);
    }

    @Test
    void transfer_toABlockedAccount_leavesTheSourceUntouched() {
        AccountResponse source = fundedAccount("10000");
        AccountResponse destination = fundedAccount("0.01");
        accountServices.changeStatus(destination.id(), AccountStatus.BLOCKED);

        assertThatThrownBy(() -> transferServices.transfer(request(source, destination, "1000")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("destinataire");

        assertThat(balanceOf(source)).isEqualByComparingTo("10000");
        assertThat(transactionRepository.countByAccountId(source.id())).isEqualTo(1);
    }

    @Test
    void transfer_toAnUnknownAccount_throwsNotFoundAndLeavesTheSourceUntouched() {
        AccountResponse source = fundedAccount("10000");

        assertThatThrownBy(() -> transferServices.transfer(
                new TransferRequest(source.id(), 999_999L, new BigDecimal("1000"), null)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(balanceOf(source)).isEqualByComparingTo("10000");
    }

    @Test
    void findByReference_returnsTheTransferOrThrowsNotFound() {
        AccountResponse source = fundedAccount("100000");
        AccountResponse destination = fundedAccount("0.01");
        OperationResponse response = transferServices.transfer(request(source, destination, "30000"));

        TransferResponse found = transferServices.findByReference(response.reference());

        assertThat(found.sourceAccountId()).isEqualTo(source.id());
        assertThat(found.destinationAccountId()).isEqualTo(destination.id());
        assertThat(found.amount()).isEqualByComparingTo("30000");
        assertThatThrownBy(() -> transferServices.findByReference("TR-999999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}