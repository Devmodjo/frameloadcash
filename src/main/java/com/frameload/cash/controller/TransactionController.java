package com.frameload.cash.controller;

import com.frameload.cash.dto.TransactionResponse;
import com.frameload.cash.enums.TransactionType;
import com.frameload.cash.services.TransactionServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consultation de l'historique des transactions. Les dates sont au format AAAA-MM-JJ, bornes incluses.
 */
@Tag(name = "Transactions", description = "Historique des dépôts, retraits et virements")
@RestController
@RequestMapping("/api/v1")
public class TransactionController {

    private final TransactionServices transactionServices;

    public TransactionController(TransactionServices transactionServices) {
        this.transactionServices = transactionServices;
    }

    @Operation(summary = "Lister les transactions",
            description = "Du plus récent au plus ancien. Tous les filtres sont facultatifs et combinables. "
                    + "Les bornes de période sont incluses : startDate commence à 00:00:00 et endDate couvre toute la journée.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des transactions"),
            @ApiResponse(responseCode = "400", description = "Type ou date invalide, ou période inversée"),
            @ApiResponse(responseCode = "404", description = "Compte introuvable")
    })
    @GetMapping("/transactions")
    public List<TransactionResponse> list(
            @Parameter(description = "Identifiant du compte") @RequestParam(required = false) Long accountId,
            @Parameter(description = "DEPOSIT, WITHDRAWAL, TRANSFER_IN ou TRANSFER_OUT")
            @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Date de début incluse (AAAA-MM-JJ)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Date de fin incluse (AAAA-MM-JJ)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return transactionServices.findAll(accountId, type, startDate, endDate);
    }

    @Operation(summary = "Consulter une transaction par référence", description = "Exemple de référence : TX-000001.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction trouvée"),
            @ApiResponse(responseCode = "404", description = "Transaction introuvable")
    })
    @GetMapping("/transactions/{reference}")
    public TransactionResponse getByReference(@PathVariable String reference) {
        return transactionServices.findByReference(reference);
    }

    @Operation(summary = "Historique d'un compte",
            description = "Mêmes filtres que la liste générale. Les bornes de période sont incluses.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions du compte"),
            @ApiResponse(responseCode = "400", description = "Type ou date invalide, ou période inversée"),
            @ApiResponse(responseCode = "404", description = "Compte introuvable")
    })
    @GetMapping("/accounts/{accountId}/transactions")
    public List<TransactionResponse> listByAccount(
            @PathVariable Long accountId,
            @Parameter(description = "DEPOSIT, WITHDRAWAL, TRANSFER_IN ou TRANSFER_OUT")
            @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Date de début incluse (AAAA-MM-JJ)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Date de fin incluse (AAAA-MM-JJ)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return transactionServices.findAll(accountId, type, startDate, endDate);
    }
}