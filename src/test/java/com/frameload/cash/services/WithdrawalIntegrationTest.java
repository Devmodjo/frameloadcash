package com.frameload.cash.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.frameload.cash.dto.AccountRequest;
import com.frameload.cash.dto.AccountResponse;
import com.frameload.cash.dto.ClientRequest;
import com.frameload.cash.dto.OperationRequest;
import com.frameload.cash.dto.OperationResponse;
import com.frameload.cash.entity.Transaction;
import com.frameload.cash.enums.AccountStatus;
import com.frameload.cash.enums.AccountType;
import com.frameload.cash.enums.TransactionType;
import com.frameload.cash.exception.BusinessRuleException;
import com.frameload.cash.repositories.TransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Teste les retraits avec la vraie persistance (H2) : solde ET transactions sont vérifiés en base.
 */
@SpringBootTest
@ActiveProfiles("test")
class WithdrawalIntegrationTest {

    @Autowired
    private AccountOperationServices operationServices;

    @Autowired
    private AccountServices accountServices;

    @Autowired
    private ClientServices clientServices;

    @Autowired
    private TransactionRepository transactionRepository;

    private AccountResponse newAccount() {
        Long clientId = clientServices.create(new ClientRequest("Awa", "Nkoulou",
                "awa-" + UUID.randomUUID() + "@example.test", "+237 670 00 00 01", "Yaoundé")).id();
        return accountServices.create(new AccountRequest(clientId, AccountType.CURRENT));
    }

    private AccountResponse fundedAccount(String amount) {
        AccountResponse account = newAccount();
        operationServices.deposit(account.id(), new OperationRequest(new BigDecimal(amount), "Provision"));
        return account;
    }

    private OperationRequest amount(String value) {
        return new OperationRequest(new BigDecimal(value), "Retrait de démonstration");
    }

    @Test
    void withdraw_decreasesTheBalanceAndRecordsAWithdrawalTransaction() {
        AccountResponse account = fundedAccount("50000");

        OperationResponse response = operationServices.withdraw(account.id(), amount("10000"));

        assertThat(accountServices.findById(account.id()).balance()).isEqualByComparingTo("40000");
        assertThat(response.reference()).matches("TX-\\d{6}");
        List<Transaction> history = transactionRepository.findByAccountIdOrderByIdDesc(account.id());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(history.get(0).getAmount()).isEqualByComparingTo("10000");
        assertThat(history.get(0).getReference()).isEqualTo(response.reference());
    }

    @Test
    void withdraw_theWholeBalance_isAllowed() {
        AccountResponse account = fundedAccount("20000");

        operationServices.withdraw(account.id(), amount("20000"));

        assertThat(accountServices.findById(account.id()).balance()).isEqualByComparingTo("0");
    }

    @Test
    void withdraw_moreThanTheBalance_changesNothing() {
        AccountResponse account = fundedAccount("20000");

        assertThatThrownBy(() -> operationServices.withdraw(account.id(), amount("20000.01")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("insuffisant");

        assertThat(accountServices.findById(account.id()).balance()).isEqualByComparingTo("20000");
        assertThat(transactionRepository.countByAccountId(account.id())).isEqualTo(1);
    }

    @Test
    void withdraw_onABlockedAccount_changesNothing() {
        AccountResponse account = fundedAccount("5000");
        accountServices.changeStatus(account.id(), AccountStatus.BLOCKED);

        assertThatThrownBy(() -> operationServices.withdraw(account.id(), amount("100")))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(accountServices.findById(account.id()).balance()).isEqualByComparingTo("5000");
        assertThat(transactionRepository.countByAccountId(account.id())).isEqualTo(1);
    }

    @Test
    void withdraw_onAClosedAccount_isRejected() {
        AccountResponse account = newAccount();
        accountServices.changeStatus(account.id(), AccountStatus.CLOSED);

        assertThatThrownBy(() -> operationServices.withdraw(account.id(), amount("1")))
                .isInstanceOf(BusinessRuleException.class);
    }
}