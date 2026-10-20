package com.frameload.cash.services;

import com.frameload.cash.dto.AccountRequest;
import com.frameload.cash.dto.AccountResponse;
import com.frameload.cash.enums.AccountStatus;
import java.util.List;

public interface AccountServices {

    AccountResponse create(AccountRequest request);

    List<AccountResponse> findAll();

    AccountResponse findById(Long id);

    AccountResponse findByAccountNumber(String accountNumber);

    List<AccountResponse> findByClientId(Long clientId);

    AccountResponse changeStatus(Long id, AccountStatus status);
}