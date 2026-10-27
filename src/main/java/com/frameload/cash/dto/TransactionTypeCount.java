package com.frameload.cash.dto;

import com.frameload.cash.enums.TransactionType;

/**
 * Nombre de transactions d'un type donné.
 */
public record TransactionTypeCount(TransactionType type, Long count) {
}