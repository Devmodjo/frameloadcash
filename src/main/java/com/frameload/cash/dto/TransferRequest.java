package com.frameload.cash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Corps JSON d'un virement.
 */
public record TransferRequest(
        @Schema(description = "Identifiant du compte à débiter", example = "1")
        @NotNull(message = "Le compte source est obligatoire.")
        @Positive(message = "L'identifiant du compte source doit être positif.")
        Long sourceAccountId,

        @Schema(description = "Identifiant du compte à créditer (différent de la source)", example = "2")
        @NotNull(message = "Le compte destinataire est obligatoire.")
        @Positive(message = "L'identifiant du compte destinataire doit être positif.")
        Long destinationAccountId,

        @Schema(description = "Montant strictement positif, 2 décimales au plus", example = "30000")
        @NotNull(message = "Le montant est obligatoire.")
        @Positive(message = "Le montant doit être supérieur à zéro.")
        @Digits(integer = 15, fraction = 2, message = "Le montant doit comporter au plus 2 décimales.")
        BigDecimal amount,

        @Schema(description = "Libellé facultatif", example = "Virement de démonstration")
        @Size(max = 255, message = "La description ne doit pas dépasser 255 caractères.")
        String description) {
}