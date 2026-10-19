package com.frameload.cash.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.frameload.cash.dto.ClientRequest;
import com.frameload.cash.dto.ClientResponse;
import com.frameload.cash.enums.ClientStatus;
import com.frameload.cash.exception.BusinessRuleException;
import com.frameload.cash.exception.ResourceNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Teste les règles métier des clients avec la vraie base de test (H2 en mémoire).
 * Chaque test utilise un e-mail unique pour ne pas dépendre des autres.
 */
@SpringBootTest
@ActiveProfiles("test")
class ClientServiceImplTest {

    @Autowired
    private ClientServices clientServices;

    private String uniqueEmail() {
        return "awa-" + UUID.randomUUID() + "@example.test";
    }

    private ClientRequest request(String email) {
        return new ClientRequest("Awa", "Nkoulou", email, "+237 670 00 00 01", "Yaoundé");
    }

    @Test
    void create_assignsClientNumberAndActiveStatus() {
        ClientResponse client = clientServices.create(request(uniqueEmail()));

        assertThat(client.clientNumber()).isEqualTo(String.format("CL-%06d", client.id()));
        assertThat(client.status()).isEqualTo(ClientStatus.ACTIVE);
        assertThat(client.createdAt()).isNotNull();
    }

    @Test
    void create_rejectsDuplicateEmailIgnoringCase() {
        String email = uniqueEmail();
        clientServices.create(request(email));

        assertThatThrownBy(() -> clientServices.create(request(email.toUpperCase())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("e-mail");
    }

    @Test
    void findById_returnsTheClient() {
        ClientResponse created = clientServices.create(request(uniqueEmail()));

        assertThat(clientServices.findById(created.id()).email()).isEqualTo(created.email());
    }

    @Test
    void findById_withUnknownId_throwsNotFound() {
        assertThatThrownBy(() -> clientServices.findById(999_999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_changesDetailsButKeepsClientNumber() {
        ClientResponse created = clientServices.create(request(uniqueEmail()));

        ClientResponse updated = clientServices.update(created.id(),
                new ClientRequest("Awa", "Mballa", created.email(), "+237 670 00 00 01", "Douala"));

        assertThat(updated.lastName()).isEqualTo("Mballa");
        assertThat(updated.clientNumber()).isEqualTo(created.clientNumber());
    }

    @Test
    void update_rejectsEmailOfAnotherClient() {
        ClientResponse first = clientServices.create(request(uniqueEmail()));
        ClientResponse second = clientServices.create(request(uniqueEmail()));

        assertThatThrownBy(() -> clientServices.update(second.id(), request(first.email())))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void changeStatus_deactivatesTheClientWithoutDeletingIt() {
        ClientResponse created = clientServices.create(request(uniqueEmail()));

        clientServices.changeStatus(created.id(), ClientStatus.INACTIVE);

        assertThat(clientServices.findById(created.id()).status()).isEqualTo(ClientStatus.INACTIVE);
    }

    @Test
    void findAll_containsTheCreatedClient() {
        ClientResponse created = clientServices.create(request(uniqueEmail()));

        assertThat(clientServices.findAll()).extracting(ClientResponse::id).contains(created.id());
    }
}