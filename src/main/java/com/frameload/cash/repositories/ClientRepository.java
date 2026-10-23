package com.frameload.cash.repositories;

import com.frameload.cash.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /**
     * Recherche un texte dans le numéro client, le prénom, le nom et l'e-mail (contient, sans tenir compte de la casse).
     */
    @Query("""
        select c from Client c
        where lower(c.clientNumber) like lower(concat('%', :term, '%'))
           or lower(c.firstname) like lower(concat('%', :term, '%'))
           or lower(c.lastname) like lower(concat('%', :term, '%'))
           or lower(c.email) like lower(concat('%', :term, '%'))
        order by c.id
        """)
    List<Client> search(@Param("term") String term);
}
