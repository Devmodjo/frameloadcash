package com.frameload.cash.services;

import com.frameload.cash.dto.DashboardResponse;
import com.frameload.cash.dto.StatisticsResponse;
import java.time.LocalDate;

public interface StatisticsServices {

    DashboardResponse dashboard();

    StatisticsResponse statistics(LocalDate startDate, LocalDate endDate);
}