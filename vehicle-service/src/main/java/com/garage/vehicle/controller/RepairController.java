package com.garage.vehicle.controller;

import com.garage.vehicle.dto.VehicleDto;
import com.garage.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Flat repair (service record) endpoints expected by the frontend.
 * Delegates to VehicleService for business logic.
 */
@Slf4j
@RestController
@RequestMapping("/api/repairs")
@RequiredArgsConstructor
public class RepairController {

    private final VehicleService vehicleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<List<VehicleDto.ServiceRecordResponse>> getAll(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) Long clientId) {

        if (vehicleId != null) {
            return ResponseEntity.ok(vehicleService.getServiceRecordsByVehicle(vehicleId));
        }
        if (clientId != null) {
            return ResponseEntity.ok(vehicleService.getServiceRecordsByOwner(clientId));
        }
        return ResponseEntity.ok(vehicleService.getAllServiceRecords());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleDto.ServiceRecordResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getServiceRecordById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<VehicleDto.ServiceRecordResponse> create(
            @RequestParam Long vehicleId,
            @Valid @RequestBody VehicleDto.CreateServiceRequest request) {
        log.info("Creating repair for vehicle {}", vehicleId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleService.createServiceRecord(vehicleId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<VehicleDto.ServiceRecordResponse> update(
            @PathVariable Long id,
            @RequestParam Long vehicleId,
            @RequestBody VehicleDto.UpdateServiceRequest request) {
        return ResponseEntity.ok(vehicleService.updateServiceRecord(vehicleId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vehicleService.deleteServiceRecord(id);
        return ResponseEntity.noContent().build();
    }
}
