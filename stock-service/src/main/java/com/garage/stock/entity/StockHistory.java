package com.garage.stock.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockMovementType movementType;  // IN, OUT, RESERVED, UNRESERVED, ADJUSTMENT

    @Column(nullable = false)
    private Integer quantity;

    private String reference;  // Service ID, Order ID, or description

    private String notes;

    private Long mechanicId;  // If used by a mechanic

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum StockMovementType {
        IN,          // Stock received
        OUT,         // Stock used in service
        RESERVED,    // Stock reserved for upcoming service
        UNRESERVED,  // Stock no longer reserved
        ADJUSTMENT   // Manual adjustment
    }
}

