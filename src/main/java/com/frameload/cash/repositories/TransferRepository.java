package com.frameload.cash.repositories;

import com.frameload.cash.entity.Transfer;
import com.frameload.cash.enums.TransactionStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByReference(String reference);

    long countByStatus(TransactionStatus status);

    @Query("""
            select count(tr) from Transfer tr
            where tr.status = :transferStatus and tr.createdAt >= :startAt and tr.createdAt < :endAt
            """)
    long countInPeriod(@Param("transferStatus") TransactionStatus transferStatus,
                       @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);
}