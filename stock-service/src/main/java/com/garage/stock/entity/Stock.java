package com.garage.stock.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity = 0;  // Current stock quantity

    @Column(nullable = false)
    private Integer minThreshold = 5;  // When to trigger low stock alert

    @Column(nullable = false)
    private Integer maxThreshold = 100;  // Maximum stock level

    @Column(nullable = false)
    private Integer reserved = 0;  // Quantity reserved for current services

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastRestockDate;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedAt;

    public Integer getAvailableQuantity() {
        return quantity - reserved;
    }

    public boolean isLowStock() {
        return getAvailableQuantity() <= minThreshold;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

