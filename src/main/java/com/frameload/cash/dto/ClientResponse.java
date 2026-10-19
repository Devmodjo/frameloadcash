package com.frameload.cash.dto;

import com.frameload.cash.entity.Client;
import com.frameload.cash.enums.ClientStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Représentation d'un client exposée par l'API : l'objet interne {@link Client} n'est jamais renvoyé tel quel.
 */
public record ClientResponse(Long id, String clientNumber, String firstName, String lastName, String email,
                             String phone, String address, ClientStatus status, LocalDate createdAt) {

    public static ClientResponse from(Client client) {
        return new ClientResponse(client.getId(), client.getClientNumber(), client.getFirstname(),
                client.getLastname(), client.getEmail(), client.getPhone(), client.getAddress(),
                client.getStatus(), client.getCreatedAt());
    }
}