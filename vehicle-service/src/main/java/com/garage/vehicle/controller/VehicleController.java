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
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<VehicleDto.VehicleResponse> createVehicle(
            @Valid @RequestBody VehicleDto.CreateVehicleRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleService.createVehicle(request));
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

    @GetMapping("/owner/{clientId}")
    public ResponseEntity<List<VehicleDto.VehicleResponse>> getVehiclesByClient(
            @PathVariable Long clientId
    ) {
        return ResponseEntity.ok(vehicleService.getVehiclesByClient(clientId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<List<VehicleDto.VehicleResponse>> getVehiclesByStatus(
            @PathVariable VehicleStatus status
    ) {
        return ResponseEntity.ok(vehicleService.getVehiclesByStatus(status));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<VehicleDto.VehicleResponse> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody VehicleDto.UpdateVehicleRequest request
    ) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    // ── Nested service-record endpoints ─────────────────────────────────────

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
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
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
            @Valid @RequestBody VehicleDto.UpdateServiceRequest request
    ) {
        return ResponseEntity.ok(vehicleService.updateServiceRecord(vehicleId, recordId, request));
    }

    @DeleteMapping("/{vehicleId}/services/{recordId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<Void> deleteServiceRecord(
            @PathVariable Long vehicleId,
            @PathVariable Long recordId) {
        vehicleService.deleteServiceRecord(vehicleId, recordId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/services/{recordId}/parts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<VehicleDto.ServiceRecordResponse> addUsedPart(
            @PathVariable Long recordId,
            @RequestBody VehicleDto.AddUsedPartRequest request) {
        return ResponseEntity.ok(vehicleService.addUsedPart(recordId, request));
    }
}
