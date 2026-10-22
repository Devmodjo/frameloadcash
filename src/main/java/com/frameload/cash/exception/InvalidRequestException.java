package com.frameload.cash.exception;

/**
 * Levée lorsque les paramètres d'une requête sont incohérents entre eux (ex. période inversée). Traduite en HTTP 400.
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
