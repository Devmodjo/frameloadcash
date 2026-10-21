package com.frameload.cash.services;

import com.frameload.cash.dto.OperationResponse;
import com.frameload.cash.dto.TransferRequest;
import com.frameload.cash.dto.TransferResponse;

public interface TransferServices {

    OperationResponse transfer(TransferRequest request);

    TransferResponse findByReference(String reference);
}