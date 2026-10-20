package com.frameload.cash.enums;


/**
 * Type de compte de demonstration
 */
public enum AccountType {

    CURRENT("CURRENT"),
    SAVING("SAVING");

    private final String v;

    AccountType(String v) {
        this.v = v;
    }
}
