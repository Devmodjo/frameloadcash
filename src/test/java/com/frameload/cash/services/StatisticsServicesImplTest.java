package com.frameload.cash.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.frameload.cash.dto.AccountRequest;
import com.frameload.cash.dto.AccountResponse;
import com.frameload.cash.dto.CardRequest;
import com.frameload.cash.dto.CardResponse;
import com.frameload.cash.dto.ClientRequest;
import com.frameload.cash.dto.DashboardResponse;
import com.frameload.cash.dto.OperationRequest;
import com.frameload.cash.dto.StatisticsResponse;
import com.frameload.cash.dto.TransferRequest;
import com.frameload.cash.enums.AccountStatus;
import com.frameload.cash.enums.AccountType;
import com.frameload.cash.enums.CardStatus;
import com.frameload.cash.enums.CardType;
import com.frameload.cash.enums.TransactionType;
import com.frameload.cash.exception.InvalidRequestException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Teste le tableau de bord et les statistiques avec la vraie persistance (H2) : chaque valeur est vérifiée par
 * différence entre un état « avant » et un état « après » des opérations connues.
 */
@SpringBootTest
@ActiveProfiles("test")
class StatisticsServicesImplTest {

    @Autowired
    private StatisticsServices statisticsServices;

    @Autowired
    private ClientServices clientServices;

    @Autowired
    private AccountServices accountServices;

    @Autowired
    private AccountOperationServices operationServices;

    @Autowired
    private TransferServices transferServices;

    @Autowired
    private CardServices cardServices;

    private AccountResponse newAccount() {
        Long clientId = clientServices.create(new ClientRequest("Awa", "Nkoulou",
                "awa-" + UUID.randomUUID() + "@example.test", "+237 670 00 00 01", "Yaoundé")).id();
        return accountServices.create(new AccountRequest(clientId, AccountType.CURRENT));
    }

    private OperationRequest amount(String value) {
        return new OperationRequest(new BigDecimal(value), "Opération de démonstration");
    }

    private long countOf(StatisticsResponse statistics, TransactionType type) {
        return statistics.transactionsByType().stream()
                .filter(row -> row.type() == type).findFirst().orElseThrow().count();
    }

    @Test
    void dashboard_countsTheNewData() {
        DashboardResponse before = statisticsServices.dashboard();
        AccountResponse source = newAccount();
        AccountResponse destination = newAccount();
        cardServices.create(source.id(), new CardRequest(CardType.DEBIT));
        operationServices.deposit(source.id(), amount("100000"));
        operationServices.withdraw(source.id(), amount("20000"));
        transferServices.transfer(new TransferRequest(source.id(), destination.id(), new BigDecimal("30000"), "Virement"));
        accountServices.changeStatus(destination.id(), AccountStatus.BLOCKED);

        DashboardResponse after = statisticsServices.dashboard();

        assertThat(after.totalClients() - before.totalClients()).isEqualTo(2);
        assertThat(after.totalAccounts() - before.totalAccounts()).isEqualTo(2);
        assertThat(after.activeAccounts() - before.activeAccounts()).isEqualTo(1);
        assertThat(after.activeCards() - before.activeCards()).isEqualTo(1);
        assertThat(after.completedTransactions() - before.completedTransactions()).isEqualTo(4);
        assertThat(after.totalDepositsVolume().subtract(before.totalDepositsVolume())).isEqualByComparingTo("100000");
        assertThat(after.totalWithdrawalsVolume().subtract(before.totalWithdrawalsVolume())).isEqualByComparingTo("20000");
        assertThat(after.completedTransfers() - before.completedTransfers()).isEqualTo(1);
    }

    @Test
    void dashboard_doesNotCountABlockedCardAsActive() {
        DashboardResponse before = statisticsServices.dashboard();
        CardResponse card = cardServices.create(newAccount().id(), new CardRequest(CardType.PREPAID));

        assertThat(statisticsServices.dashboard().activeCards() - before.activeCards()).isEqualTo(1);

        cardServices.changeStatus(card.id(), CardStatus.BLOCKED);

        assertThat(statisticsServices.dashboard().activeCards()).isEqualTo(before.activeCards());
    }

