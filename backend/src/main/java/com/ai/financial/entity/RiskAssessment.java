package com.ai.financial.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "risk_assessments")
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime assessmentDate;

    // Specific to Uzbekistan's economy
    private Double inflationSurgeProbability; 
    
    private Double currencyDevaluationProbability;

    private Double recessionProbability; // 0.0 to 1.0 (e.g. 0.05 -> 5%)

    @Column(name = "instability_prob")
    private Double instabilityProbability;

    private String marketRiskLevel; // LOW, MEDIUM, HIGH

    private Double economicStabilityScore; // 0 to 100

    @Column(length = 2000)
    private String analysisSummary; // AI-generated text summary or key drivers
}
