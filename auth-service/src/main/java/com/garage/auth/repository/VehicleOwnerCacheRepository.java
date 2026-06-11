package com.garage.auth.repository;

import com.garage.auth.entity.VehicleOwnerCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleOwnerCacheRepository extends JpaRepository<VehicleOwnerCache, Long> {
    Optional<VehicleOwnerCache> findByVehicleId(Long vehicleId);
}
