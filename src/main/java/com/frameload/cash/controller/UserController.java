package com.frameload.cash.controller;

import com.frameload.cash.dto.UserRequest;
import com.frameload.cash.dto.UserResponse;
import com.frameload.cash.enums.UserStatus;
import com.frameload.cash.services.UserServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de gestion des utilisateurs de démonstration. Ils seront réservés au rôle ADMIN avec la sécurité
 * des endpoints (commit suivant).
 */
@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs de démonstration et de leur rôle")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserServices userServices;

    public UserController(UserServices userServices) {
        this.userServices = userServices;
    }

    @Operation(summary = "Créer un utilisateur",
            description = "Le mot de passe est haché en BCrypt et n'est jamais renvoyé.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Utilisateur créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "409", description = "Nom d'utilisateur déjà utilisé")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        return userServices.create(request);
    }

    @Operation(summary = "Lister les utilisateurs", description = "Ni mot de passe, ni hachage dans la réponse.")
    @ApiResponse(responseCode = "200", description = "Liste des utilisateurs")
    @GetMapping
    public List<UserResponse> list() {
        return userServices.findAll();
    }

    @Operation(summary = "Activer ou désactiver un utilisateur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut modifié"),
            @ApiResponse(responseCode = "400", description = "Statut inconnu"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @PatchMapping("/{id}/status")
    public UserResponse changeStatus(@PathVariable Long id,
                                     @Parameter(description = "Nouveau statut : ACTIVE ou INACTIVE")
                                     @RequestParam UserStatus status) {
        return userServices.changeStatus(id, status);
    }
}