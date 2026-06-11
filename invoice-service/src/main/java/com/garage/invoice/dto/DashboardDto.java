package com.garage.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class DashboardDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsResponse {
        private BigDecimal revenue;
        private double revenueDeltaPercent;
        private int activeRepairs;
        private int pendingRepairs;
        private int newClients;
        private BigDecimal unpaidAmount;
        private int unpaidCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        private String dotColor;
        private String text;
        private String time;
    }
}
