package com.frameload.cash.dto;

import com.frameload.cash.entity.BankCard;
import com.frameload.cash.enums.CardStatus;
import com.frameload.cash.enums.CardType;
import java.time.LocalDate;

/**
 * Représentation d'une carte fictive exposée par l'API : aucun numéro de carte, CVV ou PIN.
 */
public record CardResponse(Long id, String cardReference, Long accountId, CardType cardType,
                           CardStatus status, LocalDate expirationDate, LocalDate createdAt) {

    public static CardResponse from(BankCard card) {
        return new CardResponse(card.getId(), card.getCardReference(), card.getAccount().getId(),
                card.getCardType(), card.getStatus(), card.getExpirationDate(), card.getCreatedAt());
    }
}