package com.garage.invoice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "garage_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GarageStats {

    @Id
    private Long id;

    @Builder.Default
    @Column(name = "active_repairs")
    private int activeRepairs = 0;

    @Builder.Default
    @Column(name = "pending_repairs")
    private int pendingRepairs = 0;

    @Builder.Default
    @Column(name = "new_clients")
    private int newClients = 0;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
