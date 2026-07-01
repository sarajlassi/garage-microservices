package com.garage.vehicle.dto;

import com.garage.vehicle.entity.ServiceStatus;
import com.garage.vehicle.entity.ServiceType;
import com.garage.vehicle.entity.VehicleStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class VehicleDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateVehicleRequest {
        @NotBlank(message = "License plate is required")
        private String licensePlate;

        @NotBlank(message = "Make is required")
        private String make;

        @NotBlank(message = "Model is required")
        private String model;

        @NotNull(message = "Year is required")
        @Min(value = 1900, message = "Year must be >= 1900")
        @Max(value = 2100, message = "Year must be <= 2100")
        private Integer year;

        private String color;
        private String vin;
        private Integer mileage;
        private String notes;

        // Owning client — either reference an existing client by ID,
        // or supply enough info to find-or-create one by email.
        private Long clientId;
        private String clientFirstName;
        private String clientLastName;
        private String clientEmail;
        private String clientPhone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateVehicleRequest {
        private String make;
        private String model;
        private String color;
        private Integer mileage;
        private VehicleStatus status;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleResponse {
        private Long id;
        private String licensePlate;
        private String make;
        private String model;
        private Integer year;
        private String color;
        private String vin;
        private Long clientId;
        private String clientFirstName;
        private String clientLastName;
        private String clientEmail;
        private String clientPhone;
        private String createdByUsername;
        private VehicleStatus status;
        private Integer mileage;
        private LocalDate lastServiceDate;
        private String notes;
        private int repairCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateServiceRequest {
        @NotBlank(message = "Description is required")
        private String description;

        @NotNull(message = "Service type is required")
        private ServiceType serviceType;

        @NotNull(message = "Service date is required")
        private LocalDate serviceDate;

        private LocalDate scheduledDate;
        private Integer mileageAtService;
        private BigDecimal cost;
        private String notes;

        // Vehicle fields — used to look up an existing vehicle by plate
        // or create a new one when no vehicleId is provided.
        private String licensePlate;
        private String make;
        private String model;
        private Integer year;
        private String color;
        private String vin;
        private Long clientId;

        // Client fields — used to find-or-create the client if they
        // do not exist yet.
        private String clientFirstName;
        private String clientLastName;
        private String clientEmail;
        private String clientPhone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateServiceRequest {
        private String description;
        private ServiceStatus serviceStatus;
        private LocalDate completedDate;
        private BigDecimal cost;
        private Integer mileageAtService;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceRecordResponse {
        private Long id;
        private Long vehicleId;
        private String vehicleLicensePlate;
        private String description;
        private ServiceType serviceType;
        private LocalDate serviceDate;
        private LocalDate scheduledDate;
        private LocalDate completedDate;
        private ServiceStatus serviceStatus;
        private Long mechanicId;
        private String mechanicUsername;
        private BigDecimal cost;
        private Integer mileageAtService;
        private String notes;
        private LocalDateTime createdAt;
    }
}
