package com.garage.vehicle.kafka;

import com.garage.vehicle.entity.ServiceType;
import com.garage.vehicle.entity.VehicleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class KafkaEvents {

    // Produced by vehicle-service
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleCreatedEvent {
        private Long vehicleId;
        private String licensePlate;
        private String make;
        private String model;
        private Integer year;
        private Long ownerId;
        private String ownerUsername;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleUpdatedEvent {
        private Long vehicleId;
        private String licensePlate;
        private VehicleStatus oldStatus;
        private VehicleStatus newStatus;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleDeletedEvent {
        private Long vehicleId;
        private String licensePlate;
        private Long ownerId;
        private LocalDateTime deletedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceScheduledEvent {
        private Long serviceRecordId;
        private Long vehicleId;
        private String licensePlate;
        private String description;
        private ServiceType serviceType;
        private LocalDate scheduledDate;
        private Long mechanicId;
        private String mechanicUsername;
        private BigDecimal estimatedCost;
        private LocalDateTime scheduledAt;
    }

    // Consumed from auth-service
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRegisteredEvent {
        private Long userId;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
        private LocalDateTime registeredAt;
    }
}
