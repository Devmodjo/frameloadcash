package com.frameload.cash.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corps JSON pour créer ou modifier un client. Les contraintes Bean Validation sont déclenchées
 * par {@code @Valid} dans le contrôleur, avant tout traitement métier.
 *
 * @param firstName prénom (obligatoire)
 * @param lastName  nom (obligatoire)
 * @param email     adresse e-mail valide, unique
 * @param phone     numéro fictif facultatif (chiffres, espaces, + initial)
 * @param address   adresse facultative
 */
public record ClientRequest(
        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères.")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
        String lastName,

        @NotBlank(message = "L'adresse e-mail est obligatoire.")
        @Email(message = "L'adresse e-mail n'est pas valide.")
        @Size(max = 150, message = "L'adresse e-mail ne doit pas dépasser 150 caractères.")
        String email,

        @Pattern(regexp = "^\\+?[0-9 ]{6,20}$", message = "Le numéro de téléphone n'est pas valide.")
        String phone,

        @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères.")
        String address
) {
}