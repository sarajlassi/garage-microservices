package com.garage.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class InvoiceDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineRequest {
        private Long stockItemId;
        @NotBlank
        private String name;
        private String ref;
        @NotNull
        private Integer quantity;
        @NotNull
        private BigDecimal unitPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotNull(message = "clientId is required")
        private Long clientId;
        private String clientName;
        private String clientPhone;
        private Long vehicleId;
        private String vehicleName;
        private String licensePlate;
        private Long serviceRecordId;
        private String mechanicName;
        private String description;
        private LocalDate entryDate;
        private LocalDate invoiceDate;
        private String laborDescription;
        @Builder.Default
        private BigDecimal laborCost = BigDecimal.ZERO;
        private List<LineRequest> lines;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String clientName;
        private String clientPhone;
        private String mechanicName;
        private String description;
        private LocalDate invoiceDate;
        private String laborDescription;
        private BigDecimal laborCost;
        private List<LineRequest> lines;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayRequest {
        @NotBlank(message = "paymentMethod is required")
        private String paymentMethod;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineResponse {
        private Long id;
        private Long stockItemId;
        private String name;
        private String ref;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceResponse {
        private Long id;
        private String invoiceNumber;
        private Long clientId;
        private String clientName;
        private String clientPhone;
        private Long vehicleId;
        private String vehicleName;
        private String licensePlate;
        private Long serviceRecordId;
        private String mechanicName;
        private String description;
        private String entryDate;
        private String invoiceDate;
        private String laborDescription;
        private BigDecimal laborCost;
        private BigDecimal totalParts;
        private BigDecimal total;
        private BigDecimal paidAmount;
        private BigDecimal remaining;
        private String status;
        private String paymentMethod;
        private String paidAt;
        private String createdAt;
        private List<LineResponse> lines;
    }
}
