package com.ai.financial.dto;

import lombok.Data;

@Data
public class ScenarioAnalysisResponse {

    private String generatedAt;
    private ScenarioSnapshot bestCase;
    private ScenarioSnapshot baselineCase;
    private ScenarioSnapshot worstCase;

    @Data
    public static class ScenarioSnapshot {
        private String name;
        private String description;
        private Double gdpDeltaPercentagePoints;
        private Double inflationDeltaPercentagePoints;
        private Double exchangeRateDeltaPercent;
        private Double policyRateDeltaPercentagePoints;
        private MacroForecastResponse macroForecast;
        private UzRiskResponse risk;
    }
}
