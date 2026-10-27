package com.frameload.cash.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Teste le contrat HTTP du tableau de bord et des statistiques : structure du JSON, filtres et erreurs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dashboard_returnsAllTheIndicators() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClients").isNumber())
                .andExpect(jsonPath("$.totalAccounts").isNumber())
                .andExpect(jsonPath("$.activeAccounts").isNumber())
                .andExpect(jsonPath("$.activeCards").isNumber())
                .andExpect(jsonPath("$.completedTransactions").isNumber())
                .andExpect(jsonPath("$.totalDepositsVolume").isNumber())
                .andExpect(jsonPath("$.totalWithdrawalsVolume").isNumber())
                .andExpect(jsonPath("$.completedTransfers").isNumber());
    }

    @Test
    void statistics_withoutDates_coversTheWholePeriod() throws Exception {
        mockMvc.perform(get("/api/v1/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDeposits").isNumber())
                .andExpect(jsonPath("$.transactionsByType.length()").value(4))
                .andExpect(jsonPath("$.mostActiveAccounts").isArray())
                .andExpect(jsonPath("$.accountBalances").isArray());
    }

    @Test
    void statistics_forAPeriodWithoutOperations_returnsZeros() throws Exception {
        mockMvc.perform(get("/api/v1/statistics").param("startDate", "2000-01-01").param("endDate", "2000-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDeposits").value(0))
                .andExpect(jsonPath("$.totalWithdrawals").value(0))
                .andExpect(jsonPath("$.completedTransfers").value(0))
                .andExpect(jsonPath("$.startDate").value("2000-01-01"))
                .andExpect(jsonPath("$.endDate").value("2000-01-31"));
    }

    @Test
    void statistics_withAnInvertedPeriod_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/statistics").param("startDate", "2026-12-31").param("endDate", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    void statistics_withAnInvalidDate_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/statistics").param("startDate", "hier"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }
}