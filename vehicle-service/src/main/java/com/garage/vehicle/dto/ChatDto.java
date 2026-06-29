package com.garage.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ChatDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRequest {
        @NotBlank
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatResponse {
        private String reply;
    }
}
