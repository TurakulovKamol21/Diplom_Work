package com.ai.financial.service;

import com.ai.financial.entity.*;
import com.ai.financial.repository.*;
import com.ai.financial.dto.UzRiskResponse;
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
        List<ExchangeRate> rates = exchangeRepo.findByCurrencyCodeOrderByDateAsc("USD");
        List<InflationData> inflations = inflationRepo.findAllByOrderByPeriodAsc();
        List<GDPData> gdpData = gdpRepo.findAllByOrderByYearAscQuarterAsc();

        double fxVolatility = calculateVolatility(rates);
        double P_fx = Math.min(1.0, fxVolatility / 100.0); 

        double recentInflation = inflations.isEmpty() ? 8.5 : inflations.get(inflations.size() - 1).getAnnualRate();
        double P_inf = Math.min(1.0, recentInflation / 20.0); 

        double recentGdp = gdpData.isEmpty() ? 5.8 : gdpData.get(gdpData.size() - 1).getGrowthRate();
        double P_rec = recentGdp < 4.0 ? Math.min(1.0, (4.0 - recentGdp) / 4.0 + 0.1) : 0.05; 

        RiskAssessment risk = new RiskAssessment();
        risk.setAssessmentDate(LocalDateTime.now());
        risk.setCurrencyDevaluationProbability(P_fx);
        risk.setInflationSurgeProbability(P_inf);
        risk.setRecessionProbability(P_rec);

        double instabilityWeighted = (P_inf * 45) + (P_fx * 25) + (P_rec * 30);
        double score = Math.max(0, 100 - instabilityWeighted);
        
        risk.setEconomicStabilityScore(score);
        risk.setInstabilityProbability(instabilityWeighted / 100.0);

        // RISK_PAST, RISK_MEDIUM, RISK_HIGH keys
        if (score > 75) risk.setMarketRiskLevel("RISK_PAST");
        else if (score > 50) risk.setMarketRiskLevel("RISK_MEDIUM");
        else risk.setMarketRiskLevel("RISK_HIGH");

        // Use summary field as a template key for frontend
        risk.setAnalysisSummary("RISK_SUMMARY_TEMPLATE");
        
        return new UzRiskResponse(riskRepo.save(risk));
    }

    private double calculateVolatility(List<ExchangeRate> rates) {
        if (rates.size() < 2) return 4.0;
        double sumDiff = 0;
        int limit = Math.min(30, rates.size());
        for (int i = rates.size() - limit; i < rates.size(); i++) {
            sumDiff += Math.abs(rates.get(i).getDiff());
        }
        return sumDiff / limit; 
    }
}
