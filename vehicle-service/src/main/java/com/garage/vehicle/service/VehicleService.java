package com.garage.vehicle.service;

import com.garage.vehicle.dto.VehicleDto;
import com.garage.vehicle.entity.*;
import com.garage.vehicle.kafka.KafkaEvents;
import com.garage.vehicle.kafka.VehicleKafkaProducer;
import com.garage.vehicle.repository.ServiceRecordRepository;
import com.garage.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;
    private final VehicleKafkaProducer kafkaProducer;
    private final IClientService clientService;

    @Transactional
    public VehicleDto.VehicleResponse createVehicle(VehicleDto.CreateVehicleRequest request) {
        if (vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new IllegalArgumentException("License plate already registered: " + request.getLicensePlate());
        }
        if (!request.getVin().isBlank() && vehicleRepository.existsByVin(request.getVin())) {
            throw new IllegalArgumentException("VIN already registered: " + request.getVin());
        }

        Client client = resolveClient(request.getClientId(), request.getClientFirstName(),
                request.getClientLastName(), request.getClientEmail(), request.getClientPhone());

        String username = getCurrentUsername();

        Vehicle vehicle = Vehicle.builder()
                .licensePlate(request.getLicensePlate().toUpperCase())
                .make(request.getMake())
                .model(request.getModel())
                .year(request.getYear())
                .color(request.getColor())
              //  .vin(request.getVin())
                .client(client)
                .createdByUsername(username)
                .status(VehicleStatus.ACTIVE)
                .mileage(request.getMileage())
                .notes(request.getNotes())
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle created: {} for client: {}", saved.getLicensePlate(), client.getId());

        kafkaProducer.publishVehicleCreated(KafkaEvents.VehicleCreatedEvent.builder()
                .vehicleId(saved.getId())
                .licensePlate(saved.getLicensePlate())
                .make(saved.getMake())
                .model(saved.getModel())
                .year(saved.getYear())
                .clientId(client.getId())
                .clientFirstName(client.getFirstName())
                .clientLastName(client.getLastName())
                .clientEmail(client.getEmail())
                .clientPhone(client.getPhone())
                .createdAt(LocalDateTime.now())
                .build());

        return mapToVehicleResponse(saved);
    }

    public VehicleDto.VehicleResponse getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .map(this::mapToVehicleResponse)
                .orElseThrow(() -> new NoSuchElementException("Vehicle not found: " + id));
    }

    public List<VehicleDto.VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(this::mapToVehicleResponse)
                .toList();
    }

    public List<VehicleDto.VehicleResponse> getVehiclesByClient(Long clientId) {
        return vehicleRepository.findByClientId(clientId).stream()
                .map(this::mapToVehicleResponse)
                .toList();
    }

    public List<VehicleDto.VehicleResponse> getVehiclesByStatus(VehicleStatus status) {
        return vehicleRepository.findByStatus(status).stream()
                .map(this::mapToVehicleResponse)
                .toList();
    }

    @Transactional
    public VehicleDto.VehicleResponse updateVehicle(Long id, VehicleDto.UpdateVehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Vehicle not found: " + id));

        VehicleStatus oldStatus = vehicle.getStatus();

        if (request.getMake() != null) vehicle.setMake(request.getMake());
        if (request.getModel() != null) vehicle.setModel(request.getModel());
        if (request.getColor() != null) vehicle.setColor(request.getColor());
        if (request.getMileage() != null) vehicle.setMileage(request.getMileage());
        if (request.getStatus() != null) vehicle.setStatus(request.getStatus());
        if (request.getNotes() != null) vehicle.setNotes(request.getNotes());

        Vehicle updated = vehicleRepository.save(vehicle);

        kafkaProducer.publishVehicleUpdated(KafkaEvents.VehicleUpdatedEvent.builder()
                .vehicleId(updated.getId())
                .licensePlate(updated.getLicensePlate())
                .oldStatus(oldStatus)
                .newStatus(updated.getStatus())
                .updatedAt(LocalDateTime.now())
                .build());

        return mapToVehicleResponse(updated);
    }

    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Vehicle not found: " + id));

        vehicleRepository.delete(vehicle);
        log.info("Vehicle deleted: {}", vehicle.getLicensePlate());

        kafkaProducer.publishVehicleDeleted(KafkaEvents.VehicleDeletedEvent.builder()
                .vehicleId(vehicle.getId())
                .licensePlate(vehicle.getLicensePlate())
                .clientId(vehicle.getClient().getId())
                .deletedAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public VehicleDto.ServiceRecordResponse createServiceRecord(
            Long vehicleId, VehicleDto.CreateServiceRequest request) {

        Vehicle vehicle = resolveOrCreateVehicle(vehicleId, request);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String mechanicUsername = auth != null ? auth.getName() : "unknown";

        ServiceRecord record = ServiceRecord.builder()
                .vehicle(vehicle)
                .description(request.getDescription())
                .serviceType(request.getServiceType())
                .serviceDate(request.getServiceDate())
                .scheduledDate(request.getScheduledDate())
                .serviceStatus(ServiceStatus.SCHEDULED)
                .mechanicUsername(mechanicUsername)
                .cost(request.getCost())
                .mileageAtService(request.getMileageAtService())
                .notes(request.getNotes())
                .build();

        ServiceRecord saved = serviceRecordRepository.save(record);

        // Update vehicle status to IN_SERVICE
        vehicle.setStatus(VehicleStatus.IN_SERVICE);
        vehicleRepository.save(vehicle);

        clientService.registerVisit(vehicle.getClient().getId(), LocalDateTime.now());

        kafkaProducer.publishServiceScheduled(KafkaEvents.ServiceScheduledEvent.builder()
                .serviceRecordId(saved.getId())
                .vehicleId(vehicle.getId())
                .licensePlate(vehicle.getLicensePlate())
                .clientId(vehicle.getClient().getId())
                .description(saved.getDescription())
                .serviceType(saved.getServiceType())
                .scheduledDate(saved.getScheduledDate())
                .mechanicUsername(mechanicUsername)
                .estimatedCost(saved.getCost())
                .scheduledAt(LocalDateTime.now())
                .build());

        return mapToServiceResponse(saved);
    }

    public List<VehicleDto.ServiceRecordResponse> getServiceRecordsByVehicle(Long vehicleId) {
        return serviceRecordRepository.findByVehicleId(vehicleId).stream()
                .map(this::mapToServiceResponse)
                .toList();
    }

    public List<VehicleDto.ServiceRecordResponse> getAllServiceRecords() {
        return serviceRecordRepository.findAll().stream()
                .map(this::mapToServiceResponse)
                .toList();
    }

    public VehicleDto.ServiceRecordResponse getServiceRecordById(Long id) {
        return serviceRecordRepository.findById(id)
                .map(this::mapToServiceResponse)
                .orElseThrow(() -> new NoSuchElementException("Service record not found: " + id));
    }

    public List<VehicleDto.ServiceRecordResponse> getServiceRecordsByClient(Long clientId) {
        return serviceRecordRepository.findByVehicleClientId(clientId).stream()
                .map(this::mapToServiceResponse)
                .toList();
    }

    public List<VehicleDto.ServiceRecordResponse> getServiceRecordsByMechanic(String mechanicUsername) {
        return serviceRecordRepository.findByMechanicUsername(mechanicUsername).stream()
                .map(this::mapToServiceResponse)
                .toList();
    }

    @Transactional
    public void deleteServiceRecord(Long vehicleId, Long id) {
        ServiceRecord record = serviceRecordRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Service record not found: " + id));
        if (vehicleId != null && !record.getVehicle().getId().equals(vehicleId)) {
            throw new IllegalArgumentException("Service record does not belong to vehicle " + vehicleId);
        }
        serviceRecordRepository.delete(record);
        log.info("Service record {} deleted", id);
    }

    @Transactional
    public VehicleDto.ServiceRecordResponse updateServiceRecord(
            Long vehicleId, Long recordId, VehicleDto.UpdateServiceRequest request) {

        ServiceRecord record = serviceRecordRepository.findById(recordId)
                .orElseThrow(() -> new NoSuchElementException("Service record not found: " + recordId));

        if (vehicleId != null && !record.getVehicle().getId().equals(vehicleId)) {
            throw new IllegalArgumentException("Service record does not belong to vehicle " + vehicleId);
        }

        if (request.getDescription() != null) record.setDescription(request.getDescription());
        if (request.getServiceStatus() != null) record.setServiceStatus(request.getServiceStatus());
        if (request.getCompletedDate() != null) record.setCompletedDate(request.getCompletedDate());
        if (request.getCost() != null) record.setCost(request.getCost());
        if (request.getMileageAtService() != null) record.setMileageAtService(request.getMileageAtService());
        if (request.getNotes() != null) record.setNotes(request.getNotes());

        // If completed, update vehicle status and last service date
        if (request.getServiceStatus() == ServiceStatus.COMPLETED) {
            Vehicle vehicle = record.getVehicle();
            vehicle.setStatus(VehicleStatus.REPAIRED);
            vehicle.setLastServiceDate(request.getCompletedDate() != null
                    ? request.getCompletedDate() : java.time.LocalDate.now());
            if (request.getMileageAtService() != null) {
                vehicle.setMileage(request.getMileageAtService());
            }
            vehicleRepository.save(vehicle);
        }

        return mapToServiceResponse(serviceRecordRepository.save(record));
    }

    private Vehicle resolveOrCreateVehicle(Long vehicleId, VehicleDto.CreateServiceRequest request) {
        // 1. Try by explicit ID
        if (vehicleId != null) {
            var byId = vehicleRepository.findById(vehicleId);
            if (byId.isPresent()) return byId.get();
        }

        // 2. Try by license plate (vehicle already exists)
        if (request.getLicensePlate() != null) {
            var byPlate = vehicleRepository.findByLicensePlate(request.getLicensePlate().toUpperCase());
            if (byPlate.isPresent()) return byPlate.get();
        }

        // 3. Auto-create — requires minimum vehicle fields
        if (request.getLicensePlate() == null || request.getMake() == null
                || request.getModel() == null || request.getYear() == null) {
            throw new IllegalArgumentException(
                    "Véhicule Introuvable");
        }

        Client client = resolveClient(request.getClientId(), request.getClientFirstName(),
                request.getClientLastName(), request.getClientEmail(), request.getClientPhone());

        String username = getCurrentUsername();
        Vehicle newVehicle = Vehicle.builder()
                .licensePlate(request.getLicensePlate().toUpperCase())
                .make(request.getMake())
                .model(request.getModel())
                .year(request.getYear())
                .color(request.getColor())
                .vin(request.getVin())
                .client(client)
                .createdByUsername(username)
                .status(VehicleStatus.ACTIVE)
                .mileage(request.getMileageAtService())
                .build();

        Vehicle saved = vehicleRepository.save(newVehicle);
        log.info("Auto-created vehicle {} for client {}", saved.getLicensePlate(), client.getId());

        kafkaProducer.publishVehicleCreated(KafkaEvents.VehicleCreatedEvent.builder()
                .vehicleId(saved.getId())
                .licensePlate(saved.getLicensePlate())
                .make(saved.getMake())
                .model(saved.getModel())
                .year(saved.getYear())
                .clientId(client.getId())
                .clientFirstName(client.getFirstName())
                .clientLastName(client.getLastName())
                .clientEmail(client.getEmail())
                .clientPhone(client.getPhone())
                .createdAt(LocalDateTime.now())
                .build());

        return saved;
    }

    private Client resolveClient(Long clientId, String firstName, String lastName, String email, String phone) {
        if (clientId != null) {
            var existing = clientService.tryFindClientEntityById(clientId);
            if (existing.isPresent()) return existing.get();
            log.warn("Client ID {} not found — registering new client before creating vehicle", clientId);
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("clientId or clientEmail is required to identify the vehicle's owner.");
        }
        return clientService.findOrCreateByEmail(firstName, lastName, email, phone);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "unknown";
    }

    private VehicleDto.VehicleResponse mapToVehicleResponse(Vehicle v) {
        Client client = v.getClient();
        int repairCount = v.getId() != null ? serviceRecordRepository.countByVehicleId(v.getId()) : 0;
        return VehicleDto.VehicleResponse.builder()
                .id(v.getId())
                .licensePlate(v.getLicensePlate())
                .make(v.getMake())
                .model(v.getModel())
                .year(v.getYear())
                .color(v.getColor())
                .vin(v.getVin())
                .clientId(client != null ? client.getId() : null)
                .clientFirstName(client != null ? client.getFirstName() : null)
                .clientLastName(client != null ? client.getLastName() : null)
                .clientEmail(client != null ? client.getEmail() : null)
                .clientPhone(client != null ? client.getPhone() : null)
                .createdByUsername(v.getCreatedByUsername())
                .status(v.getStatus())
                .mileage(v.getMileage())
                .lastServiceDate(v.getLastServiceDate())
                .notes(v.getNotes())
                .repairCount(repairCount)
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }

    private VehicleDto.ServiceRecordResponse mapToServiceResponse(ServiceRecord r) {
        return VehicleDto.ServiceRecordResponse.builder()
                .id(r.getId())
                .vehicleId(r.getVehicle().getId())
                .vehicleLicensePlate(r.getVehicle().getLicensePlate())
                .description(r.getDescription())
                .serviceType(r.getServiceType())
                .serviceDate(r.getServiceDate())
                .scheduledDate(r.getScheduledDate())
                .completedDate(r.getCompletedDate())
                .serviceStatus(r.getServiceStatus())
                .mechanicId(r.getMechanicId())
                .mechanicUsername(r.getMechanicUsername())
                .cost(r.getCost())
                .mileageAtService(r.getMileageAtService())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
