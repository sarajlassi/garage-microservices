package com.garage.invoice.controller;

import com.garage.invoice.dto.DashboardDto;
import com.garage.invoice.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardDto.StatsResponse> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping("/activity")
    public ResponseEntity<List<DashboardDto.ActivityItem>> getActivity() {
        return ResponseEntity.ok(dashboardService.getActivity());
    }
}
