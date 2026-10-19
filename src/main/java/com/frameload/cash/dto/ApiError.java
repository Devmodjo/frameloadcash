package com.frameload.cash.dto;

import java.time.LocalDateTime;

/**
 * Corps JSON normalisé de toutes les erreurs renvoyées par l'API.
 *
 * @param timestamp instant de l'erreur
 * @param status    code HTTP
 * @param error     code fonctionnel (ex. VALIDATION_ERROR, NOT_FOUND)
 * @param message   message compréhensible, sans détail technique
 * @param path      URL appelée
 */
public record ApiError(LocalDateTime timestamp, int status, String error, String message, String path) {
}