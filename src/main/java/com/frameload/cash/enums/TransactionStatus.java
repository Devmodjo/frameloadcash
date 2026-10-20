package com.frameload.cash.enums;

/**
 * Statut d'une transaction. Seules les opérations réussies sont enregistrées (COMPLETED) :
 * une opération refusée est annulée. FAILED est réservé à un futur journal d'audit.
 */
public enum TransactionStatus {
    COMPLETED("complete"),
    FAILED("failed");

    private final String v;

    TransactionStatus(String v) {
        this.v = v;
    }
}