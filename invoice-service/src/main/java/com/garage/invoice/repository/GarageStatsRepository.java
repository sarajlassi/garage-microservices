package com.garage.invoice.repository;

import com.garage.invoice.entity.GarageStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GarageStatsRepository extends JpaRepository<GarageStats, Long> {
}
