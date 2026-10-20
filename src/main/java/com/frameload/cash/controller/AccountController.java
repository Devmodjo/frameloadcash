package com.frameload.cash.controller;

import com.frameload.cash.dto.AccountRequest;
import com.frameload.cash.dto.AccountResponse;
import com.frameload.cash.enums.AccountStatus;
import com.frameload.cash.services.AccountServices;
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
 * Endpoints REST des comptes : valide l'entrée, délègue au service, choisit le code HTTP.
 */
@Tag(name = "Comptes", description = "Gestion des comptes bancaires de démonstration")
@RestController
@RequestMapping("/api/v1")
public class AccountController {

    private final AccountServices accountServices;

    public AccountController(AccountServices accountServices) {
        this.accountServices = accountServices;
    }

    @Operation(summary = "Ouvrir un compte",
            description = "Solde nul, statut ACTIVE et numéro FC-000001 attribués par l'application.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Client introuvable"),
            @ApiResponse(responseCode = "409", description = "Client désactivé")
    })
    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody AccountRequest request) {
        return accountServices.create(request);
    }

    @Operation(summary = "Lister les comptes")
    @ApiResponse(responseCode = "200", description = "Liste des comptes")
    @GetMapping("/accounts")
    public List<AccountResponse> list() {
        return accountServices.findAll();
    }

    @Operation(summary = "Consulter un compte par identifiant", description = "Retourne le solde et le statut du compte.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte trouvé"),
            @ApiResponse(responseCode = "404", description = "Compte introuvable")
    })
    @GetMapping("/accounts/{id}")
    public AccountResponse get(@PathVariable Long id) {
        return accountServices.findById(id);
    }

    @Operation(summary = "Consulter un compte par numéro", description = "Exemple de numéro : FC-000001.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte trouvé"),
            @ApiResponse(responseCode = "404", description = "Compte introuvable")
    })
    @GetMapping("/accounts/number/{accountNumber}")
    public AccountResponse getByNumber(@PathVariable String accountNumber) {
        return accountServices.findByAccountNumber(accountNumber);
    }

    @Operation(summary = "Lister les comptes d'un client")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comptes du client"),
            @ApiResponse(responseCode = "404", description = "Client introuvable")
    })
    @GetMapping("/clients/{clientId}/accounts")
    public List<AccountResponse> listByClient(@PathVariable Long clientId) {
        return accountServices.findByClientId(clientId);
    }

    @Operation(summary = "Bloquer, réactiver ou clôturer un compte",
            description = "Un compte clôturé est définitif et ne peut être clôturé que si son solde est nul.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut modifié"),
            @ApiResponse(responseCode = "400", description = "Statut inconnu"),
            @ApiResponse(responseCode = "404", description = "Compte introuvable"),
            @ApiResponse(responseCode = "409", description = "Compte clôturé ou solde non nul")
    })
    @PatchMapping("/accounts/{id}/status")
    public AccountResponse changeStatus(@PathVariable Long id,
                                        @Parameter(description = "Nouveau statut : ACTIVE, BLOCKED ou CLOSED")
                                        @RequestParam AccountStatus status) {
        return accountServices.changeStatus(id, status);
    }
}