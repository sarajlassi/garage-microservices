package com.garage.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicle_owner_cache")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleOwnerCache {

    @Id
    private Long vehicleId;

    @Column(nullable = false)
    private Long clientId;
}
