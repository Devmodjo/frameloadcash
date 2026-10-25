package com.frameload.cash.entity;

import com.frameload.cash.enums.CardStatus;
import com.frameload.cash.enums.CardType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Carte bancaire fictive rattachée à un compte. Elle ne contient volontairement aucun numéro de carte,
 * aucun CVV et aucun PIN : seulement une référence fictive, un type, un statut et une date d'expiration.
 */
@Entity
@Table(name = "bank_cards")
public class BankCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String cardReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private CardType cardType;

    @Column(nullable = false)
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private CardStatus status;

    @Column(nullable = false)
    private LocalDate createdAt;

    public BankCard() {
    }

    public BankCard(Account account, CardType cardType, LocalDate expirationDate) {
        this.account = account;
        this.cardType = cardType;
        this.expirationDate = expirationDate;
        this.status = CardStatus.ACTIVE;
        this.createdAt = LocalDate.now();
    }

    public void assignCardReference(String cardReference) {
        this.cardReference = cardReference;
    }

    public Long getId() {
        return id;
    }

    public String getCardReference() {
        return cardReference;
    }

    public Account getAccount() {
        return account;
    }

    public CardType getCardType() {
        return cardType;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }
}