package com.garage.invoice.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class KafkaEvents {

    // Produced by invoice-service
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
    public static class InvoiceCreatedEvent {
        private Long invoiceId;
        private String invoiceNumber;
        private Long clientId;
        private BigDecimal total;
        private LocalDateTime createdAt;
    }

    // Consumed from vehicle-service
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceScheduledEvent {
        private Long serviceRecordId;
        private Long vehicleId;
        private String licensePlate;
        private String description;
        private String serviceType;
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
