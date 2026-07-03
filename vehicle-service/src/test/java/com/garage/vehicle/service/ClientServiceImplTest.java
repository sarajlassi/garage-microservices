package com.garage.vehicle.service;

import com.garage.vehicle.dto.ClientDto;
import com.garage.vehicle.entity.Client;
import com.garage.vehicle.repository.ClientRepository;
import com.garage.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock private ClientRepository clientRepository;
    @Mock private VehicleRepository vehicleRepository;

    @InjectMocks
    private ClientServiceImpl clientService;

    private Client sampleClient;

    @BeforeEach
    void setUp() {
        sampleClient = Client.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Martin")
                .email("alice@garage.com")
                .phone("0600000001")
                .repairCount(2)
                .totalSpent(BigDecimal.valueOf(500))
                .build();
    }

    @Test
    void getAllClients_returnsMappedList() {
        when(clientRepository.findAll()).thenReturn(List.of(sampleClient));
        when(vehicleRepository.countByClientId(1L)).thenReturn(3);

        List<ClientDto.ClientResponse> result = clientService.getAllClients();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("Alice");
        assertThat(result.get(0).getVehicleCount()).isEqualTo(3);
    }

    @Test
    void getClientById_found_returnsMapped() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(sampleClient));
        when(vehicleRepository.countByClientId(1L)).thenReturn(1);

        ClientDto.ClientResponse resp = clientService.getClientById(1L);

        assertThat(resp.getEmail()).isEqualTo("alice@garage.com");
    }

    @Test
    void getClientById_notFound_throws() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getClientById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Client not found");
    }

    @Test
    void createClient_success() {
        ClientDto.CreateRequest req = ClientDto.CreateRequest.builder()
                .firstName("Bob").lastName("Dupont")
                .email("bob@garage.com").phone("0700000000").build();

        when(clientRepository.existsByEmail("bob@garage.com")).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenReturn(
                Client.builder().id(2L).firstName("Bob").lastName("Dupont")
                        .email("bob@garage.com").phone("0700000000").build());
        when(vehicleRepository.countByClientId(2L)).thenReturn(0);

        ClientDto.ClientResponse resp = clientService.createClient(req);

        assertThat(resp.getEmail()).isEqualTo("bob@garage.com");
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void createClient_emailExists_throws() {
        ClientDto.CreateRequest req = ClientDto.CreateRequest.builder()
                .email("alice@garage.com").firstName("X").lastName("Y").build();

        when(clientRepository.existsByEmail("alice@garage.com")).thenReturn(true);

        assertThatThrownBy(() -> clientService.createClient(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void updateClient_updatesFieldsAndSaves() {
        ClientDto.UpdateRequest req = ClientDto.UpdateRequest.builder()
                .firstName("Alicia").phone("0600000099").build();

        when(clientRepository.findById(1L)).thenReturn(Optional.of(sampleClient));
        when(clientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.countByClientId(anyLong())).thenReturn(0);

        ClientDto.ClientResponse resp = clientService.updateClient(1L, req);

        assertThat(resp.getFirstName()).isEqualTo("Alicia");
        assertThat(resp.getPhone()).isEqualTo("0600000099");
    }

    @Test
    void updateClient_notFound_throws() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.updateClient(99L, ClientDto.UpdateRequest.builder().build()))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteClient_callsRepository() {
        doNothing().when(clientRepository).deleteById(1L);

        clientService.deleteClient(1L);

        verify(clientRepository).deleteById(1L);
    }

    @Test
    void findOrCreateByEmail_existingEmail_returnsExisting() {
        when(clientRepository.findByEmail("alice@garage.com")).thenReturn(Optional.of(sampleClient));

        Client result = clientService.findOrCreateByEmail("Alice", "Martin", "alice@garage.com", null);

        assertThat(result.getId()).isEqualTo(1L);
        verify(clientRepository, never()).save(any());
    }

    @Test
    void findOrCreateByEmail_newEmail_createsAndSaves() {
        when(clientRepository.findByEmail("new@garage.com")).thenReturn(Optional.empty());
        Client saved = Client.builder().id(5L).firstName("New").lastName("User")
                .email("new@garage.com").build();
        when(clientRepository.save(any())).thenReturn(saved);

        Client result = clientService.findOrCreateByEmail("New", "User", "new@garage.com", null);

        assertThat(result.getId()).isEqualTo(5L);
        verify(clientRepository).save(any());
    }

    @Test
    void findOrCreateByEmail_nullEmail_createsWithNullEmail() {
        Client saved = Client.builder().id(6L).firstName("X").lastName("Y").build();
        when(clientRepository.save(any())).thenReturn(saved);

        Client result = clientService.findOrCreateByEmail("X", "Y", null, null);

        assertThat(result.getId()).isEqualTo(6L);
    }

    @Test
    void findOrCreateByEmail_nullNames_usesEmptyStrings() {
        when(clientRepository.findByEmail("test@x.com")).thenReturn(Optional.empty());
        when(clientRepository.save(any())).thenAnswer(inv -> {
            Client c = inv.getArgument(0);
            assertThat(c.getFirstName()).isEqualTo("");
            assertThat(c.getLastName()).isEqualTo("");
            return c;
        });

        clientService.findOrCreateByEmail(null, null, "test@x.com", null);
    }

    @Test
    void getClientEntityById_returnsEntity() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(sampleClient));

        Client result = clientService.getClientEntityById(1L);

        assertThat(result).isEqualTo(sampleClient);
    }

    @Test
    void tryFindClientEntityById_found_returnsPresent() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(sampleClient));

        Optional<Client> result = clientService.tryFindClientEntityById(1L);

        assertThat(result).isPresent();
    }

    @Test
    void tryFindClientEntityById_notFound_returnsEmpty() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Client> result = clientService.tryFindClientEntityById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void registerVisit_incrementsRepairCountAndSetsVisit() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(sampleClient));
        when(clientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clientService.registerVisit(1L, LocalDateTime.now());

        assertThat(sampleClient.getRepairCount()).isEqualTo(3);
        assertThat(sampleClient.getLastVisit()).isNotNull();
    }

    @Test
    void registerVisit_clientNotFound_doesNothing() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        clientService.registerVisit(99L, LocalDateTime.now());

        verify(clientRepository, never()).save(any());
    }

    @Test
    void addTotalSpent_addsToCurrent() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(sampleClient));
        when(clientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clientService.addTotalSpent(1L, BigDecimal.valueOf(200));

        assertThat(sampleClient.getTotalSpent()).isEqualByComparingTo("700");
    }

    @Test
    void addTotalSpent_nullCurrentSpent_treatsAsZero() {
        sampleClient.setTotalSpent(null);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(sampleClient));
        when(clientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clientService.addTotalSpent(1L, BigDecimal.valueOf(100));

        assertThat(sampleClient.getTotalSpent()).isEqualByComparingTo("100");
    }
}
