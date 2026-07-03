package com.garage.invoice.service;

import com.garage.invoice.dto.DashboardDto;
import com.garage.invoice.entity.GarageStats;
import com.garage.invoice.entity.Invoice;
import com.garage.invoice.entity.InvoiceStatus;
import com.garage.invoice.repository.GarageStatsRepository;
import com.garage.invoice.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private GarageStatsRepository garageStatsRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private GarageStats sampleStats;

    @BeforeEach
    void setUp() {
        sampleStats = GarageStats.builder()
                .id(1L).activeRepairs(5).pendingRepairs(3).newClients(2).build();
    }

    // ── getStats ──────────────────────────────────────────────────────────────

    @Test
    void getStats_withValidData_returnsStatsResponse() {
        when(invoiceRepository.sumPaidAmountBetween(any(), any()))
                .thenReturn(BigDecimal.valueOf(1500), BigDecimal.valueOf(1000));
        when(invoiceRepository.sumUnpaidAmount()).thenReturn(BigDecimal.valueOf(300));
        when(invoiceRepository.countUnpaidInvoices()).thenReturn(3L);
        when(garageStatsRepository.findById(1L)).thenReturn(Optional.of(sampleStats));

        DashboardDto.StatsResponse resp = dashboardService.getStats();

        assertThat(resp).isNotNull();
        assertThat(resp.getRevenue()).isEqualByComparingTo("1500");
        assertThat(resp.getActiveRepairs()).isEqualTo(5);
        assertThat(resp.getPendingRepairs()).isEqualTo(3);
        assertThat(resp.getUnpaidAmount()).isEqualByComparingTo("300");
        assertThat(resp.getUnpaidCount()).isEqualTo(3);
    }

    @Test
    void getStats_nullRevenue_treatsAsZero() {
        when(invoiceRepository.sumPaidAmountBetween(any(), any())).thenReturn(null);
        when(invoiceRepository.sumUnpaidAmount()).thenReturn(null);
        when(invoiceRepository.countUnpaidInvoices()).thenReturn(0L);
        when(garageStatsRepository.findById(1L)).thenReturn(Optional.of(sampleStats));

        DashboardDto.StatsResponse resp = dashboardService.getStats();

        assertThat(resp.getRevenue()).isEqualByComparingTo("0");
        assertThat(resp.getUnpaidAmount()).isEqualByComparingTo("0");
        assertThat(resp.getRevenueDeltaPercent()).isEqualTo(0.0);
    }

    @Test
    void getStats_noStatsRecord_createsDefault() {
        when(invoiceRepository.sumPaidAmountBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.sumUnpaidAmount()).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.countUnpaidInvoices()).thenReturn(0L);
        when(garageStatsRepository.findById(1L)).thenReturn(Optional.empty());

        DashboardDto.StatsResponse resp = dashboardService.getStats();

        assertThat(resp.getActiveRepairs()).isEqualTo(0);
    }

    @Test
    void getStats_withLastMonthRevenue_computesDelta() {
        // current = 1200, last = 1000, delta = +20%
        when(invoiceRepository.sumPaidAmountBetween(any(), any()))
                .thenReturn(BigDecimal.valueOf(1200), BigDecimal.valueOf(1000));
        when(invoiceRepository.sumUnpaidAmount()).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.countUnpaidInvoices()).thenReturn(0L);
        when(garageStatsRepository.findById(1L)).thenReturn(Optional.of(sampleStats));

        DashboardDto.StatsResponse resp = dashboardService.getStats();

        assertThat(resp.getRevenueDeltaPercent()).isEqualTo(20.0);
    }

    @Test
    void getStats_zeroLastRevenue_deltaIsZero() {
        when(invoiceRepository.sumPaidAmountBetween(any(), any()))
                .thenReturn(BigDecimal.valueOf(500), BigDecimal.ZERO);
        when(invoiceRepository.sumUnpaidAmount()).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.countUnpaidInvoices()).thenReturn(0L);
        when(garageStatsRepository.findById(1L)).thenReturn(Optional.of(sampleStats));

        DashboardDto.StatsResponse resp = dashboardService.getStats();

        assertThat(resp.getRevenueDeltaPercent()).isEqualTo(0.0);
    }

    // ── getActivity ───────────────────────────────────────────────────────────

    @Test
    void getActivity_returnsMappedList() {
        Invoice inv = Invoice.builder()
                .id(1L).invoiceNumber("INV-2026-00001").clientName("Alice")
                .status(InvoiceStatus.DRAFT).lines(new ArrayList<>())
                .laborCost(BigDecimal.ZERO).totalParts(BigDecimal.ZERO)
                .total(BigDecimal.ZERO).paidAmount(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now()).build();

        when(invoiceRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of(inv));

        List<DashboardDto.ActivityItem> result = dashboardService.getActivity();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getText()).contains("INV-2026-00001");
        assertThat(result.get(0).getDotColor()).isEqualTo("blue");
    }

    @Test
    void getActivity_invoiceWithNullCreatedAt_returnsEmptyTimeString() {
        Invoice inv = Invoice.builder()
                .id(2L).invoiceNumber("INV-2026-00002").clientName("Bob")
                .status(InvoiceStatus.DRAFT).lines(new ArrayList<>())
                .laborCost(BigDecimal.ZERO).totalParts(BigDecimal.ZERO)
                .total(BigDecimal.ZERO).paidAmount(BigDecimal.ZERO)
                .createdAt(null).build();

        when(invoiceRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of(inv));

        List<DashboardDto.ActivityItem> result = dashboardService.getActivity();

        assertThat(result.get(0).getTime()).isEqualTo("");
    }

    @Test
    void getActivity_emptyList_returnsEmpty() {
        when(invoiceRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<DashboardDto.ActivityItem> result = dashboardService.getActivity();

        assertThat(result).isEmpty();
    }

    // ── incrementActiveRepairs ────────────────────────────────────────────────

    @Test
    void incrementActiveRepairs_existingStats_incrementsByOne() {
        when(garageStatsRepository.findById(1L)).thenReturn(Optional.of(sampleStats));
        when(garageStatsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        dashboardService.incrementActiveRepairs();

        assertThat(sampleStats.getActiveRepairs()).isEqualTo(6);
        verify(garageStatsRepository).save(sampleStats);
    }

    @Test
    void incrementActiveRepairs_noStats_createsAndIncrements() {
        GarageStats newStats = GarageStats.builder().id(1L).activeRepairs(0).build();
        when(garageStatsRepository.findById(1L)).thenReturn(Optional.empty());
        when(garageStatsRepository.save(any())).thenReturn(newStats);

        dashboardService.incrementActiveRepairs();

        verify(garageStatsRepository, times(2)).save(any());
    }

    // ── incrementNewClients ───────────────────────────────────────────────────

    @Test
    void incrementNewClients_existingStats_incrementsByOne() {
        when(garageStatsRepository.findById(1L)).thenReturn(Optional.of(sampleStats));
        when(garageStatsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        dashboardService.incrementNewClients();

        assertThat(sampleStats.getNewClients()).isEqualTo(3);
        verify(garageStatsRepository).save(sampleStats);
    }

    @Test
    void incrementNewClients_noStats_createsNew() {
        GarageStats newStats = GarageStats.builder().id(1L).newClients(0).build();
        when(garageStatsRepository.findById(1L)).thenReturn(Optional.empty());
        when(garageStatsRepository.save(any())).thenReturn(newStats);

        dashboardService.incrementNewClients();

        verify(garageStatsRepository, times(2)).save(any());
    }
}
