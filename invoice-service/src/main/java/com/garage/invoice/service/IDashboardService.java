package com.garage.invoice.service;

import com.garage.invoice.dto.DashboardDto;

import java.util.List;

public interface IDashboardService {

    DashboardDto.StatsResponse getStats();

    List<DashboardDto.ActivityItem> getActivity();

    void incrementActiveRepairs();

    void incrementNewClients();
}
