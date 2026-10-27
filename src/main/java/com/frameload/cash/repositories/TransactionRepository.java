package com.frameload.cash.repositories;

import com.frameload.cash.dto.AccountActivity;
import com.frameload.cash.dto.TransactionTypeCount;
import com.frameload.cash.entity.Transaction;
import com.frameload.cash.enums.TransactionStatus;
import com.frameload.cash.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Accès aux transactions. JpaSpecificationExecutor permet de combiner des filtres facultatifs pour l'historique ;
 * les requêtes JPQL calculent les agrégats (SUM, COUNT, GROUP BY) en base, sur une période [startAt, endAt[.
 */
@Repository
public interface TransactionRepository
        extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByReference(String reference);

    long countByAccountId(Long accountId);

    long countByStatus(TransactionStatus status);

    List<Transaction> findByAccountIdOrderByIdDesc(Long accountId);

    List<Transaction> findByTransferReference(String reference);

    /**
     * Somme des montants d'un type de transaction sur la période (null s'il n'y en a aucune).
     */
    @Query("""
            select sum(t.amount) from Transaction t
            where t.type = :transactionType and t.status = :transactionStatus
              and t.createdAt >= :startAt and t.createdAt < :endAt
            """)
    BigDecimal sumAmount(@Param("transactionType") TransactionType transactionType,
                         @Param("transactionStatus") TransactionStatus transactionStatus,
                         @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    /**
     * Nombre de transactions par type sur la période (les types absents ne sont pas retournés).
     */
    @Query("""
            select new com.frameload.cash.dto.TransactionTypeCount(t.type, count(t))
            from Transaction t
            where t.status = :transactionStatus and t.createdAt >= :startAt and t.createdAt < :endAt
            group by t.type
            """)
    List<TransactionTypeCount> countByType(@Param("transactionStatus") TransactionStatus transactionStatus,
                                           @Param("startAt") LocalDateTime startAt,
                                           @Param("endAt") LocalDateTime endAt);

    /**
     * Nombre d'opérations par compte sur la période, du plus actif au moins actif.
     */
    @Query("""
            select new com.frameload.cash.dto.AccountActivity(a.id, a.accountNumber, count(t))
            from Transaction t join t.account a
            where t.status = :transactionStatus and t.createdAt >= :startAt and t.createdAt < :endAt
            group by a.id, a.accountNumber
            order by count(t) desc, a.id asc
            """)
    List<AccountActivity> countByAccount(@Param("transactionStatus") TransactionStatus transactionStatus,
                                         @Param("startAt") LocalDateTime startAt,
                                         @Param("endAt") LocalDateTime endAt);
}