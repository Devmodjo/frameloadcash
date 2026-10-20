package com.frameload.cash.dto;

import com.frameload.cash.entity.Account;
import com.frameload.cash.enums.AccountStatus;
import com.frameload.cash.enums.AccountType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Représentation d'un compte exposée par l'API.
 */
public record AccountResponse(Long id, String accountNumber, Long clientId, AccountType accountType,
                              BigDecimal balance, String currency, AccountStatus status, LocalDate createdAt) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getAccountNumber(), account.getClient().getId(),
                account.getAccountType(), account.getBalance(), account.getCurrency(), account.getStatus(),
                account.getCreatedAt());
    }
}