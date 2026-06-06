package com.garage.stock.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private String supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;  // PENDING, ORDERED, IN_TRANSIT, RECEIVED, CANCELLED

    @Temporal(TemporalType.DATE)
    private LocalDate orderDate;

    @Temporal(TemporalType.DATE)
    private LocalDate expectedDeliveryDate;

    @Temporal(TemporalType.DATE)
    private LocalDate actualDeliveryDate;

    private String referenceNumber;

    private String notes;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (totalPrice == null && unitPrice != null) {
            totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    @PrePersist
    protected void onCreate() {
        if (orderDate == null) {
            orderDate = LocalDate.now();
        }
        if (totalPrice == null && unitPrice != null) {
            totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public enum OrderStatus {
        PENDING,
        ORDERED,
        IN_TRANSIT,
        RECEIVED,
        CANCELLED
    }
}

