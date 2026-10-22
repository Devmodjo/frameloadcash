package com.frameload.cash.repositories;

import com.frameload.cash.entity.Transaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Accès aux transactions. JpaSpecificationExecutor permet de combiner des filtres facultatifs
 * (compte, type, période) sans écrire une requête par combinaison.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByReference(String reference);

    long countByAccountId(Long accountId);

    List<Transaction> findByAccountIdOrderByIdDesc(Long accountId);

    List<Transaction> findByTransferReference(String reference);
}