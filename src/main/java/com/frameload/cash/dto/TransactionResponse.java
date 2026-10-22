package com.frameload.cash.dto;

import com.frameload.cash.entity.Transaction;
import com.frameload.cash.enums.TransactionStatus;
import com.frameload.cash.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Écriture de l'historique exposée par l'API. {@code transferReference} n'est renseignée que pour un virement.
 */
public record TransactionResponse(Long id, String reference, Long accountId, String accountNumber,
                                  TransactionType type, BigDecimal amount, String description,
                                  TransactionStatus status, LocalDateTime createdAt, String transferReference) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getReference(),
                transaction.getAccount().getId(), transaction.getAccount().getAccountNumber(),
                transaction.getType(), transaction.getAmount(), transaction.getDescription(),
                transaction.getStatus(), transaction.getCreatedAt(),
                transaction.getTransfer() == null ? null : transaction.getTransfer().getReference());
    }
}