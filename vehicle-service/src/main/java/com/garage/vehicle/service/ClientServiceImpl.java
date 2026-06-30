package com.garage.vehicle.service;

import com.garage.vehicle.dto.ClientDto;
import com.garage.vehicle.entity.Client;
import com.garage.vehicle.repository.ClientRepository;
import com.garage.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements IClientService {

    private final ClientRepository clientRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    public List<ClientDto.ClientResponse> getAllClients() {
        return clientRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public ClientDto.ClientResponse getClientById(Long id) {
        return mapToResponse(findById(id));
    }

    @Override
    @Transactional
    public ClientDto.ClientResponse createClient(ClientDto.CreateRequest request) {
        if (clientRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }
        Client client = Client.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .build();
        return mapToResponse(clientRepository.save(client));
    }

    @Override
    @Transactional
    public ClientDto.ClientResponse updateClient(Long id, ClientDto.UpdateRequest request) {
        Client client = findById(id);
        if (request.getFirstName() != null) client.setFirstName(request.getFirstName());
        if (request.getLastName() != null) client.setLastName(request.getLastName());
        if (request.getEmail() != null) client.setEmail(request.getEmail());
        if (request.getPhone() != null) client.setPhone(request.getPhone());
        return mapToResponse(clientRepository.save(client));
    }

    @Override
    @Transactional
    public void deleteClient(Long id) {
        clientRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Client findOrCreateByEmail(String firstName, String lastName, String email, String phone) {
        if (email != null) {
            var existing = clientRepository.findByEmail(email);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        Client client = Client.builder()
                .firstName(firstName != null ? firstName : "")
                .lastName(lastName != null ? lastName : "")
                .email(email)
                .phone(phone)
                .build();
        Client saved = clientRepository.save(client);
        log.info("Auto-created client {} ({})", saved.getId(), saved.getEmail());
        return saved;
    }

    @Override
    public Client getClientEntityById(Long id) {
        return findById(id);
    }

    @Override
    @Transactional
    public void registerVisit(Long clientId, LocalDateTime visitDate) {
        clientRepository.findById(clientId).ifPresent(client -> {
            client.setRepairCount(client.getRepairCount() + 1);
            client.setLastVisit(visitDate != null ? visitDate : LocalDateTime.now());
            clientRepository.save(client);
        });
    }

    @Override
    @Transactional
    public void addTotalSpent(Long clientId, BigDecimal amount) {
        clientRepository.findById(clientId).ifPresent(client -> {
            BigDecimal current = client.getTotalSpent() != null ? client.getTotalSpent() : BigDecimal.ZERO;
            client.setTotalSpent(current.add(amount));
            clientRepository.save(client);
        });
    }

    private Client findById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Client not found: " + id));
    }

    private ClientDto.ClientResponse mapToResponse(Client client) {
        return ClientDto.ClientResponse.builder()
                .id(client.getId())
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .email(client.getEmail())
                .phone(client.getPhone())
                .vehicleCount(vehicleRepository.countByClientId(client.getId()))
                .repairCount(client.getRepairCount())
                .totalSpent(client.getTotalSpent() != null ? client.getTotalSpent() : BigDecimal.ZERO)
                .lastVisit(client.getLastVisit() != null ? client.getLastVisit().toString() : null)
                .createdAt(client.getCreatedAt() != null ? client.getCreatedAt().toString() : null)
                .build();
    }
}
