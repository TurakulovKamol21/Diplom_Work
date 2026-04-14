package com.ai.financial.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "predictions")
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private ForecastRun forecastRun;

    private LocalDate targetDate;

    private Double predictedValue;

    private Double lowerBound; // For confidence intervals
    private Double upperBound;

    private Double confidencePercentage; // e.g. 95%
}
