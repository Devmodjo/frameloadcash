package com.frameload.cash.enums;

public enum Role {
    ADMIN("ADMIN"),
    AGENT("AGENT"),
    SUPERVISOR("SUPERVISOR");

    private final String v;

    Role(String v) {
        this.v = v;
    }
}
