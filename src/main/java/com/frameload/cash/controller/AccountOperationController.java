package com.frameload.cash.controller;

import com.frameload.cash.dto.OperationRequest;
import com.frameload.cash.dto.OperationResponse;
import com.frameload.cash.services.AccountOperationServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints des opérations sur un compte. Aucun endpoint ne permet de modifier un solde librement.
 */
@Tag(name = "Opérations", description = "Dépôts, retraits et virements sur les comptes")
@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
public class AccountOperationController {

    private final AccountOperationServices operationServices;

    public AccountOperationController(AccountOperationServices operationServices) {
        this.operationServices = operationServices;
    }

    @Operation(summary = "Effectuer un dépôt",
            description = "Augmente le solde et enregistre une transaction DEPOSIT avec une référence unique (TX-000001).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dépôt effectué"),
            @ApiResponse(responseCode = "400", description = "Montant invalide (nul, négatif, non numérique, trop de décimales)"),
            @ApiResponse(responseCode = "404", description = "Compte introuvable"),
            @ApiResponse(responseCode = "409", description = "Compte bloqué ou clôturé")
    })
    @PostMapping("/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public OperationResponse deposit(@PathVariable Long accountId, @Valid @RequestBody OperationRequest request) {
        return operationServices.deposit(accountId, request);
    }
}