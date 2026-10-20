package com.frameload.cash.enums;

/**
 * Nature d'une écriture sur un compte.
 */
public enum TransactionType {
    DEPOSIT("depot"),
    WITHDRAWAL("retrait"),
    TRANSFER_IN("transfer_in"),
    TRANSFER_OUT("transfer_out");

    private final String v;

    TransactionType(String v) {
        this.v = v;
    }
}