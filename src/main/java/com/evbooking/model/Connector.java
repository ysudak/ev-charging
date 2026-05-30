package com.evbooking.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "connectors")
@Getter
@Setter
@NoArgsConstructor
public class Connector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private ChargingStation station;

    @NotBlank
    @Column(name = "connector_type", nullable = false, length = 30)
    private String connectorType;

    @Min(1)
    @Column(name = "power_kw", nullable = false)
    private Integer powerKw;

    public Connector(ChargingStation station, String connectorType, Integer powerKw) {
        this.station = station;
        this.connectorType = connectorType;
        this.powerKw = powerKw;
    }
}
