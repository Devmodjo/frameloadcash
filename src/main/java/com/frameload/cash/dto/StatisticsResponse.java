package com.frameload.cash.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Statistiques financières sur une période (cahier des charges §15). Les dates sont nulles si la période n'est pas bornée.
 */
public record StatisticsResponse(LocalDate startDate, LocalDate endDate, BigDecimal totalDeposits,
                                 BigDecimal totalWithdrawals, long completedTransfers,
                                 List<TransactionTypeCount> transactionsByType,
                                 List<AccountActivity> mostActiveAccounts,
                                 List<AccountBalance> accountBalances) {
}