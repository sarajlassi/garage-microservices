package com.garage.vehicle.controller;

import com.garage.vehicle.dto.VehicleDto;
import com.garage.vehicle.entity.VehicleStatus;
import com.garage.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<VehicleDto.VehicleResponse> createVehicle(
            @Valid @RequestBody VehicleDto.CreateVehicleRequest request,
            @RequestHeader("X-User-Id") Long ownerId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleService.createVehicle(request, ownerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleDto.VehicleResponse> getVehicle(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<List<VehicleDto.VehicleResponse>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<VehicleDto.VehicleResponse>> getVehiclesByOwner(
            @PathVariable Long ownerId
    ) {
        return ResponseEntity.ok(vehicleService.getVehiclesByOwner(ownerId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<List<VehicleDto.VehicleResponse>> getVehiclesByStatus(
            @PathVariable VehicleStatus status
    ) {
        return ResponseEntity.ok(vehicleService.getVehiclesByStatus(status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleDto.VehicleResponse> updateVehicle(
            @PathVariable Long id,
            @RequestBody VehicleDto.UpdateVehicleRequest request
    ) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    // Service records endpoints

    @PostMapping("/{vehicleId}/services")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<VehicleDto.ServiceRecordResponse> createServiceRecord(
            @PathVariable Long vehicleId,
            @Valid @RequestBody VehicleDto.CreateServiceRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleService.createServiceRecord(vehicleId, request));
    }

    @GetMapping("/{vehicleId}/services")
    public ResponseEntity<List<VehicleDto.ServiceRecordResponse>> getServiceRecords(
            @PathVariable Long vehicleId
    ) {
        return ResponseEntity.ok(vehicleService.getServiceRecordsByVehicle(vehicleId));
    }

    @PutMapping("/{vehicleId}/services/{recordId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<VehicleDto.ServiceRecordResponse> updateServiceRecord(
            @PathVariable Long vehicleId,
            @PathVariable Long recordId,
            @RequestBody VehicleDto.UpdateServiceRequest request
    ) {
        return ResponseEntity.ok(vehicleService.updateServiceRecord(vehicleId, recordId, request));
    }
}
