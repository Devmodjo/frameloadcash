package com.frameload.cash.services.impl;

import com.frameload.cash.dto.ClientRequest;
import com.frameload.cash.dto.ClientResponse;
import com.frameload.cash.entity.Client;
import com.frameload.cash.enums.ClientStatus;
import com.frameload.cash.exception.BusinessRuleException;
import com.frameload.cash.exception.ResourceNotFoundException;
import com.frameload.cash.repositories.ClientRepository;
import com.frameload.cash.services.ClientServices;
import com.frameload.cash.services.ReferenceGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
public class ClientServicesImpl implements ClientServices {


    private final ClientRepository clientRepository;

    public ClientServicesImpl(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    @Transactional
    public ClientResponse create(ClientRequest request) {

        if (clientRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessRuleException("Cette adresse e-mail est déjà utilisée par un autre client.");
        }

        Client client = new Client(request.firstName(), request.lastName(), request.email(),
                request.phone(), request.address());

        client.assignClientNumber(ReferenceGenerator.temporary());
        clientRepository.saveAndFlush(client);
        client.assignClientNumber(ReferenceGenerator.format("CL", client.getId()));
        return ClientResponse.from(client);
    }

    @Override
    public List<ClientResponse> findAll() {

        List<ClientResponse> list = new ArrayList<>();
        clientRepository.findAll().forEach(e -> list.add(new ClientResponse(
                e.getId(), e.getClientNumber(), e.getFirstname(), e.getLastname(),
                e.getEmail(), e.getPhone(), e.getAddress(), e.getStatus(), e.getCreatedAt()
        )));
        return list;
    }

    @Override
    public ClientResponse findById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + id));
        return ClientResponse.from(client);
    }

    @Override
    @Transactional
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + id));

        if (clientRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new BusinessRuleException("Cette adresse e-mail est déjà utilisée par un autre client.");
        }

        client.setFirstname(request.firstName());
        client.setLastname(request.lastName());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        client.setAddress(request.address());
        return ClientResponse.from(client);
    }

    @Override
    @Transactional
    public ClientResponse changeStatus(Long id, ClientStatus status) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + id));
        client.setStatus(status);
        return ClientResponse.from(client);
    }
}
