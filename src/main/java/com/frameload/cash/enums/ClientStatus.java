package com.frameload.cash.enums;

public enum ClientStatus {

    ACTIVE("active"),
    INACTIVE("inactive");

    private final String v;

    ClientStatus(String v) {
        this.v = v;
    }
}
