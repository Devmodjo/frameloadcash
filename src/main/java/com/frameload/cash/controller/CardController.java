package com.frameload.cash.controller;

import com.frameload.cash.dto.CardRequest;
import com.frameload.cash.dto.CardResponse;
import com.frameload.cash.enums.CardStatus;
import com.frameload.cash.services.CardServices;
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
 * Endpoints des cartes bancaires fictives. Aucune donnée de paiement n'est collectée ni exposée.
 */
@Tag(name = "Cartes", description = "Cartes bancaires fictives rattachées aux comptes")
@RestController
@RequestMapping("/api/v1")
public class CardController {

    private final CardServices cardServices;

    public CardController(CardServices cardServices) {
        this.cardServices = cardServices;
    }

    @Operation(summary = "Créer une carte fictive pour un compte",
            description = "Référence fictive CD-000001, statut ACTIVE, expiration fictive à 3 ans. Aucun numéro de carte, CVV ou PIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Carte créée"),
            @ApiResponse(responseCode = "400", description = "Type de carte invalide"),
            @ApiResponse(responseCode = "404", description = "Compte introuvable")
    })
    @PostMapping("/accounts/{accountId}/cards")
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse create(@PathVariable Long accountId, @Valid @RequestBody CardRequest request) {
        return cardServices.create(accountId, request);
    }

    @Operation(summary = "Lister les cartes d'un compte")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cartes du compte"),
            @ApiResponse(responseCode = "404", description = "Compte introuvable")
    })
    @GetMapping("/accounts/{accountId}/cards")
    public List<CardResponse> listByAccount(@PathVariable Long accountId) {
        return cardServices.findByAccountId(accountId);
    }

    @Operation(summary = "Consulter une carte", description = "Retourne le statut et la date d'expiration fictive.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carte trouvée"),
            @ApiResponse(responseCode = "404", description = "Carte introuvable")
    })
    @GetMapping("/cards/{id}")
    public CardResponse get(@PathVariable Long id) {
        return cardServices.findById(id);
    }

    @Operation(summary = "Bloquer, réactiver ou marquer une carte comme expirée",
            description = "Une carte expirée est définitive et ne peut plus changer de statut.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut modifié"),
            @ApiResponse(responseCode = "400", description = "Statut inconnu"),
            @ApiResponse(responseCode = "404", description = "Carte introuvable"),
            @ApiResponse(responseCode = "409", description = "Carte expirée")
    })
    @PatchMapping("/cards/{id}/status")
    public CardResponse changeStatus(@PathVariable Long id,
                                     @Parameter(description = "Nouveau statut : ACTIVE, BLOCKED ou EXPIRED")
                                     @RequestParam CardStatus status) {
        return cardServices.changeStatus(id, status);
    }
}