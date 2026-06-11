package com.garage.invoice.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", unique = true)
    private String invoiceNumber;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "client_phone")
    private String clientPhone;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "vehicle_name")
    private String vehicleName;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "service_record_id")
    private Long serviceRecordId;

    @Column(name = "mechanic_name")
    private String mechanicName;

    @Column(length = 1000)
    private String description;

    @Column(name = "entry_date")
    private LocalDate entryDate;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "labor_description")
    private String laborDescription;

    @Builder.Default
    @Column(name = "labor_cost", precision = 10, scale = 2)
    private BigDecimal laborCost = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_parts", precision = 10, scale = 2)
    private BigDecimal totalParts = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<InvoiceLine> lines = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (laborCost == null) laborCost = BigDecimal.ZERO;
        if (totalParts == null) totalParts = BigDecimal.ZERO;
        if (paidAmount == null) paidAmount = BigDecimal.ZERO;
        recomputeTotal();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        recomputeTotal();
    }

    public void recomputeTotal() {
        BigDecimal parts = lines == null ? BigDecimal.ZERO :
            lines.stream()
                .filter(l -> l.getLineTotal() != null)
                .map(InvoiceLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalParts = parts;
        total = (laborCost != null ? laborCost : BigDecimal.ZERO).add(parts);
    }
}
