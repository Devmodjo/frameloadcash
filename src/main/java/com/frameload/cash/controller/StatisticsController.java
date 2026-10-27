package com.frameload.cash.controller;

import com.frameload.cash.dto.DashboardResponse;
import com.frameload.cash.dto.StatisticsResponse;
import com.frameload.cash.services.StatisticsServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de consultation du tableau de bord et des statistiques (lecture seule, aucune page graphique).
 */
@Tag(name = "Synthèse", description = "Tableau de bord et statistiques financières")
@RestController
@RequestMapping("/api/v1")
public class StatisticsController {

    private final StatisticsServices statisticsServices;

    public StatisticsController(StatisticsServices statisticsServices) {
        this.statisticsServices = statisticsServices;
    }

    @Operation(summary = "Tableau de bord",
            description = "Clients, comptes (et comptes actifs), cartes actives, transactions validées, volumes des dépôts "
                    + "et des retraits, virements. Les volumes mesurent l'activité de la banque, pas son bénéfice.")
    @ApiResponse(responseCode = "200", description = "Indicateurs de synthèse")
    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return statisticsServices.dashboard();
    }

    @Operation(summary = "Statistiques financières sur une période",
            description = "Totaux des dépôts et des retraits, virements terminés, transactions par type, comptes les plus actifs "
                    + "et soldes actuels. Les bornes de période sont incluses : startDate commence à 00:00:00 et endDate "
                    + "couvre toute la journée. Sans date, toute la période est prise en compte.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistiques de la période"),
            @ApiResponse(responseCode = "400", description = "Date invalide ou période inversée")
    })
    @GetMapping("/statistics")
    public StatisticsResponse statistics(
            @Parameter(description = "Date de début incluse (AAAA-MM-JJ)", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Date de fin incluse (AAAA-MM-JJ)", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return statisticsServices.statistics(startDate, endDate);
    }
}