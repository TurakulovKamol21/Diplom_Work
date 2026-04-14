package com.ai.financial.dto;

import com.ai.financial.entity.RiskAssessment;

public class UzRiskResponse {

    private String assessmentDate;
    private String marketRiskLevel;
    private Double economicStabilityScore;
    private Double inflationSurgeProbability;
    private Double currencyDevaluationProbability;
    private Double recessionProbability;
    private String summary;

    public UzRiskResponse() {}

    public UzRiskResponse(RiskAssessment assessment) {
        this.assessmentDate = assessment.getAssessmentDate().toString();
        this.marketRiskLevel = assessment.getMarketRiskLevel();
        this.economicStabilityScore = assessment.getEconomicStabilityScore();
        this.inflationSurgeProbability = assessment.getInflationSurgeProbability();
        this.currencyDevaluationProbability = assessment.getCurrencyDevaluationProbability();
        this.recessionProbability = assessment.getRecessionProbability();
        this.summary = assessment.getAnalysisSummary();
    }

    // Getters and Setters
    public String getAssessmentDate() { return assessmentDate; }
    public void setAssessmentDate(String assessmentDate) { this.assessmentDate = assessmentDate; }

    public String getMarketRiskLevel() { return marketRiskLevel; }
    public void setMarketRiskLevel(String marketRiskLevel) { this.marketRiskLevel = marketRiskLevel; }

    public Double getEconomicStabilityScore() { return economicStabilityScore; }
    public void setEconomicStabilityScore(Double economicStabilityScore) { this.economicStabilityScore = economicStabilityScore; }

    public Double getInflationSurgeProbability() { return inflationSurgeProbability; }
    public void setInflationSurgeProbability(Double inflationSurgeProbability) { this.inflationSurgeProbability = inflationSurgeProbability; }

    public Double getCurrencyDevaluationProbability() { return currencyDevaluationProbability; }
    public void setCurrencyDevaluationProbability(Double currencyDevaluationProbability) { this.currencyDevaluationProbability = currencyDevaluationProbability; }

    public Double getRecessionProbability() { return recessionProbability; }
    public void setRecessionProbability(Double recessionProbability) { this.recessionProbability = recessionProbability; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
