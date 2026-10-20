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
import com.frameload.cash.enums.TransactionStatus;
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
 * Teste les dépôts avec la vraie persistance (H2) : solde ET transaction sont vérifiés en base.
 */
@SpringBootTest
@ActiveProfiles("test")
class AccountOperationServicesImplTest {

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

    private OperationRequest amount(String value) {
        return new OperationRequest(new BigDecimal(value), "Dépôt de démonstration");
    }

    @Test
    void deposit_increasesTheBalanceAndRecordsACompletedTransaction() {
        AccountResponse account = newAccount();

        OperationResponse response = operationServices.deposit(account.id(), amount("25000"));

        assertThat(accountServices.findById(account.id()).balance()).isEqualByComparingTo("25000");
        assertThat(response.reference()).matches("TX-\\d{6}");
        assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);

        List<Transaction> history = transactionRepository.findByAccountIdOrderByIdDesc(account.id());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(history.get(0).getAmount()).isEqualByComparingTo("25000");
        assertThat(history.get(0).getReference()).isEqualTo(response.reference());
        assertThat(history.get(0).getCreatedAt()).isNotNull();
    }

    @Test
    void deposit_twice_accumulatesTheBalanceWithDistinctReferences() {
        AccountResponse account = newAccount();

        OperationResponse first = operationServices.deposit(account.id(), amount("1000"));
        OperationResponse second = operationServices.deposit(account.id(), amount("2500.50"));

        assertThat(accountServices.findById(account.id()).balance()).isEqualByComparingTo("3500.50");
        assertThat(first.reference()).isNotEqualTo(second.reference());
    }

    @Test
    void deposit_onABlockedAccount_changesNothing() {
        AccountResponse account = newAccount();
        accountServices.changeStatus(account.id(), AccountStatus.BLOCKED);

        assertThatThrownBy(() -> operationServices.deposit(account.id(), amount("1000")))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(accountServices.findById(account.id()).balance()).isEqualByComparingTo("0");
        assertThat(transactionRepository.countByAccountId(account.id())).isZero();
    }
}