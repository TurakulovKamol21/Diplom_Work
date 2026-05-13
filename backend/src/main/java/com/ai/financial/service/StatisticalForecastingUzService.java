package com.ai.financial.service;

import com.ai.financial.dto.BacktestResponse;
import com.ai.financial.dto.MacroForecastResponse;
import com.ai.financial.dto.ScenarioAnalysisResponse;
import com.ai.financial.dto.UzRiskResponse;
import com.ai.financial.entity.ExchangeRate;
import com.ai.financial.entity.GDPData;
import com.ai.financial.entity.InflationData;
import com.ai.financial.entity.PolicyRateData;
import com.ai.financial.repository.ExchangeRateRepository;
import com.ai.financial.repository.GDPDataRepository;
import com.ai.financial.repository.InflationDataRepository;
import com.ai.financial.repository.PolicyRateDataRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticalForecastingUzService {

    private static final int GDP_HORIZON = 1;
    private static final int INFLATION_HORIZON = 1;
    private static final int EXCHANGE_HORIZON = 7;
    private static final int POLICY_HORIZON = 1;

    private final GDPDataRepository gdpRepo;
    private final InflationDataRepository inflationRepo;
    private final ExchangeRateRepository exchangeRepo;
    private final PolicyRateDataRepository policyRepo;
    private final MacroAnalyticsService analyticsService;
    private final RiskAnalysisUzService riskService;

    public StatisticalForecastingUzService(GDPDataRepository gdpRepo,
                                           InflationDataRepository inflationRepo,
                                           ExchangeRateRepository exchangeRepo,
                                           PolicyRateDataRepository policyRepo,
                                           MacroAnalyticsService analyticsService,
                                           RiskAnalysisUzService riskService) {
        this.gdpRepo = gdpRepo;
        this.inflationRepo = inflationRepo;
        this.exchangeRepo = exchangeRepo;
        this.policyRepo = policyRepo;
        this.analyticsService = analyticsService;
        this.riskService = riskService;
    }

    public MacroForecastResponse generateMacroForecast() {
        MacroForecastResponse response = new MacroForecastResponse();
        response.setGdpForecast(forecastGdp());
        response.setInflationForecast(forecastInflation());
        response.setExchangeForecast(forecastExchange());
        response.setPolicyRateForecast(forecastPolicyRate());
        return response;
    }

    public ScenarioAnalysisResponse generateScenarioAnalysis() {
        MacroForecastResponse baselineForecast = generateMacroForecast();
        ScenarioAnalysisResponse response = new ScenarioAnalysisResponse();
        response.setGeneratedAt(LocalDateTime.now().toString());
        response.setBestCase(buildScenario(
            "SCENARIO_BEST",
            "SCENARIO_BEST_DESC",
            baselineForecast,
            0.8,
            -1.2,
            -1.5,
            -0.5
        ));
        response.setBaselineCase(buildScenario(
            "SCENARIO_BASE",
            "SCENARIO_BASE_DESC",
            baselineForecast,
            0.0,
            0.0,
            0.0,
            0.0
        ));
        response.setWorstCase(buildScenario(
            "SCENARIO_WORST",
            "SCENARIO_WORST_DESC",
            baselineForecast,
            -1.2,
            1.8,
            2.8,
            0.75
        ));
        return response;
    }

    public BacktestResponse generateBacktestReport() {
        BacktestResponse response = new BacktestResponse();
        response.setGeneratedAt(LocalDateTime.now().toString());
        response.setTrainRatio(0.70);
        response.setSummary("BACKTEST_SUMMARY");

        MacroAnalyticsService.MethodComparison gdpComparison =
            analyticsService.buildBacktestComparison(extractGdpSeries(), "METHOD_WEIGHTED", 4, 3);
        MacroAnalyticsService.MethodComparison inflationComparison =
            analyticsService.buildBacktestComparison(extractInflationSeries(), "METHOD_LINEAR", 4, 3);
        MacroAnalyticsService.MethodComparison exchangeComparison =
            analyticsService.buildBacktestComparison(extractExchangeSeries(), "METHOD_LINEAR", 7, 5);
        MacroAnalyticsService.MethodComparison policyComparison =
            analyticsService.buildBacktestComparison(extractPolicyRateSeries(), "METHOD_SMA", 14, 4);

        BacktestResponse.IndicatorBacktestResult gdp = toIndicatorResult("GdpGrowth", gdpComparison);
        BacktestResponse.IndicatorBacktestResult inflation = toIndicatorResult("Inflation", inflationComparison);
        BacktestResponse.IndicatorBacktestResult exchange = toIndicatorResult("ExchangeRate", exchangeComparison);
        BacktestResponse.IndicatorBacktestResult policy = toIndicatorResult("PolicyRate", policyComparison);

        response.setIndicators(List.of(gdp, inflation, exchange, policy));
        response.setTrainingWindow(
            maxWindow(
                gdpComparison.getTrainingWindow(),
                inflationComparison.getTrainingWindow(),
                exchangeComparison.getTrainingWindow(),
                policyComparison.getTrainingWindow()
            )
        );
        response.setTestWindow(
            maxWindow(
                gdpComparison.getTestWindow(),
                inflationComparison.getTestWindow(),
                exchangeComparison.getTestWindow(),
                policyComparison.getTestWindow()
            )
        );
        return response;
    }

    private MacroForecastResponse.ForecastResult forecastGdp() {
        List<Double> series = extractGdpSeries();
        if (series.isEmpty()) {
            series = List.of(5.8);
        }
        MacroAnalyticsService.ForecastInsights insights =
            analyticsService.computeForecast(series, "METHOD_WEIGHTED", GDP_HORIZON, 4, 3);
        return toForecastResult(
            insights,
            chooseGdpTrend(insights.getPredictedValue()),
            series.size() < 4 ? "DESC_NO_DATA" : "DESC_GDP_AI"
        );
    }

    private MacroForecastResponse.ForecastResult forecastInflation() {
        List<Double> series = extractInflationSeries();
        if (series.isEmpty()) {
            series = List.of(8.5);
        }
        MacroAnalyticsService.ForecastInsights insights =
            analyticsService.computeForecast(series, "METHOD_LINEAR", INFLATION_HORIZON, 4, 3);
        return toForecastResult(
            insights,
            insights.getPredictedValue() >= insights.getPreviousValue() ? "TREND_ACCELERATING" : "TREND_DECELERATING",
            series.size() < 4 ? "DESC_NO_DATA" : "DESC_INF_AI"
        );
    }

    private MacroForecastResponse.ForecastResult forecastExchange() {
        List<Double> series = extractExchangeSeries();
        if (series.isEmpty()) {
            series = List.of(12450.0);
        }
        MacroAnalyticsService.ForecastInsights insights =
            analyticsService.computeForecast(series, "METHOD_LINEAR", EXCHANGE_HORIZON, 7, 5);
        return toForecastResult(
            insights,
            insights.getPredictedValue() >= insights.getPreviousValue() ? "TREND_DEPRECIATION" : "TREND_APPRECIATION",
            series.size() < 6 ? "DESC_NO_DATA" : "DESC_FX_AI"
        );
    }

    private MacroForecastResponse.ForecastResult forecastPolicyRate() {
        List<Double> series = extractPolicyRateSeries();
        if (series.isEmpty()) {
            series = List.of(13.5);
        }
        MacroAnalyticsService.ForecastInsights insights =
            analyticsService.computeForecast(series, "METHOD_SMA", POLICY_HORIZON, 14, 4);
        return toForecastResult(
            insights,
            insights.getPredictedValue() >= insights.getPreviousValue() ? "TREND_TIGHTENING" : "TREND_EASING",
            series.size() < 6 ? "DESC_NO_DATA" : "DESC_POLICY_AI"
        );
    }

    private ScenarioAnalysisResponse.ScenarioSnapshot buildScenario(String name,
                                                                   String description,
                                                                   MacroForecastResponse baselineForecast,
                                                                   double gdpDelta,
                                                                   double inflationDelta,
                                                                   double exchangeRateDeltaPercent,
                                                                   double policyDelta) {
        ScenarioAnalysisResponse.ScenarioSnapshot snapshot = new ScenarioAnalysisResponse.ScenarioSnapshot();
        snapshot.setName(name);
        snapshot.setDescription(description);
        snapshot.setGdpDeltaPercentagePoints(gdpDelta);
        snapshot.setInflationDeltaPercentagePoints(inflationDelta);
        snapshot.setExchangeRateDeltaPercent(exchangeRateDeltaPercent);
        snapshot.setPolicyRateDeltaPercentagePoints(policyDelta);

        MacroForecastResponse adjustedForecast = new MacroForecastResponse();
        adjustedForecast.setGdpForecast(adjustForecastResult(baselineForecast.getGdpForecast(), gdpDelta, false));
        adjustedForecast.setInflationForecast(adjustForecastResult(baselineForecast.getInflationForecast(), inflationDelta, false));
        adjustedForecast.setExchangeForecast(adjustForecastResult(baselineForecast.getExchangeForecast(), exchangeRateDeltaPercent, true));
        adjustedForecast.setPolicyRateForecast(adjustForecastResult(baselineForecast.getPolicyRateForecast(), policyDelta, false));
        snapshot.setMacroForecast(adjustedForecast);

        double scenarioInflation = adjustedForecast.getInflationForecast().getValue();
        double scenarioGdp = adjustedForecast.getGdpForecast().getValue();
        double scenarioExchangeVolatility = estimateScenarioFxVolatility(exchangeRateDeltaPercent);
        UzRiskResponse risk = riskService.evaluateScenarioRisk(scenarioInflation, scenarioExchangeVolatility, scenarioGdp);
        snapshot.setRisk(risk);
        return snapshot;
    }

    private MacroForecastResponse.ForecastResult adjustForecastResult(MacroForecastResponse.ForecastResult base,
                                                                      double delta,
                                                                      boolean percentBased) {
        MacroForecastResponse.ForecastResult result = new MacroForecastResponse.ForecastResult();
        double adjustedValue = percentBased
            ? base.getValue() * (1.0 + (delta / 100.0))
            : base.getValue() + delta;
        double previousValue = base.getPreviousValue() == null ? 0.0 : base.getPreviousValue();
        double adjustedLower = percentBased
            ? base.getLowerBound() * (1.0 + (delta / 100.0))
            : base.getLowerBound() + delta;
        double adjustedUpper = percentBased
            ? base.getUpperBound() * (1.0 + (delta / 100.0))
            : base.getUpperBound() + delta;
        double percentChange = previousValue == 0.0
            ? 0.0
            : ((adjustedValue - previousValue) / Math.abs(previousValue)) * 100.0;

        result.setValue(adjustedValue);
        result.setPreviousValue(previousValue);
        result.setPercentChange(percentChange);
        result.setProbability(base.getProbability());
        result.setConfidenceLevel(base.getConfidenceLevel());
        result.setLowerBound(Math.max(0.0, adjustedLower));
        result.setUpperBound(Math.max(adjustedValue, adjustedUpper));
        result.setModelAccuracy(base.getModelAccuracy());
        result.setBenchmarkAccuracy(base.getBenchmarkAccuracy());
        result.setForecastHorizon(base.getForecastHorizon());
        result.setTestPoints(base.getTestPoints());
        result.setTrend(base.getTrend());
        result.setMethod(base.getMethod());
        result.setBaselineMethod(base.getBaselineMethod());
        result.setMlMethod(base.getMlMethod());
        result.setDescription(base.getDescription());
        return result;
    }

    private MacroForecastResponse.ForecastResult toForecastResult(MacroAnalyticsService.ForecastInsights insights,
                                                                  String trend,
                                                                  String description) {
        MacroForecastResponse.ForecastResult result = new MacroForecastResponse.ForecastResult();
        result.setValue(insights.getPredictedValue());
        result.setPreviousValue(insights.getPreviousValue());
        result.setPercentChange(insights.getPercentChange());
        result.setProbability(insights.getProbability());
        result.setConfidenceLevel(insights.getConfidenceLevel());
        result.setLowerBound(insights.getLowerBound());
        result.setUpperBound(insights.getUpperBound());
        result.setModelAccuracy(insights.getModelAccuracy());
        result.setBenchmarkAccuracy(insights.getBenchmarkAccuracy());
        result.setForecastHorizon(insights.getForecastHorizon());
        result.setTestPoints(insights.getTestPoints());
        result.setTrend(trend);
        result.setMethod(insights.getMethod());
        result.setBaselineMethod(insights.getBaselineMethod());
        result.setMlMethod(insights.getMlMethod());
        result.setDescription(description);
        return result;
    }

    private BacktestResponse.IndicatorBacktestResult toIndicatorResult(String indicator,
                                                                       MacroAnalyticsService.MethodComparison comparison) {
        BacktestResponse.IndicatorBacktestResult result = new BacktestResponse.IndicatorBacktestResult();
        result.setIndicator(indicator);
        result.setProductionMethod(comparison.getProductionMethod());
        result.setBaselineMethod(comparison.getBaselineMethod());
        result.setMlMethod(comparison.getMlMethod());
        result.setMlMae(comparison.getMlMetrics().getMae());
        result.setMlMape(comparison.getMlMetrics().getMape());
        result.setMlRmse(comparison.getMlMetrics().getRmse());
        result.setMlAccuracy(comparison.getMlMetrics().getAccuracyScore());
        result.setMae(comparison.getProductionMetrics().getMae());
        result.setMape(comparison.getProductionMetrics().getMape());
        result.setRmse(comparison.getProductionMetrics().getRmse());
        result.setDirectionalAccuracy(comparison.getProductionMetrics().getDirectionalAccuracy());
        result.setModelAccuracy(comparison.getProductionMetrics().getAccuracyScore());
        result.setBenchmarkMae(comparison.getBenchmarkMetrics().getMae());
        result.setBenchmarkMape(comparison.getBenchmarkMetrics().getMape());
        result.setBenchmarkRmse(comparison.getBenchmarkMetrics().getRmse());
        result.setBenchmarkAccuracy(comparison.getBenchmarkMetrics().getAccuracyScore());
        result.setTestPoints(comparison.getTestWindow());
        return result;
    }

    private List<Double> extractGdpSeries() {
        List<Double> values = new ArrayList<>();
        for (GDPData row : gdpRepo.findAllByOrderByYearAscQuarterAsc()) {
            values.add(row.getGrowthRate());
        }
        return values;
    }

    private List<Double> extractInflationSeries() {
        List<Double> values = new ArrayList<>();
        for (InflationData row : inflationRepo.findAllByOrderByPeriodAsc()) {
            values.add(row.getAnnualRate());
        }
        return values;
    }

    private List<Double> extractExchangeSeries() {
        List<Double> values = new ArrayList<>();
        for (ExchangeRate row : exchangeRepo.findByCurrencyCodeOrderByDateAsc("USD")) {
            values.add(row.getRate());
        }
        return values;
    }

    private List<Double> extractPolicyRateSeries() {
        List<Double> values = new ArrayList<>();
        for (PolicyRateData row : policyRepo.findAllByOrderByDateAsc()) {
            values.add(row.getRate());
        }
        return values;
    }

    private String chooseGdpTrend(double forecast) {
        if (forecast > 5.5) {
            return "TREND_HIGH_GROWTH";
        }
        if (forecast > 0) {
            return "TREND_STABLE_GROWTH";
        }
        return "TREND_RECESSION";
    }

    private double estimateScenarioFxVolatility(double exchangeRateDeltaPercent) {
        List<ExchangeRate> rates = exchangeRepo.findByCurrencyCodeOrderByDateAsc("USD");
        if (rates.size() < 2) {
            return 4.0 + Math.abs(exchangeRateDeltaPercent);
        }

        int limit = Math.min(30, rates.size());
        double totalDiff = 0.0;
        for (int i = rates.size() - limit; i < rates.size(); i++) {
            totalDiff += Math.abs(rates.get(i).getDiff());
        }
        double baselineVolatility = totalDiff / limit;
        return baselineVolatility * (1.0 + Math.abs(exchangeRateDeltaPercent) / 100.0);
    }

    private int maxWindow(Integer... values) {
        int max = 0;
        for (Integer value : values) {
            if (value != null) {
                max = Math.max(max, value);
            }
        }
        return max;
    }
}