    @Test
    void statistics_sumAndCountTheOperationsOfThePeriod() {
        LocalDate today = LocalDate.now();
        StatisticsResponse before = statisticsServices.statistics(today, today);
        AccountResponse source = newAccount();
        AccountResponse destination = newAccount();
        operationServices.deposit(source.id(), amount("100000"));
        operationServices.deposit(source.id(), amount("5000.50"));
        operationServices.withdraw(source.id(), amount("20000"));
        transferServices.transfer(new TransferRequest(source.id(), destination.id(), new BigDecimal("30000"), "Virement"));

        StatisticsResponse after = statisticsServices.statistics(today, today);

        assertThat(after.totalDeposits().subtract(before.totalDeposits())).isEqualByComparingTo("105000.50");
        assertThat(after.totalWithdrawals().subtract(before.totalWithdrawals())).isEqualByComparingTo("20000");
        assertThat(after.completedTransfers() - before.completedTransfers()).isEqualTo(1);
        assertThat(countOf(after, TransactionType.DEPOSIT) - countOf(before, TransactionType.DEPOSIT)).isEqualTo(2);
        assertThat(countOf(after, TransactionType.WITHDRAWAL) - countOf(before, TransactionType.WITHDRAWAL)).isEqualTo(1);
        assertThat(countOf(after, TransactionType.TRANSFER_OUT) - countOf(before, TransactionType.TRANSFER_OUT)).isEqualTo(1);
        assertThat(countOf(after, TransactionType.TRANSFER_IN) - countOf(before, TransactionType.TRANSFER_IN)).isEqualTo(1);
    }

    @Test
    void statistics_periodBoundsAreInclusive() {
        LocalDate today = LocalDate.now();
        StatisticsResponse before = statisticsServices.statistics(today, today);
        operationServices.deposit(newAccount().id(), amount("7000"));

        StatisticsResponse after = statisticsServices.statistics(today, today);

        assertThat(after.totalDeposits().subtract(before.totalDeposits())).isEqualByComparingTo("7000");
        assertThat(statisticsServices.statistics(today.plusDays(1), today.plusDays(1)).totalDeposits())
                .isEqualByComparingTo("0");
        assertThat(statisticsServices.statistics(today.minusDays(5), today.minusDays(1)).totalDeposits())
                .isEqualByComparingTo("0");
        assertThat(statisticsServices.statistics(today, null).totalDeposits())
                .isGreaterThanOrEqualTo(after.totalDeposits());
    }

    @Test
    void statistics_forAPeriodWithoutOperations_isEmpty() {
        newAccount();

        StatisticsResponse statistics = statisticsServices.statistics(LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 31));

        assertThat(statistics.totalDeposits()).isEqualByComparingTo("0");
        assertThat(statistics.totalWithdrawals()).isEqualByComparingTo("0");
        assertThat(statistics.completedTransfers()).isZero();
        assertThat(statistics.transactionsByType()).hasSize(4).allSatisfy(row -> assertThat(row.count()).isZero());
        assertThat(statistics.mostActiveAccounts()).isEmpty();
    }

    @Test
    void statistics_ranksTheMostActiveAccountsFirst() {
        AccountResponse busy = newAccount();
        for (int i = 0; i < 12; i++) {
            operationServices.deposit(busy.id(), amount("100"));
        }

        StatisticsResponse statistics = statisticsServices.statistics(null, null);

        assertThat(statistics.mostActiveAccounts()).hasSizeLessThanOrEqualTo(5);
        assertThat(statistics.mostActiveAccounts().get(0).accountId()).isEqualTo(busy.id());
        assertThat(statistics.mostActiveAccounts().get(0).operations()).isEqualTo(12);
    }

    @Test
    void statistics_listsTheCurrentBalances() {
        AccountResponse account = newAccount();
        operationServices.deposit(account.id(), amount("42000"));

        StatisticsResponse statistics = statisticsServices.statistics(null, null);

        assertThat(statistics.accountBalances())
                .filteredOn(balance -> balance.accountNumber().equals(account.accountNumber()))
                .singleElement()
                .satisfies(balance -> assertThat(balance.balance()).isEqualByComparingTo("42000"));
    }

    @Test
    void statistics_withAnInvertedPeriod_isRejected() {
        assertThatThrownBy(() -> statisticsServices.statistics(LocalDate.now(), LocalDate.now().minusDays(1)))
                .isInstanceOf(InvalidRequestException.class);
    }
}