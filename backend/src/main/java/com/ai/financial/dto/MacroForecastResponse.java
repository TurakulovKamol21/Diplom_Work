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
        private String trend; // GROWTH, STABLE, DECLINE, DEPRECIATION, APPRECIATION
        private String method; // WEIGHTED_TREND, LINEAR_REGRESSION, MOVING_AVERAGE
        private String description;
    }
}
