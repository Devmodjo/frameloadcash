package com.frameload.cash.dto;

import com.frameload.cash.enums.TransactionStatus;
import java.util.Map;

/**
 * Réponse normalisée d'une opération financière (cahier des charges ) : message lisible,
 * référence de la transaction, statut et détails (compte, montant, nouveau solde).
 */
public record OperationResponse(String message, String reference, TransactionStatus status,
                                Map<String, Object> data) {
}