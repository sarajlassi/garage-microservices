package com.garage.vehicle.service;

import com.garage.vehicle.dto.ClientDto;
import com.garage.vehicle.entity.Client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IClientService {

    List<ClientDto.ClientResponse> getAllClients();

    ClientDto.ClientResponse getClientById(Long id);

    ClientDto.ClientResponse createClient(ClientDto.CreateRequest request);

    ClientDto.ClientResponse updateClient(Long id, ClientDto.UpdateRequest request);

    void deleteClient(Long id);

    Client findOrCreateByEmail(String firstName, String lastName, String email, String phone);

    Client getClientEntityById(Long id);

    Optional<Client> tryFindClientEntityById(Long id);

    void registerVisit(Long clientId, LocalDateTime visitDate);

    void addTotalSpent(Long clientId, BigDecimal amount);
}
