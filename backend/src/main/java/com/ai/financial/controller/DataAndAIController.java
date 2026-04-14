package com.ai.financial.controller;

import com.ai.financial.entity.*;
import com.ai.financial.repository.*;
import com.ai.financial.service.*;
import com.ai.financial.dto.MacroForecastResponse;
import com.ai.financial.dto.UzRiskResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DataAndAIController {

    private final CbuIntegrationService cbuService;
    private final StatUzIntegrationService statService;
    private final StatisticalForecastingUzService aiService;
    private final RiskAnalysisUzService riskService;
    private final ExchangeRateRepository fxRepo;
    private final GDPDataRepository gdpRepo;
    private final InflationDataRepository infRepo;
    private final PolicyRateDataRepository policyRepo;
    private final SectorGrowthDataRepository sectorRepo;
    private final MarketIndexDataRepository marketRepo;


    public DataAndAIController(CbuIntegrationService cbuService,
                               StatUzIntegrationService statService,
                               StatisticalForecastingUzService aiService,
                               RiskAnalysisUzService riskService,
                               ExchangeRateRepository fxRepo,
                               GDPDataRepository gdpRepo,
                               InflationDataRepository infRepo,
                               PolicyRateDataRepository policyRepo,
                               SectorGrowthDataRepository sectorRepo,
                               MarketIndexDataRepository marketRepo) {
        this.cbuService = cbuService;
        this.statService = statService;
        this.aiService = aiService;
        this.riskService = riskService;
        this.fxRepo = fxRepo;
        this.gdpRepo = gdpRepo;
        this.infRepo = infRepo;
        this.policyRepo = policyRepo;
        this.sectorRepo = sectorRepo;
        this.marketRepo = marketRepo;
    }

    @PostMapping("/simulate")
    public ResponseEntity<?> simulateData() {
        cbuService.fetchAndSaveLatestRates();
        statService.initializeMockData();
        return ResponseEntity.ok(Map.of("message", "Full Uzbekistan Macroeconomic statistics successfully modeled/fetched."));
    }

    @GetMapping("/market-data")
    public ResponseEntity<?> getMarketData() {
        // Return full multi-sector dataset for UI visualization
        return ResponseEntity.ok(Map.of(
            "exchangeRates", fxRepo.findByCurrencyCodeOrderByDateAsc("USD"),
            "gdp", gdpRepo.findAllByOrderByYearAscQuarterAsc(),
            "inflation", infRepo.findAllByOrderByPeriodAsc(),
            "policyRates", policyRepo.findAllByOrderByDateAsc(),
            "sectorGrowth", sectorRepo.findAllByOrderByYearAscQuarterAsc(),
            "marketIndex", marketRepo.findAllByOrderByDateAsc()
        ));
    }

    @GetMapping("/predictions")
    public ResponseEntity<MacroForecastResponse> getPredictions() {
        // Unified UI forecast object bridging GDP, Inflation, FX, and Policy
        return ResponseEntity.ok(aiService.generateMacroForecast());
    }

    @GetMapping("/risk")
    public ResponseEntity<UzRiskResponse> getRisk() {
        // Probability-based stability evaluator
        return ResponseEntity.ok(riskService.generateRiskAssessment());
    }
}
