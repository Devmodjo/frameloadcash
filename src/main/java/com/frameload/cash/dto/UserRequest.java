package com.frameload.cash.dto;

import com.frameload.cash.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corps JSON pour créer un utilisateur de démonstration. Le mot de passe est reçu en clair, haché immédiatement
 * par le service, puis jamais renvoyé. La limite de 72 caractères est celle de BCrypt.
 */
public record UserRequest(
        @Schema(description = "Nom d'utilisateur unique (lettres, chiffres, point, tiret, soulignement)", example = "agent.fotso")
        @NotBlank(message = "Le nom d'utilisateur est obligatoire.")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$",
                message = "Le nom d'utilisateur doit contenir de 3 à 50 caractères (lettres, chiffres, . _ -).")
        String username,

        @Schema(description = "Mot de passe de 8 à 72 caractères", example = "mot-de-passe-fictif")
        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 8, max = 72, message = "Le mot de passe doit contenir de 8 à 72 caractères.")
        String password,

        @Schema(description = "Rôle : ADMIN, AGENT ou SUPERVISOR", example = "AGENT")
        @NotNull(message = "Le rôle est obligatoire.")
        Role role) {
}