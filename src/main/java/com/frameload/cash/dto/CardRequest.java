package com.frameload.cash.dto;

import com.frameload.cash.enums.CardType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Corps JSON pour créer une carte fictive. Le compte vient de l'URL ; la référence, le statut et la date
 * d'expiration sont décidés par l'application. Aucune donnée de paiement n'est demandée.
 */
public record CardRequest(
        @Schema(description = "Type de carte : DEBIT ou PREPAID", example = "DEBIT")
        @NotNull(message = "Le type de carte est obligatoire.")
        CardType cardType) {
}