package com.frameload.cash.services;

import com.frameload.cash.dto.TransactionResponse;
import com.frameload.cash.enums.TransactionType;
import java.time.LocalDate;
import java.util.List;

public interface TransactionServices {

    List<TransactionResponse> findAll(Long accountId, TransactionType type, LocalDate startDate, LocalDate endDate);

    TransactionResponse findByReference(String reference);
}