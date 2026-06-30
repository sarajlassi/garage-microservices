package com.garage.invoice.controller;

import com.garage.invoice.dto.DashboardDto;
import com.garage.invoice.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<DashboardDto.StatsResponse> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping("/activity")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECANICIEN')")
    public ResponseEntity<List<DashboardDto.ActivityItem>> getActivity() {
        return ResponseEntity.ok(dashboardService.getActivity());
    }
}
