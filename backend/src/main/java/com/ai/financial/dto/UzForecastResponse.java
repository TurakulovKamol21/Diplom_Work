package com.ai.financial.dto;

import java.util.List;

public class UzForecastResponse {

    private String target; // "USD/UZS", "GDP", "INFLATION"
    private Double baseValue;
    private String trend; // "DEPRECIATION", "GROWTH", "STABLE"
    private List<PredictionDto> predictions;
    private String model;
    private Double rSquare;

    // Getters and Setters
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public Double getBaseValue() { return baseValue; }
    public void setBaseValue(Double baseValue) { this.baseValue = baseValue; }

    public String getTrend() { return trend; }
    public void setTrend(String trend) { this.trend = trend; }

    public List<PredictionDto> getPredictions() { return predictions; }
    public void setPredictions(List<PredictionDto> predictions) { this.predictions = predictions; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Double getrSquare() { return rSquare; }
    public void setrSquare(Double rSquare) { this.rSquare = rSquare; }
    
    public static class PredictionDto {
        private String date;
        private Double predictedValue;
        private Double lowerBound;
        private Double upperBound;
        private Double probabilityScore;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public Double getPredictedValue() { return predictedValue; }
        public void setPredictedValue(Double predictedValue) { this.predictedValue = predictedValue; }

        public Double getLowerBound() { return lowerBound; }
        public void setLowerBound(Double lowerBound) { this.lowerBound = lowerBound; }

        public Double getUpperBound() { return upperBound; }
        public void setUpperBound(Double upperBound) { this.upperBound = upperBound; }

        public Double getProbabilityScore() { return probabilityScore; }
        public void setProbabilityScore(Double probabilityScore) { this.probabilityScore = probabilityScore; }
    }
}
