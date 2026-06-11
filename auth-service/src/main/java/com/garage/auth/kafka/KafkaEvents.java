package com.garage.auth.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class KafkaEvents {

    // Consumed from vehicle-service
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
    public static class ServiceScheduledEvent {
        private Long serviceRecordId;
        private Long vehicleId;
        private String licensePlate;
        private String description;
        private LocalDate scheduledDate;
        private Long mechanicId;
        private String mechanicUsername;
        private BigDecimal estimatedCost;
        private LocalDateTime scheduledAt;
    }

    // Consumed from invoice-service
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoicePaidEvent {
        private Long invoiceId;
        private String invoiceNumber;
        private Long clientId;
        private BigDecimal amount;
        private LocalDateTime paidAt;
    }


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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserLoginEvent {
        private Long userId;
        private String username;
        private String email;
        private LocalDateTime loginAt;
        private String ipAddress;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenValidatedEvent {
        private Long userId;
        private String username;
        private String role;
        private boolean valid;
        private LocalDateTime validatedAt;
    }
}
