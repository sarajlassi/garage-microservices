package com.garage.auth.service;

import com.garage.auth.dto.ClientDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface IClientService {

    List<ClientDto.ClientResponse> getAllClients();

    ClientDto.ClientResponse getClientById(Long id);

    ClientDto.ClientResponse createClient(ClientDto.CreateRequest request);

    ClientDto.ClientResponse updateClient(Long id, ClientDto.UpdateRequest request);

    void deleteClient(Long id);

    void incrementVehicleCount(Long clientId);

    void incrementRepairCount(Long clientId, LocalDateTime visitDate);

    void addTotalSpent(Long clientId, BigDecimal amount);
}
