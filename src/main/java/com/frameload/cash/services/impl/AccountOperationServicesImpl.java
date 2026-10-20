package com.frameload.cash.services.impl;

import com.frameload.cash.dto.OperationRequest;
import com.frameload.cash.dto.OperationResponse;
import com.frameload.cash.entity.Account;
import com.frameload.cash.entity.Transaction;
import com.frameload.cash.enums.AccountStatus;
import com.frameload.cash.enums.TransactionType;
import com.frameload.cash.exception.BusinessRuleException;
import com.frameload.cash.exception.ResourceNotFoundException;
import com.frameload.cash.repositories.AccountRepository;
import com.frameload.cash.repositories.TransactionRepository;
import com.frameload.cash.services.AccountOperationServices;
import com.frameload.cash.services.ReferenceGenerator;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opérations sur un compte (dépôts, puis retraits). Chaque opération est atomique : la mise à jour du solde
 * et l'enregistrement de la transaction sont validés ensemble, ou annulés ensemble (rollback) si une exception survient.
 * Le solde de l'entité est enregistré automatiquement au commit (dirty checking d'Hibernate).
 */
@Service
public class AccountOperationServicesImpl implements AccountOperationServices {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountOperationServicesImpl(AccountRepository accountRepository,
                                        TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public OperationResponse deposit(Long accountId, OperationRequest request) {
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new BusinessRuleException("Le montant doit être supérieur à zéro.");
        }

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable : " + accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "Ce compte n'est pas actif (statut : " + account.getStatus() + ") : aucune opération n'est possible.");
        }

        account.setBalance(account.getBalance().add(request.amount()));

        Transaction transaction = new Transaction(account, TransactionType.DEPOSIT,
                request.amount(), request.description());
        transaction.assignReference(ReferenceGenerator.temporary());
        transactionRepository.saveAndFlush(transaction);
        transaction.assignReference(ReferenceGenerator.format("TX", transaction.getId()));

        return new OperationResponse("Dépôt effectué avec succès.", transaction.getReference(),
                transaction.getStatus(),
                Map.of("accountId", accountId,
                        "amount", transaction.getAmount(),
                        "newBalance", account.getBalance()));
    }
}