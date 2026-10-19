package com.frameload.cash.services;

import java.util.UUID;


/**
 * Fabrique les références métier (CL-000001, FC-000001, TX-000001...).
 * Une référence dérive de l'identifiant généré par la base : on enregistre d'abord l'objet avec une
 * référence temporaire unique, puis on la remplace par la référence définitive dans la même transaction.
 */
public final class ReferenceGenerator {

    private ReferenceGenerator() {
    }

    public static String temporary() {
        return "TMP-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String format(String prefix, Long id) {
        return String.format("%s-%06d", prefix, id);
    }
}