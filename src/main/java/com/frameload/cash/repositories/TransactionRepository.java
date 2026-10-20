package com.frameload.cash.repositories;

import com.frameload.cash.entity.Transaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    long countByAccountId(Long accountId);

    List<Transaction> findByAccountIdOrderByIdDesc(Long accountId);
}