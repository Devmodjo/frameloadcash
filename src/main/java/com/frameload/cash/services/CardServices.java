package com.frameload.cash.services;

import com.frameload.cash.dto.CardRequest;
import com.frameload.cash.dto.CardResponse;
import com.frameload.cash.enums.CardStatus;
import java.util.List;

public interface CardServices {

    CardResponse create(Long accountId, CardRequest request);

    List<CardResponse> findByAccountId(Long accountId);

    CardResponse findById(Long id);

    CardResponse changeStatus(Long id, CardStatus status);
}