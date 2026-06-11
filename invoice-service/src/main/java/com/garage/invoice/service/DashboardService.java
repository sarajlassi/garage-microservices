package com.garage.invoice.service;

import com.garage.invoice.dto.DashboardDto;
import com.garage.invoice.entity.GarageStats;
import com.garage.invoice.repository.GarageStatsRepository;
import com.garage.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService implements IDashboardService {

    private final InvoiceRepository invoiceRepository;
    private final GarageStatsRepository garageStatsRepository;

    public DashboardDto.StatsResponse getStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startOfLastMonth = startOfMonth.minusMonths(1);

        BigDecimal currentRevenue = invoiceRepository.sumPaidAmountBetween(startOfMonth, now);
        BigDecimal lastRevenue = invoiceRepository.sumPaidAmountBetween(startOfLastMonth, startOfMonth);
        if (currentRevenue == null) currentRevenue = BigDecimal.ZERO;
        if (lastRevenue == null) lastRevenue = BigDecimal.ZERO;

        double delta = 0;
        if (lastRevenue.compareTo(BigDecimal.ZERO) > 0) {
            delta = currentRevenue.subtract(lastRevenue)
                    .divide(lastRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        BigDecimal unpaidAmount = invoiceRepository.sumUnpaidAmount();
        if (unpaidAmount == null) unpaidAmount = BigDecimal.ZERO;
        long unpaidCount = invoiceRepository.countUnpaidInvoices();

        GarageStats stats = garageStatsRepository.findById(1L)
                .orElseGet(() -> GarageStats.builder().id(1L).build());

        return DashboardDto.StatsResponse.builder()
                .revenue(currentRevenue)
                .revenueDeltaPercent(Math.round(delta * 10.0) / 10.0)
                .activeRepairs(stats.getActiveRepairs())
                .pendingRepairs(stats.getPendingRepairs())
                .newClients(stats.getNewClients())
                .unpaidAmount(unpaidAmount)
                .unpaidCount((int) unpaidCount)
                .build();
    }

    public List<DashboardDto.ActivityItem> getActivity() {
        return invoiceRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(inv -> DashboardDto.ActivityItem.builder()
                        .dotColor("blue")
                        .text("Facture " + inv.getInvoiceNumber() + " — " + inv.getClientName())
                        .time(inv.getCreatedAt() != null ? inv.getCreatedAt().toString() : "")
                        .build())
                .toList();
    }

    @Transactional
    public void incrementActiveRepairs() {
        GarageStats stats = getOrCreateStats();
        stats.setActiveRepairs(stats.getActiveRepairs() + 1);
        garageStatsRepository.save(stats);
    }

    @Transactional
    public void incrementNewClients() {
        GarageStats stats = getOrCreateStats();
        stats.setNewClients(stats.getNewClients() + 1);
        garageStatsRepository.save(stats);
    }

    private GarageStats getOrCreateStats() {
        return garageStatsRepository.findById(1L)
                .orElseGet(() -> garageStatsRepository.save(GarageStats.builder().id(1L).build()));
    }
}
