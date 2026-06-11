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

public class QuoteDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineRequest {
        @NotBlank
        private String description;
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
        private LocalDate date;
        private String description;
        private List<LineRequest> lines;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String clientName;
        private String description;
        private LocalDate date;
        private String status;
        private List<LineRequest> lines;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineResponse {
        private Long id;
        private String description;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuoteResponse {
        private Long id;
        private String quoteNumber;
        private Long clientId;
        private String clientName;
        private String date;
        private String description;
        private BigDecimal total;
        private String status;
        private Long convertedInvoiceId;
        private String createdAt;
        private List<LineResponse> lines;
    }
}
