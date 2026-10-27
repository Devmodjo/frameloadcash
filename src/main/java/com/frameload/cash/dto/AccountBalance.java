package com.frameload.cash.dto;

import com.frameload.cash.enums.AccountStatus;
import java.math.BigDecimal;

/**
 * Solde actuel d'un compte.
 */
public record AccountBalance(String accountNumber, BigDecimal balance, String currency, AccountStatus status) {
}