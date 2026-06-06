package com.garage.stock.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class KafkaEvents {

    // Produced by stock-service
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductAddedEvent {
        private Long productId;
        private String productCode;
        private String productName;
        private String category;
        private LocalDateTime addedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductLowStockEvent {
        private Long productId;
        private String productCode;
        private String productName;
        private Integer currentQuantity;
        private Integer minThreshold;
        private Integer availableQuantity;
        private LocalDateTime alertAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockReservedEvent {
        private Long stockId;
        private Long productId;
        private String productCode;
        private Integer reservedQuantity;
        private Long serviceRecordId;
        private Long vehicleId;
        private LocalDateTime reservedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierOrderPlacedEvent {
        private Long orderId;
        private Long productId;
        private String productCode;
        private Integer quantity;
        private BigDecimal totalPrice;
        private String supplier;
        private LocalDate expectedDeliveryDate;
        private String referenceNumber;
        private LocalDateTime placedAt;
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
        private String serviceType;  // OIL_CHANGE, BRAKE_SERVICE, etc.
        private LocalDate scheduledDate;
        private Long mechanicId;
        private String mechanicUsername;
        private BigDecimal estimatedCost;
        private LocalDateTime scheduledAt;
    }
}


