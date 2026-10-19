package com.frameload.cash.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;



/**
 * Levée lorsqu'une règle métier est violée (doublon, solde insuffisant, compte bloqué...).
 * Traduite en HTTP 409 avec le code fonctionnel fourni (ex. {@code INSUFFICIENT_FUNDS}).
 * Étant une exception non contrôlée, elle provoque aussi le rollback d'une méthode {@code @Transactional}.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
