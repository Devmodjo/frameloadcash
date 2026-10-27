package com.frameload.cash.services.impl;

import com.frameload.cash.dto.AccountActivity;
import com.frameload.cash.dto.AccountBalance;
import com.frameload.cash.dto.DashboardResponse;
import com.frameload.cash.dto.StatisticsResponse;
import com.frameload.cash.dto.TransactionTypeCount;
import com.frameload.cash.enums.AccountStatus;
import com.frameload.cash.enums.CardStatus;
import com.frameload.cash.enums.TransactionStatus;
import com.frameload.cash.enums.TransactionType;
import com.frameload.cash.exception.InvalidRequestException;
import com.frameload.cash.repositories.AccountRepository;
import com.frameload.cash.repositories.CardRepository;
import com.frameload.cash.repositories.ClientRepository;
import com.frameload.cash.repositories.TransactionRepository;
import com.frameload.cash.repositories.TransferRepository;
import com.frameload.cash.services.StatisticsServices;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tableau de bord et statistiques (cahier des charges, sections 14 et 15). Tout est calculé à partir des
 * données persistées, et seules les transactions COMPLETED sont comptées. Les bornes de période sont INCLUSIVES :
 * {@code startDate} commence à 00:00:00 et {@code endDate} couvre toute la journée.
 */
@Service
public class StatisticsServicesImpl implements StatisticsServices {

    /**
     * Début de la période quand aucune date de début n'est donnée.
     */
    private static final LocalDateTime FIRST_INSTANT = LocalDateTime.of(1970, 1, 1, 0, 0);

    /**
     * Fin (exclue) de la période quand aucune date de fin n'est donnée.
     */
    private static final LocalDateTime LAST_INSTANT = LocalDateTime.of(9999, 1, 1, 0, 0);

    private static final int TOP_ACCOUNTS = 5;

    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;

    public StatisticsServicesImpl(ClientRepository clientRepository, AccountRepository accountRepository,
                                  CardRepository cardRepository, TransactionRepository transactionRepository,
                                  TransferRepository transferRepository) {
        this.clientRepository = clientRepository;
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.transferRepository = transferRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        BigDecimal deposits = Objects.requireNonNullElse(transactionRepository.sumAmount(
                TransactionType.DEPOSIT, TransactionStatus.COMPLETED, FIRST_INSTANT, LAST_INSTANT), BigDecimal.ZERO);
        BigDecimal withdrawals = Objects.requireNonNullElse(transactionRepository.sumAmount(
                TransactionType.WITHDRAWAL, TransactionStatus.COMPLETED, FIRST_INSTANT, LAST_INSTANT), BigDecimal.ZERO);

        return new DashboardResponse(
                clientRepository.count(),
                accountRepository.count(),
                accountRepository.countByStatus(AccountStatus.ACTIVE),
                cardRepository.countByStatus(CardStatus.ACTIVE),
                transactionRepository.countByStatus(TransactionStatus.COMPLETED),
                deposits,
                withdrawals,
                transferRepository.countByStatus(TransactionStatus.COMPLETED));
    }

    @Override
    @Transactional(readOnly = true)
    public StatisticsResponse statistics(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidRequestException("La date de début doit précéder ou égaler la date de fin.");
        }
        LocalDateTime startAt = startDate == null ? FIRST_INSTANT : startDate.atStartOfDay();
        LocalDateTime endAt = endDate == null ? LAST_INSTANT : endDate.plusDays(1).atStartOfDay();

        BigDecimal deposits = Objects.requireNonNullElse(transactionRepository.sumAmount(
                TransactionType.DEPOSIT, TransactionStatus.COMPLETED, startAt, endAt), BigDecimal.ZERO);
        BigDecimal withdrawals = Objects.requireNonNullElse(transactionRepository.sumAmount(
                TransactionType.WITHDRAWAL, TransactionStatus.COMPLETED, startAt, endAt), BigDecimal.ZERO);

        Map<TransactionType, Long> counts = new EnumMap<>(TransactionType.class);
        for (TransactionType type : TransactionType.values()) {
            counts.put(type, 0L);
        }
        for (TransactionTypeCount row : transactionRepository.countByType(TransactionStatus.COMPLETED, startAt, endAt)) {
            counts.put(row.type(), row.count());
        }
        List<TransactionTypeCount> byType = counts.entrySet().stream()
                .map(entry -> new TransactionTypeCount(entry.getKey(), entry.getValue()))
                .toList();

        List<AccountActivity> mostActive = transactionRepository
                .countByAccount(TransactionStatus.COMPLETED, startAt, endAt)
                .stream().limit(TOP_ACCOUNTS).toList();

        List<AccountBalance> balances = accountRepository.findAll(Sort.by("id")).stream()
                .map(account -> new AccountBalance(account.getAccountNumber(), account.getBalance(),
                        account.getCurrency(), account.getStatus()))
                .toList();

        return new StatisticsResponse(startDate, endDate, deposits, withdrawals,
                transferRepository.countInPeriod(TransactionStatus.COMPLETED, startAt, endAt),
                byType, mostActive, balances);
    }
}