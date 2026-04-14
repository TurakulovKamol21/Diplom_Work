package com.ai.financial.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "forecast_runs")
public class ForecastRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime runDate;

    @Column(nullable = false)
    private String algorithmType; // LINEAR_REGRESSION, MOVING_AVERAGE, WEIGHTED_TREND

    @Column(nullable = false)
    private String targetEntity; // EXCHANGE_RATE, INFLATION, GDP

    private Double modelAccuracy; // R-squared or MAPE score

    private String status; // SUCCESS, FAILED
}
