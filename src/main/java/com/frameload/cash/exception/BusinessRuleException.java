package com.frameload.cash.exception;

/**
 * Levée lorsqu'une règle métier est violée (doublon, solde insuffisant, compte bloqué...).
 * Traduite en HTTP 409. Le code fonctionnel est facultatif : sans code, l'erreur porte le code
 * BUSINESS_RULE_VIOLATION. Étant une exception non contrôlée, elle provoque aussi le rollback
 * d'une méthode @Transactional.
 */
public class BusinessRuleException extends RuntimeException {

    private static final String DEFAULT_CODE = "BUSINESS_RULE_VIOLATION";

    private final String code;

    public BusinessRuleException(String message) {
        this(DEFAULT_CODE, message);
    }

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}