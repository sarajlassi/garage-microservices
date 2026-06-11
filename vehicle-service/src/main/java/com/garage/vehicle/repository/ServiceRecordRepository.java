package com.garage.vehicle.repository;

import com.garage.vehicle.entity.ServiceRecord;
import com.garage.vehicle.entity.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {
    List<ServiceRecord> findByVehicleId(Long vehicleId);
    List<ServiceRecord> findByServiceStatus(ServiceStatus status);
    List<ServiceRecord> findByMechanicId(Long mechanicId);

    @org.springframework.data.jpa.repository.Query("SELECT sr FROM ServiceRecord sr WHERE sr.vehicle.ownerId = :ownerId")
    List<ServiceRecord> findByVehicleOwnerId(@org.springframework.data.repository.query.Param("ownerId") Long ownerId);
}
