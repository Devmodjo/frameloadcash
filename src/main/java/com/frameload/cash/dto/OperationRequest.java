package com.frameload.cash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Corps JSON d'un dépôt ou d'un retrait. Le montant est un {@link BigDecimal} : jamais de double pour l'argent.
 */
public record OperationRequest(
        @Schema(description = "Montant strictement positif, 2 décimales au plus", example = "25000")
        @NotNull(message = "Le montant est obligatoire.")
        @Positive(message = "Le montant doit être supérieur à zéro.")
        @Digits(integer = 15, fraction = 2, message = "Le montant doit comporter au plus 2 décimales.")
        BigDecimal amount,

        @Schema(description = "Libellé facultatif", example = "Dépôt de démonstration")
        @Size(max = 255, message = "La description ne doit pas dépasser 255 caractères.")
        String description) {
}