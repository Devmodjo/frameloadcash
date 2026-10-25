package com.frameload.cash.services.impl;

import com.frameload.cash.dto.CardRequest;
import com.frameload.cash.dto.CardResponse;
import com.frameload.cash.entity.Account;
import com.frameload.cash.entity.BankCard;
import com.frameload.cash.enums.CardStatus;
import com.frameload.cash.exception.BusinessRuleException;
import com.frameload.cash.exception.ResourceNotFoundException;
import com.frameload.cash.repositories.AccountRepository;
import com.frameload.cash.repositories.CardRepository;
import com.frameload.cash.services.CardServices;
import com.frameload.cash.services.ReferenceGenerator;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion des cartes fictives (cahier des charges, section 8). Aucune transaction n'est exécutée sur une carte.
 */
@Service
public class CardServicesImpl implements CardServices {

    private static final int VALIDITY_YEARS = 3;

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;

    public CardServicesImpl(CardRepository cardRepository, AccountRepository accountRepository) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public CardResponse create(Long accountId, CardRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable : " + accountId));

        BankCard card = new BankCard(account, request.cardType(), LocalDate.now().plusYears(VALIDITY_YEARS));
        card.assignCardReference(ReferenceGenerator.temporary());
        cardRepository.saveAndFlush(card);
        card.assignCardReference(ReferenceGenerator.format("CD", card.getId()));
        return CardResponse.from(card);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> findByAccountId(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Compte introuvable : " + accountId);
        }
        return cardRepository.findByAccountIdOrderByIdAsc(accountId).stream().map(CardResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse findById(Long id) {
        BankCard card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carte introuvable : " + id));
        return CardResponse.from(card);
    }

    /**
     * Bloque, réactive ou marque une carte comme expirée. Une carte expirée est définitive.
     */
    @Override
    @Transactional
    public CardResponse changeStatus(Long id, CardStatus status) {
        BankCard card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carte introuvable : " + id));

        if (card.getStatus() == CardStatus.EXPIRED && status != CardStatus.EXPIRED) {
            throw new BusinessRuleException("CARD_EXPIRED", "Une carte expirée ne peut plus être modifiée.");
        }

        card.setStatus(status);
        return CardResponse.from(card);
    }
}