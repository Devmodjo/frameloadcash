package com.frameload.cash.services.impl;

import com.frameload.cash.dto.OperationResponse;
import com.frameload.cash.dto.TransferRequest;
import com.frameload.cash.dto.TransferResponse;
import com.frameload.cash.entity.Account;
import com.frameload.cash.entity.Transaction;
import com.frameload.cash.entity.Transfer;
import com.frameload.cash.enums.AccountStatus;
import com.frameload.cash.enums.TransactionType;
import com.frameload.cash.exception.BusinessRuleException;
import com.frameload.cash.exception.ResourceNotFoundException;
import com.frameload.cash.repositories.AccountRepository;
import com.frameload.cash.repositories.TransactionRepository;
import com.frameload.cash.repositories.TransferRepository;
import com.frameload.cash.services.ReferenceGenerator;
import com.frameload.cash.services.TransferServices;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Virements entre comptes (cahier des charges, section 11). Toute la méthode {@link #transfer} s'exécute dans
 * UNE SEULE transaction : débit, crédit, virement et les deux écritures sont validés ensemble (commit)
 * ou tous annulés (rollback) dès qu'une exception est levée, à n'importe quelle étape.
 * Les deux comptes sont verrouillés dans l'ordre croissant de leur identifiant : un virement A vers B
 * et un virement B vers A simultanés ne peuvent donc pas se bloquer mutuellement (interblocage).
 */
@Service
public class TransferServicesImpl implements TransferServices {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final TransactionRepository transactionRepository;

    public TransferServicesImpl(AccountRepository accountRepository, TransferRepository transferRepository,
                                TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public OperationResponse transfer(TransferRequest request) {
        Long sourceId = request.sourceAccountId();
        Long destinationId = request.destinationAccountId();
        BigDecimal amount = request.amount();

        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Le montant doit être supérieur à zéro.");
        }
        if (sourceId.equals(destinationId)) {
            throw new BusinessRuleException(
                    "Le compte source et le compte destinataire doivent être différents.");
        }

        Account first = accountRepository.findByIdForUpdate(Math.min(sourceId, destinationId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Compte introuvable : " + Math.min(sourceId, destinationId)));
        Account second = accountRepository.findByIdForUpdate(Math.max(sourceId, destinationId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Compte introuvable : " + Math.max(sourceId, destinationId)));
        Account source = sourceId < destinationId ? first : second;
        Account destination = sourceId < destinationId ? second : first;

        if (source.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "Le compte source n'est pas actif (statut : " + source.getStatus() + ").");
        }
        if (destination.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "Le compte destinataire n'est pas actif (statut : " + destination.getStatus() + ").");
        }
        if (source.getBalance().compareTo(amount) < 0) {
            throw new BusinessRuleException("Solde insuffisant sur le compte source.");
        }

        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));

        Transfer transfer = new Transfer(source, destination, amount);
        transfer.assignReference(ReferenceGenerator.temporary());
        transferRepository.saveAndFlush(transfer);
        transfer.assignReference(ReferenceGenerator.format("TR", transfer.getId()));

        Transaction debit = new Transaction(source, TransactionType.TRANSFER_OUT, amount, request.description());
        debit.setTransfer(transfer);
        debit.assignReference(ReferenceGenerator.temporary());
        transactionRepository.saveAndFlush(debit);
        debit.assignReference(ReferenceGenerator.format("TX", debit.getId()));

        Transaction credit = new Transaction(destination, TransactionType.TRANSFER_IN, amount, request.description());
        credit.setTransfer(transfer);
        credit.assignReference(ReferenceGenerator.temporary());
        transactionRepository.saveAndFlush(credit);
        credit.assignReference(ReferenceGenerator.format("TX", credit.getId()));

        return new OperationResponse("Virement effectué avec succès.", transfer.getReference(),
                transfer.getStatus(),
                Map.of("sourceAccountId", sourceId,
                        "destinationAccountId", destinationId,
                        "amount", amount,
                        "sourceNewBalance", source.getBalance(),
                        "destinationNewBalance", destination.getBalance(),
                        "debitReference", debit.getReference(),
                        "creditReference", credit.getReference()));
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse findByReference(String reference) {
        Transfer transfer = transferRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Virement introuvable : " + reference));
        return TransferResponse.from(transfer);
    }
}