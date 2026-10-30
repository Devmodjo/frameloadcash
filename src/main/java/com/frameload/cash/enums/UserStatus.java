package com.frameload.cash.enums;

public enum UserStatus {

    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE");

    private String v;

    UserStatus(String v) {
        this.v = v;
    }
}
