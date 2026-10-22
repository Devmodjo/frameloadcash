package com.frameload.cash.services.impl;

import com.frameload.cash.dto.TransactionResponse;
import com.frameload.cash.entity.Transaction;
import com.frameload.cash.enums.TransactionType;
import com.frameload.cash.exception.InvalidRequestException;
import com.frameload.cash.exception.ResourceNotFoundException;
import com.frameload.cash.repositories.AccountRepository;
import com.frameload.cash.repositories.TransactionRepository;
import com.frameload.cash.services.TransactionServices;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultation de l'historique (cahier des charges, section 12). Les bornes de période sont INCLUSIVES :
 * {@code startDate} commence à 00:00:00 et {@code endDate} couvre toute la journée (implémenté par
 * « strictement avant le lendemain de endDate »). Les transactions sont triées de la plus récente à la plus ancienne.
 * Les réponses sont construites dans la transaction, car le compte et le virement sont chargés à la demande (LAZY).
 */
@Service
public class TransactionServicesImpl implements TransactionServices {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionServicesImpl(TransactionRepository transactionRepository,
                                   AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> findAll(Long accountId, TransactionType type,
                                             LocalDate startDate, LocalDate endDate) {
        if (accountId != null && !accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Compte introuvable : " + accountId);
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidRequestException("La date de début doit précéder ou égaler la date de fin.");
        }

        LocalDateTime from = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime toExclusive = endDate == null ? null : endDate.plusDays(1).atStartOfDay();

        Specification<Transaction> filters = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (accountId != null) {
                predicates.add(builder.equal(root.get("account").get("id"), accountId));
            }
            if (type != null) {
                predicates.add(builder.equal(root.get("type"), type));
            }
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (toExclusive != null) {
                predicates.add(builder.lessThan(root.get("createdAt"), toExclusive));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(filters, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
                .stream().map(TransactionResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse findByReference(String reference) {
        Transaction transaction = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction introuvable : " + reference));
        return TransactionResponse.from(transaction);
    }
}