package com.garage.invoice.repository;

import com.garage.invoice.entity.Invoice;
import com.garage.invoice.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByClientId(Long clientId);

    List<Invoice> findByStatus(InvoiceStatus status);

    @Query("SELECT COALESCE(SUM(i.paidAmount), 0) FROM Invoice i WHERE i.status = 'PAID' AND i.paidAt BETWEEN :from AND :to")
    BigDecimal sumPaidAmountBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(i.total - i.paidAmount), 0) FROM Invoice i WHERE i.status NOT IN ('PAID', 'CANCELLED')")
    BigDecimal sumUnpaidAmount();

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.status NOT IN ('PAID', 'CANCELLED')")
    long countUnpaidInvoices();

    List<Invoice> findTop10ByOrderByCreatedAtDesc();
}
