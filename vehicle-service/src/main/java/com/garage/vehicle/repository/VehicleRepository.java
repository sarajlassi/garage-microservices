package com.garage.vehicle.repository;

import com.garage.vehicle.entity.Vehicle;
import com.garage.vehicle.entity.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByOwnerId(Long ownerId);
    List<Vehicle> findByStatus(VehicleStatus status);
    Optional<Vehicle> findByLicensePlate(String licensePlate);
    Optional<Vehicle> findByVin(String vin);
    boolean existsByLicensePlate(String licensePlate);
    boolean existsByVin(String vin);
}
