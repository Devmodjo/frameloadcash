package com.frameload.cash.services;

import com.frameload.cash.dto.ClientRequest;
import com.frameload.cash.dto.ClientResponse;
import com.frameload.cash.enums.ClientStatus;

import java.util.List;


public interface ClientServices {

    ClientResponse create(ClientRequest request);

    List<ClientResponse> findAll();

    ClientResponse findById(Long id);

    ClientResponse update(Long id, ClientRequest request);

    ClientResponse changeStatus(Long id, ClientStatus status);

    List<ClientResponse> search(String term);
}
