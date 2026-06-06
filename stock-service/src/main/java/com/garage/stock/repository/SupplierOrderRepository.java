package com.garage.stock.repository;

import com.garage.stock.entity.SupplierOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SupplierOrderRepository extends JpaRepository<SupplierOrder, Long> {
    List<SupplierOrder> findByProductId(Long productId);
    List<SupplierOrder> findByStatus(SupplierOrder.OrderStatus status);
    List<SupplierOrder> findBySupplier(String supplier);
    List<SupplierOrder> findByExpectedDeliveryDateBetween(LocalDate startDate, LocalDate endDate);
    List<SupplierOrder> findByReferenceNumber(String referenceNumber);
}

