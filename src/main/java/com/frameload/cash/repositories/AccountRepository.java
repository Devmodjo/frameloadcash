package com.frameload.cash.repositories;

import com.frameload.cash.entity.Account;
import java.util.List;
import java.util.Optional;

import com.frameload.cash.enums.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByClientId(Long clientId);

    /**
     * Charge le compte en le verrouillant (SELECT ... FOR UPDATE) jusqu'à la fin de la transaction.
     * Deux opérations simultanées sur le même compte passent l'une après l'autre : le solde est toujours
     * calculé à partir de sa dernière valeur (cohérence des soldes, cahier des charges §2.3).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(AccountStatus status);
}