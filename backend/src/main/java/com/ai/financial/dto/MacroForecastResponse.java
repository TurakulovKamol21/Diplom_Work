package com.ai.financial.dto;

import lombok.Data;
import java.util.List;

@Data
public class MacroForecastResponse {

    private ForecastResult gdpForecast;
    private ForecastResult inflationForecast;
    private ForecastResult exchangeForecast;
    private ForecastResult policyRateForecast;
    
    @Data
    public static class ForecastResult {
        private Double value;
        private Double previousValue;
        private Double percentChange;
        private Double probability;
        private Double confidenceLevel;
        private Double lowerBound;
        private Double upperBound;
        private Double modelAccuracy;
        private Double benchmarkAccuracy;
        private Integer forecastHorizon;
        private Integer testPoints;
        private String trend; // GROWTH, STABLE, DECLINE, DEPRECIATION, APPRECIATION
        private String method; // WEIGHTED_TREND, LINEAR_REGRESSION, MOVING_AVERAGE
        private String baselineMethod;
        private String mlMethod;
        private String description;
    }
}
