package com.frameload.cash.dto;

import com.frameload.cash.entity.Transfer;
import com.frameload.cash.enums.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Détail d'un virement exposé par l'API.
 */
public record TransferResponse(String reference, Long sourceAccountId, Long destinationAccountId,
                               BigDecimal amount, TransactionStatus status, LocalDateTime createdAt) {

    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(transfer.getReference(), transfer.getSourceAccount().getId(),
                transfer.getDestinationAccount().getId(), transfer.getAmount(), transfer.getStatus(),
                transfer.getCreatedAt());
    }
}