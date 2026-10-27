package com.frameload.cash.dto;

/**
 * Nombre d'opérations enregistrées sur un compte (sert au classement des comptes les plus actifs).
 */
public record AccountActivity(Long accountId, String accountNumber, Long operations) {
}