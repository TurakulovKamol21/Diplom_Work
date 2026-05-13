package com.ai.financial.service;

import com.ai.financial.dto.UzRiskResponse;
import com.ai.financial.entity.ExchangeRate;
import com.ai.financial.entity.GDPData;
import com.ai.financial.entity.InflationData;
import com.ai.financial.entity.RiskAssessment;
import com.ai.financial.repository.ExchangeRateRepository;
import com.ai.financial.repository.GDPDataRepository;
import com.ai.financial.repository.InflationDataRepository;
import com.ai.financial.repository.RiskAssessmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RiskAnalysisUzService {

    private final RiskAssessmentRepository riskRepo;
    private final ExchangeRateRepository exchangeRepo;
    private final InflationDataRepository inflationRepo;
    private final GDPDataRepository gdpRepo;

    public RiskAnalysisUzService(RiskAssessmentRepository riskRepo,
                                 ExchangeRateRepository exchangeRepo,
                                 InflationDataRepository inflationRepo,
                                 GDPDataRepository gdpRepo) {
        this.riskRepo = riskRepo;
        this.exchangeRepo = exchangeRepo;
        this.inflationRepo = inflationRepo;
        this.gdpRepo = gdpRepo;
    }

    public UzRiskResponse generateRiskAssessment() {
        RiskAssessment snapshot = computeCurrentRisk();
        return new UzRiskResponse(riskRepo.save(snapshot));
    }

    public UzRiskResponse previewRiskAssessment() {
        return new UzRiskResponse(computeCurrentRisk());
    }

    public UzRiskResponse evaluateScenarioRisk(double inflationRate,
                                               double fxVolatility,
                                               double gdpGrowth) {
        return new UzRiskResponse(buildRiskAssessment(inflationRate, fxVolatility, gdpGrowth));
    }

    private RiskAssessment computeCurrentRisk() {
        List<ExchangeRate> rates = exchangeRepo.findByCurrencyCodeOrderByDateAsc("USD");
        List<InflationData> inflations = inflationRepo.findAllByOrderByPeriodAsc();
        List<GDPData> gdpData = gdpRepo.findAllByOrderByYearAscQuarterAsc();

        double fxVolatility = calculateVolatility(rates);
        double recentInflation = inflations.isEmpty()
            ? 8.5
            : inflations.get(inflations.size() - 1).getAnnualRate();
        double recentGdp = gdpData.isEmpty()
            ? 5.8
            : gdpData.get(gdpData.size() - 1).getGrowthRate();

        return buildRiskAssessment(recentInflation, fxVolatility, recentGdp);
    }

    private RiskAssessment buildRiskAssessment(double recentInflation,
                                               double fxVolatility,
                                               double recentGdp) {
        double inflationProbability = clamp01(((recentInflation - 6.0) / 10.0) + 0.20);
        double devaluationProbability = clamp01((fxVolatility / 40.0) + 0.10);
        double recessionProbability = recentGdp < 4.0
            ? clamp01(((4.0 - recentGdp) / 4.0) + 0.10)
            : clamp01(Math.max(0.03, (5.5 - recentGdp) * 0.04));

        double instabilityWeighted = (inflationProbability * 45.0)
            + (devaluationProbability * 25.0)
            + (recessionProbability * 30.0);
        double stabilityScore = Math.max(0.0, 100.0 - instabilityWeighted);

        RiskAssessment risk = new RiskAssessment();
        risk.setAssessmentDate(LocalDateTime.now());
        risk.setInflationSurgeProbability(inflationProbability);
        risk.setCurrencyDevaluationProbability(devaluationProbability);
        risk.setRecessionProbability(recessionProbability);
        risk.setInstabilityProbability(instabilityWeighted / 100.0);
        risk.setEconomicStabilityScore(stabilityScore);

        if (stabilityScore > 75.0) {
            risk.setMarketRiskLevel("RISK_PAST");
        } else if (stabilityScore > 50.0) {
            risk.setMarketRiskLevel("RISK_MEDIUM");
        } else {
            risk.setMarketRiskLevel("RISK_HIGH");
        }

        risk.setAnalysisSummary("RISK_SUMMARY_TEMPLATE");
        return risk;
    }

    private double calculateVolatility(List<ExchangeRate> rates) {
        if (rates.size() < 2) {
            return 4.0;
        }

        double sumDiff = 0.0;
        int limit = Math.min(30, rates.size());
        for (int i = rates.size() - limit; i < rates.size(); i++) {
            sumDiff += Math.abs(rates.get(i).getDiff());
        }
        return sumDiff / limit;
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
