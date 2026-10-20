package com.frameload.cash.enums;

/**
 * Statut d'un compte : seul un compte ACTIVE pourra réaliser des opérations.
 */
public enum AccountStatus {
    ACTIVE("active"),
    BLOCKED("bloqked"),
    CLOSED("closed");

    private final String v;

    AccountStatus(String v) {
        this.v = v;
    }
}