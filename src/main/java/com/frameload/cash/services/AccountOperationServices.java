package com.frameload.cash.services;

import com.frameload.cash.dto.OperationRequest;
import com.frameload.cash.dto.OperationResponse;

public interface AccountOperationServices {

    OperationResponse deposit(Long accountId, OperationRequest request);
}