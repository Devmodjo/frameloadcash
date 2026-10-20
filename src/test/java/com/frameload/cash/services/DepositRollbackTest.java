package com.frameload.cash.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.frameload.cash.dto.AccountRequest;
import com.frameload.cash.dto.AccountResponse;
import com.frameload.cash.dto.ClientRequest;
import com.frameload.cash.dto.OperationRequest;
import com.frameload.cash.enums.AccountType;
import com.frameload.cash.repositories.TransactionRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Cahier des charges §9.2 : en cas d'échec d'enregistrement de la transaction, le solde ne doit pas rester modifié.
 * On remplace le repository des transactions par un faux qui échoue, puis on relit le solde en base.
 */
@SpringBootTest
@ActiveProfiles("test")
class DepositRollbackTest {

    @Autowired
    private AccountOperationServices operationServices;

    @Autowired
    private AccountServices accountServices;

    @Autowired
    private ClientServices clientServices;

    @MockitoBean
    private TransactionRepository transactionRepository;

    @Test
    void deposit_whenTheTransactionCannotBeSaved_leavesTheBalanceUnchanged() {
        Long clientId = clientServices.create(new ClientRequest("Awa", "Nkoulou",
                "awa-" + UUID.randomUUID() + "@example.test", "+237 670 00 00 01", "Yaoundé")).id();
        AccountResponse account = accountServices.create(new AccountRequest(clientId, AccountType.CURRENT));
        when(transactionRepository.saveAndFlush(any())).thenThrow(new IllegalStateException("Panne simulée"));

        assertThatThrownBy(() -> operationServices.deposit(account.id(),
                new OperationRequest(new BigDecimal("5000"), "Dépôt avec panne")))
                .isInstanceOf(IllegalStateException.class);

        assertThat(accountServices.findById(account.id()).balance()).isEqualByComparingTo("0");
    }
}