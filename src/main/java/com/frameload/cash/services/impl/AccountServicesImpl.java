package com.frameload.cash.services.impl;

import com.frameload.cash.dto.AccountRequest;
import com.frameload.cash.dto.AccountResponse;
import com.frameload.cash.entity.Account;
import com.frameload.cash.entity.Client;
import com.frameload.cash.enums.AccountStatus;
import com.frameload.cash.enums.ClientStatus;
import com.frameload.cash.exception.BusinessRuleException;
import com.frameload.cash.exception.ResourceNotFoundException;
import com.frameload.cash.repositories.AccountRepository;
import com.frameload.cash.repositories.ClientRepository;
import com.frameload.cash.services.AccountServices;
import com.frameload.cash.services.ReferenceGenerator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Règles métier des comptes (cahier des charges, section 7). Les réponses sont construites dans la
 * transaction, car la relation vers le client est chargée à la demande (LAZY).
 */
@Service
public class AccountServicesImpl implements AccountServices {

    private static final String CURRENCY = "XAF";

    private final AccountRepository accountRepository;
    private final ClientRepository clientRepository;

    public AccountServicesImpl(AccountRepository accountRepository, ClientRepository clientRepository) {
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    @Transactional
    public AccountResponse create(AccountRequest request) {
        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + request.clientId()));

        if (client.getStatus() != ClientStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "Un client désactivé ne peut pas ouvrir de nouveau compte.");
        }

        Account account = new Account(client, request.accountType(), CURRENCY);
        account.assignAccountNumber(ReferenceGenerator.temporary());
        accountRepository.saveAndFlush(account);
        account.assignAccountNumber(ReferenceGenerator.format("FC", account.getId()));
        return AccountResponse.from(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> findAll() {
        return accountRepository.findAll().stream().map(AccountResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse findById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable : " + id));
        return AccountResponse.from(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse findByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable : " + accountNumber));
        return AccountResponse.from(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> findByClientId(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client introuvable : " + clientId);
        }
        return accountRepository.findByClientId(clientId).stream().map(AccountResponse::from).toList();
    }

    /**
     * Bloque, réactive ou clôture un compte. Un compte clôturé est définitif et ne peut être clôturé
     * que si son solde est nul. La clôture ne supprime rien : le compte et son historique restent consultables.
     */
    @Override
    @Transactional
    public AccountResponse changeStatus(Long id, AccountStatus status) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable : " + id));

        if (account.getStatus() == AccountStatus.CLOSED && status != AccountStatus.CLOSED) {
            throw new BusinessRuleException("Un compte clôturé ne peut plus être modifié.");
        }
        if (status == AccountStatus.CLOSED && account.getBalance().signum() != 0) {
            throw new BusinessRuleException(
                    "Un compte ne peut être clôturé que si son solde est nul.");
        }

        account.setStatus(status);
        return AccountResponse.from(account);
    }
}