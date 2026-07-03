package com.garage.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "used_parts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsedPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_record_id", nullable = false)
    private ServiceRecord serviceRecord;

    @Column(name = "stock_item_id", nullable = false)
    private Long stockItemId;

    @Column(nullable = false)
    private String name;

    @Column
    private String ref;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal unitPrice;
}
