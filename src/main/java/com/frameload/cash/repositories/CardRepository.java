package com.frameload.cash.repositories;

import com.frameload.cash.entity.BankCard;
import java.util.List;

import com.frameload.cash.enums.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardRepository extends JpaRepository<BankCard, Long> {

    List<BankCard> findByAccountIdOrderByIdAsc(Long accountId);

    long countByStatus(CardStatus status);
}