package com.frameload.cash.controller;

import com.frameload.cash.dto.OperationResponse;
import com.frameload.cash.dto.TransferRequest;
import com.frameload.cash.dto.TransferResponse;
import com.frameload.cash.services.TransferServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints des virements entre comptes.
 */
@Tag(name = "Virements", description = "Virements entre deux comptes, atomiques")
@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferServices transferServices;

    public TransferController(TransferServices transferServices) {
        this.transferServices = transferServices;
    }

    @Operation(summary = "Effectuer un virement",
            description = "Débite le compte source, crédite le compte destinataire et enregistre le virement "
                    + "et ses deux transactions dans une seule transaction : tout réussit ou rien n'est modifié.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Virement effectué"),
            @ApiResponse(responseCode = "400", description = "Données invalides (montant, identifiants)"),
            @ApiResponse(responseCode = "404", description = "Compte source ou destinataire introuvable"),
            @ApiResponse(responseCode = "409", description = "Même compte, solde insuffisant, compte bloqué ou clôturé")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OperationResponse transfer(@Valid @RequestBody TransferRequest request) {
        return transferServices.transfer(request);
    }

    @Operation(summary = "Consulter un virement par référence", description = "Exemple de référence : TR-000001.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Virement trouvé"),
            @ApiResponse(responseCode = "404", description = "Virement introuvable")
    })
    @GetMapping("/{reference}")
    public TransferResponse get(@PathVariable String reference) {
        return transferServices.findByReference(reference);
    }
}