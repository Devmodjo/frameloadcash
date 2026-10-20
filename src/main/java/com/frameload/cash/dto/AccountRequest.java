package com.frameload.cash.dto;

import com.frameload.cash.enums.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Corps JSON pour ouvrir un compte. Le solde, le numéro, la devise et le statut ne sont jamais
 * envoyés par le client : ils sont décidés par l'application.
 */
public record AccountRequest(
        @Schema(description = "Identifiant du client propriétaire", example = "1")
        @NotNull(message = "Le client est obligatoire.")
        @Positive(message = "L'identifiant du client doit être positif.")
        Long clientId,

        @Schema(description = "Type de compte : CURRENT ou SAVINGS", example = "CURRENT")
        @NotNull(message = "Le type de compte est obligatoire.")
        AccountType accountType) {
}