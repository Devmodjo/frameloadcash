package com.frameload.cash.dto;

import java.math.BigDecimal;

/**
 * Indicateurs de synthèse calculés à partir des données persistées (cahier des charges §14).
 * Les volumes mesurent l'activité de la banque, pas son bénéfice.
 */
public record DashboardResponse(long totalClients, long totalAccounts, long activeAccounts, long activeCards,
                                long completedTransactions, BigDecimal totalDepositsVolume,
                                BigDecimal totalWithdrawalsVolume, long completedTransfers) {
}