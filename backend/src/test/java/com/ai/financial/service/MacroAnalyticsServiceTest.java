package com.ai.financial.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MacroAnalyticsServiceTest {

    private final MacroAnalyticsService analyticsService = new MacroAnalyticsService();

    @Test
    void computeForecastShouldExposeProbabilityAndConfidenceInterval() {
        List<Double> series = List.of(5.2, 5.4, 5.7, 5.9, 6.1, 6.2, 6.4);

        MacroAnalyticsService.ForecastInsights forecast =
            analyticsService.computeForecast(series, "METHOD_WEIGHTED", 1, 4, 3);

        assertTrue(forecast.getPredictedValue() > 0.0);
        assertTrue(forecast.getProbability() >= 0.0 && forecast.getProbability() <= 1.0);
        assertTrue(forecast.getConfidenceLevel() >= 0.0 && forecast.getConfidenceLevel() <= 1.0);
        assertTrue(forecast.getUpperBound() >= forecast.getLowerBound());
        assertTrue(forecast.getModelAccuracy() >= 0.0 && forecast.getModelAccuracy() <= 1.0);
    }

    @Test
    void backtestShouldReturnNonNegativeMetrics() {
        List<Double> series = List.of(12410.0, 12425.0, 12431.0, 12455.0, 12468.0, 12480.0, 12502.0, 12518.0, 12540.0, 12561.0);

        MacroAnalyticsService.MethodComparison comparison =
            analyticsService.buildBacktestComparison(series, "METHOD_LINEAR", 5, 4);

        assertTrue(comparison.getProductionMetrics().isValid());
        assertTrue(comparison.getProductionMetrics().getMae() >= 0.0);
        assertTrue(comparison.getProductionMetrics().getMape() >= 0.0);
        assertTrue(comparison.getProductionMetrics().getRmse() >= 0.0);
        assertTrue(comparison.getProductionMetrics().getDirectionalAccuracy() >= 0.0
            && comparison.getProductionMetrics().getDirectionalAccuracy() <= 1.0);
    }

    @Test
    void backtestShouldSelectARealMlCandidate() {
        List<Double> series = List.of(
            5.0, 5.4, 5.8, 5.6, 6.0, 6.5, 6.9, 6.7,
            7.1, 7.6, 8.0, 7.8, 8.2, 8.6, 9.0, 8.9
        );

        MacroAnalyticsService.MethodComparison comparison =
            analyticsService.buildBacktestComparison(series, "METHOD_WEIGHTED", 4, 3);

        assertNotNull(comparison.getMlMethod());
        assertTrue(
            MacroAnalyticsService.METHOD_KNN.equals(comparison.getMlMethod())
                || MacroAnalyticsService.METHOD_RIDGE.equals(comparison.getMlMethod())
        );
        assertTrue(comparison.getMlMetrics().isValid());
        assertTrue(comparison.getMlMetrics().getMape() >= 0.0);
        assertTrue(comparison.getMlMetrics().getRmse() >= 0.0);
    }
}
