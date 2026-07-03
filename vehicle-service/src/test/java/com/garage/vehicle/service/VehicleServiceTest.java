package com.garage.vehicle.service;

import com.garage.vehicle.dto.VehicleDto;
import com.garage.vehicle.entity.*;
import com.garage.vehicle.kafka.KafkaEvents;
import com.garage.vehicle.kafka.VehicleKafkaProducer;
import com.garage.vehicle.repository.ServiceRecordRepository;
import com.garage.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock private VehicleRepository vehicleRepository;
    @Mock private ServiceRecordRepository serviceRecordRepository;
    @Mock private VehicleKafkaProducer kafkaProducer;
    @Mock private IClientService clientService;

    @InjectMocks
    private VehicleService vehicleService;

    private Client sampleClient;
    private Vehicle sampleVehicle;

    @BeforeEach
    void setUp() {
        sampleClient = Client.builder()
                .id(1L).firstName("Alice").lastName("Martin")
                .email("alice@garage.com").phone("0600000001").build();

        sampleVehicle = Vehicle.builder()
                .id(10L).licensePlate("AB-123-CD").make("Renault").model("Clio")
                .year(2020).color("Red").status(VehicleStatus.ACTIVE)
                .mileage(50000).client(sampleClient).build();

        // Provide a no-op security context so getCurrentUsername() doesn't NPE
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("mechanic1");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    // ── createVehicle ─────────────────────────────────────────────────────────

    @Test
    void createVehicle_success_returnsResponse() {
        VehicleDto.CreateVehicleRequest req = VehicleDto.CreateVehicleRequest.builder()
                .licensePlate("AB-123-CD").make("Renault").model("Clio")
                .year(2020).clientEmail("alice@garage.com").vin("").build();

        when(vehicleRepository.existsByLicensePlate("AB-123-CD")).thenReturn(false);
        when(vehicleRepository.existsByVin(anyString())).thenReturn(false);
        when(clientService.tryFindClientEntityById(any())).thenReturn(Optional.empty());
        when(clientService.findOrCreateByEmail(any(), any(), eq("alice@garage.com"), any()))
                .thenReturn(sampleClient);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(sampleVehicle);
        when(serviceRecordRepository.countByVehicleId(10L)).thenReturn(0);
        doNothing().when(kafkaProducer).publishVehicleCreated(any());

        VehicleDto.VehicleResponse resp = vehicleService.createVehicle(req);

        assertThat(resp.getLicensePlate()).isEqualTo("AB-123-CD");
        assertThat(resp.getMake()).isEqualTo("Renault");
        verify(vehicleRepository).save(any());
    }

    @Test
    void createVehicle_duplicateLicensePlate_throws() {
        VehicleDto.CreateVehicleRequest req = VehicleDto.CreateVehicleRequest.builder()
                .licensePlate("AB-123-CD").make("Renault").model("Clio").year(2020).vin("").build();

        when(vehicleRepository.existsByLicensePlate("AB-123-CD")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.createVehicle(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("License plate already registered");
    }

    @Test
    void createVehicle_duplicateVin_throws() {
        VehicleDto.CreateVehicleRequest req = VehicleDto.CreateVehicleRequest.builder()
                .licensePlate("ZZ-999-ZZ").make("Peugeot").model("208").year(2022)
                .vin("VIN123").build();

        when(vehicleRepository.existsByLicensePlate("ZZ-999-ZZ")).thenReturn(false);
        when(vehicleRepository.existsByVin("VIN123")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.createVehicle(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VIN already registered");
    }

    // ── getVehicleById ────────────────────────────────────────────────────────

    @Test
    void getVehicleById_found_returnsResponse() {
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(sampleVehicle));
        when(serviceRecordRepository.countByVehicleId(10L)).thenReturn(2);

        VehicleDto.VehicleResponse resp = vehicleService.getVehicleById(10L);

        assertThat(resp.getId()).isEqualTo(10L);
        assertThat(resp.getRepairCount()).isEqualTo(2);
    }

    @Test
    void getVehicleById_notFound_throws() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.getVehicleById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Vehicle not found");
    }

    // ── getAllVehicles ─────────────────────────────────────────────────────────

    @Test
    void getAllVehicles_returnsMappedList() {
        when(vehicleRepository.findAll()).thenReturn(List.of(sampleVehicle));
        when(serviceRecordRepository.countByVehicleId(10L)).thenReturn(0);

        List<VehicleDto.VehicleResponse> result = vehicleService.getAllVehicles();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLicensePlate()).isEqualTo("AB-123-CD");
    }

    @Test
    void getVehiclesByClient_returnsFiltered() {
        when(vehicleRepository.findByClientId(1L)).thenReturn(List.of(sampleVehicle));
        when(serviceRecordRepository.countByVehicleId(10L)).thenReturn(0);

        List<VehicleDto.VehicleResponse> result = vehicleService.getVehiclesByClient(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getVehiclesByStatus_returnsFiltered() {
        when(vehicleRepository.findByStatus(VehicleStatus.ACTIVE)).thenReturn(List.of(sampleVehicle));
        when(serviceRecordRepository.countByVehicleId(10L)).thenReturn(0);

        List<VehicleDto.VehicleResponse> result = vehicleService.getVehiclesByStatus(VehicleStatus.ACTIVE);

        assertThat(result).hasSize(1);
    }

    // ── updateVehicle ─────────────────────────────────────────────────────────

    @Test
    void updateVehicle_success_updatesFields() {
        VehicleDto.UpdateVehicleRequest req = VehicleDto.UpdateVehicleRequest.builder()
                .make("Toyota").color("Blue").mileage(60000).status(VehicleStatus.IN_SERVICE).build();

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(sampleVehicle));
        when(vehicleRepository.save(any())).thenReturn(sampleVehicle);
        when(serviceRecordRepository.countByVehicleId(10L)).thenReturn(0);
        doNothing().when(kafkaProducer).publishVehicleUpdated(any());

        VehicleDto.VehicleResponse resp = vehicleService.updateVehicle(10L, req);

        assertThat(sampleVehicle.getMake()).isEqualTo("Toyota");
        assertThat(sampleVehicle.getColor()).isEqualTo("Blue");
        verify(kafkaProducer).publishVehicleUpdated(any());
    }

    @Test
    void updateVehicle_notFound_throws() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.updateVehicle(99L, VehicleDto.UpdateVehicleRequest.builder().build()))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── deleteVehicle ─────────────────────────────────────────────────────────

    @Test
    void deleteVehicle_success_callsDelete() {
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(sampleVehicle));
        doNothing().when(vehicleRepository).delete(sampleVehicle);
        doNothing().when(kafkaProducer).publishVehicleDeleted(any());

        vehicleService.deleteVehicle(10L);

        verify(vehicleRepository).delete(sampleVehicle);
        verify(kafkaProducer).publishVehicleDeleted(any());
    }

    @Test
    void deleteVehicle_notFound_throws() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.deleteVehicle(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── service records ───────────────────────────────────────────────────────

    @Test
    void createServiceRecord_byVehicleId_success() {
        VehicleDto.CreateServiceRequest req = VehicleDto.CreateServiceRequest.builder()
                .description("Oil change").serviceType(ServiceType.OIL_CHANGE)
                .serviceDate(LocalDate.now()).build();

        ServiceRecord saved = ServiceRecord.builder()
                .id(20L).vehicle(sampleVehicle).description("Oil change")
                .serviceType(ServiceType.OIL_CHANGE).serviceDate(LocalDate.now())
                .serviceStatus(ServiceStatus.SCHEDULED).build();

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(sampleVehicle));
        when(serviceRecordRepository.save(any())).thenReturn(saved);
        when(vehicleRepository.save(any())).thenReturn(sampleVehicle);
        doNothing().when(clientService).registerVisit(anyLong(), any());
        doNothing().when(kafkaProducer).publishServiceScheduled(any());

        VehicleDto.ServiceRecordResponse resp = vehicleService.createServiceRecord(10L, req);

        assertThat(resp.getDescription()).isEqualTo("Oil change");
        verify(serviceRecordRepository).save(any());
        verify(kafkaProducer).publishServiceScheduled(any());
    }

    @Test
    void getServiceRecordsByVehicle_returnsList() {
        ServiceRecord record = ServiceRecord.builder().id(20L).vehicle(sampleVehicle)
                .description("Brake check").serviceType(ServiceType.BRAKE_SERVICE)
                .serviceDate(LocalDate.now()).serviceStatus(ServiceStatus.SCHEDULED).build();

        when(serviceRecordRepository.findByVehicleId(10L)).thenReturn(List.of(record));

        List<VehicleDto.ServiceRecordResponse> result = vehicleService.getServiceRecordsByVehicle(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDescription()).isEqualTo("Brake check");
    }

    @Test
    void getAllServiceRecords_returnsList() {
        ServiceRecord record = ServiceRecord.builder().id(20L).vehicle(sampleVehicle)
                .description("General").serviceType(ServiceType.INSPECTION)
                .serviceDate(LocalDate.now()).serviceStatus(ServiceStatus.SCHEDULED).build();

        when(serviceRecordRepository.findAll()).thenReturn(List.of(record));

        List<VehicleDto.ServiceRecordResponse> result = vehicleService.getAllServiceRecords();

        assertThat(result).hasSize(1);
    }

    @Test
    void getServiceRecordById_found() {
        ServiceRecord record = ServiceRecord.builder().id(20L).vehicle(sampleVehicle)
                .description("Tyres").serviceType(ServiceType.TIRE_ROTATION)
                .serviceDate(LocalDate.now()).serviceStatus(ServiceStatus.SCHEDULED).build();

        when(serviceRecordRepository.findById(20L)).thenReturn(Optional.of(record));

        VehicleDto.ServiceRecordResponse resp = vehicleService.getServiceRecordById(20L);

        assertThat(resp.getId()).isEqualTo(20L);
    }

    @Test
    void getServiceRecordById_notFound_throws() {
        when(serviceRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.getServiceRecordById(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteServiceRecord_success() {
        ServiceRecord record = ServiceRecord.builder().id(20L).vehicle(sampleVehicle)
                .description("X").serviceType(ServiceType.OIL_CHANGE)
                .serviceDate(LocalDate.now()).serviceStatus(ServiceStatus.SCHEDULED).build();

        when(serviceRecordRepository.findById(20L)).thenReturn(Optional.of(record));
        doNothing().when(serviceRecordRepository).delete(record);

        vehicleService.deleteServiceRecord(10L, 20L);

        verify(serviceRecordRepository).delete(record);
    }

    @Test
    void deleteServiceRecord_wrongVehicle_throws() {
        ServiceRecord record = ServiceRecord.builder().id(20L).vehicle(sampleVehicle)
                .description("X").serviceType(ServiceType.OIL_CHANGE)
                .serviceDate(LocalDate.now()).serviceStatus(ServiceStatus.SCHEDULED).build();

        when(serviceRecordRepository.findById(20L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> vehicleService.deleteServiceRecord(99L, 20L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to vehicle");
    }

    @Test
    void updateServiceRecord_completed_setsVehicleRepaired() {
        ServiceRecord record = ServiceRecord.builder().id(20L).vehicle(sampleVehicle)
                .description("Oil change").serviceType(ServiceType.OIL_CHANGE)
                .serviceDate(LocalDate.now()).serviceStatus(ServiceStatus.SCHEDULED).build();

        VehicleDto.UpdateServiceRequest req = VehicleDto.UpdateServiceRequest.builder()
                .serviceStatus(ServiceStatus.COMPLETED).completedDate(LocalDate.now())
                .cost(BigDecimal.valueOf(150)).build();

        when(serviceRecordRepository.findById(20L)).thenReturn(Optional.of(record));
        when(serviceRecordRepository.save(any())).thenReturn(record);
        when(vehicleRepository.save(any())).thenReturn(sampleVehicle);

        vehicleService.updateServiceRecord(10L, 20L, req);

        assertThat(sampleVehicle.getStatus()).isEqualTo(VehicleStatus.REPAIRED);
        verify(vehicleRepository).save(sampleVehicle);
    }

    @Test
    void getServiceRecordsByClient_returnsList() {
        ServiceRecord record = ServiceRecord.builder().id(20L).vehicle(sampleVehicle)
                .description("Check").serviceType(ServiceType.INSPECTION)
                .serviceDate(LocalDate.now()).serviceStatus(ServiceStatus.SCHEDULED).build();

        when(serviceRecordRepository.findByVehicleClientId(1L)).thenReturn(List.of(record));

        List<VehicleDto.ServiceRecordResponse> result = vehicleService.getServiceRecordsByClient(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getServiceRecordsByMechanic_returnsList() {
        ServiceRecord record = ServiceRecord.builder().id(20L).vehicle(sampleVehicle)
                .description("Check").serviceType(ServiceType.INSPECTION)
                .serviceDate(LocalDate.now()).serviceStatus(ServiceStatus.SCHEDULED)
                .mechanicUsername("mechanic1").build();

        when(serviceRecordRepository.findByMechanicUsername("mechanic1")).thenReturn(List.of(record));

        List<VehicleDto.ServiceRecordResponse> result = vehicleService.getServiceRecordsByMechanic("mechanic1");

        assertThat(result).hasSize(1);
    }
}
