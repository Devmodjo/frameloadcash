package com.frameload.cash.controller;

import com.frameload.cash.dto.ClientRequest;
import com.frameload.cash.dto.ClientResponse;
import com.frameload.cash.enums.ClientStatus;
import com.frameload.cash.services.ClientServices;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST des clients : valide l'entrée, délègue au service, choisit le code HTTP.
 */
@Tag(name = "Clients", description = "Gestion des clients fictifs de la banque")
@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final ClientServices clientServices;

    public ClientController(ClientServices clientServices) {
        this.clientServices = clientServices;
    }

    @Operation(summary = "Créer un client", description = "Le numéro client (CL-000001) et le statut ACTIVE sont attribués par l'application.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "409", description = "Adresse e-mail déjà utilisée")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse create(@Valid @RequestBody ClientRequest request) {
        return clientServices.create(request);
    }

    @Operation(summary = "Lister ou rechercher les clients",
            description = "Sans paramètre, retourne tous les clients. Avec search, filtre par numéro client, "
                    + "prénom, nom ou e-mail (contient, sans tenir compte de la casse).")
    @ApiResponse(responseCode = "200", description = "Liste des clients")
    @GetMapping
    public List<ClientResponse> list(
            @Parameter(description = "Texte à rechercher", example = "nkoulou")
            @RequestParam(required = false) String search) {
        return clientServices.search(search);
    }

    @Operation(summary = "Consulter un client par identifiant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client trouvé"),
            @ApiResponse(responseCode = "404", description = "Client introuvable")
    })
    @GetMapping("/{id}")
    public ClientResponse get(@PathVariable Long id) {
        return clientServices.findById(id);
    }

    @Operation(summary = "Modifier un client", description = "Le numéro client et la date de création ne changent jamais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client modifié"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Client introuvable"),
            @ApiResponse(responseCode = "409", description = "Adresse e-mail déjà utilisée par un autre client")
    })
    @PutMapping("/{id}")
    public ClientResponse update(@PathVariable Long id, @Valid @RequestBody ClientRequest request) {
        return clientServices.update(id, request);
    }

    @Operation(summary = "Activer ou désactiver un client", description = "La désactivation ne supprime ni les comptes ni l'historique.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut modifié"),
            @ApiResponse(responseCode = "400", description = "Statut inconnu"),
            @ApiResponse(responseCode = "404", description = "Client introuvable")
    })
    @PatchMapping("/{id}/status")
    public ClientResponse changeStatus(@PathVariable Long id,
                                       @Parameter(description = "Nouveau statut : ACTIVE ou INACTIVE")
                                       @RequestParam ClientStatus status) {
        return clientServices.changeStatus(id, status);
    }
}