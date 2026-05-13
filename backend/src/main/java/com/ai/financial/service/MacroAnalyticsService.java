package com.ai.financial.service;

import lombok.Data;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class MacroAnalyticsService {

    public static final String METHOD_KNN = "METHOD_KNN";
    public static final String METHOD_RIDGE = "METHOD_RIDGE";
    public static final String METHOD_HYBRID_AI = "METHOD_HYBRID_AI";

    private static final double EPSILON = 1e-6;
    private static final double RIDGE_LAMBDA = 1.25;

    public ForecastInsights computeForecast(List<Double> rawSeries,
                                            String baselineMethod,
                                            int forecastHorizon,
                                            int movingWindow,
                                            int knnWindow) {
        List<Double> series = sanitize(rawSeries);
        ForecastInsights insights = new ForecastInsights();
        insights.setMethod(baselineMethod);
        insights.setBaselineMethod(baselineMethod);
        insights.setForecastHorizon(forecastHorizon);
        insights.setTestPoints(0);

        if (series.isEmpty()) {
            insights.setPredictedValue(0.0);
            insights.setPreviousValue(0.0);
            insights.setPercentChange(0.0);
            insights.setProbability(0.0);
            insights.setConfidenceLevel(0.0);
            insights.setLowerBound(0.0);
            insights.setUpperBound(0.0);
            insights.setModelAccuracy(0.0);
            insights.setBenchmarkAccuracy(0.0);
            return insights;
        }

        MethodEvaluation evaluation = evaluateMethods(series, baselineMethod, forecastHorizon, movingWindow, knnWindow);
        double previousValue = series.get(series.size() - 1);
        double predictedValue = evaluation.getProductionPrediction();
        double rmse = evaluation.getProductionMetrics().isValid()
            ? evaluation.getProductionMetrics().getRmse()
            : estimateNoise(series);
        double margin = Math.max(Math.abs(previousValue) * 0.01, rmse * 1.645);
        if (margin < 0.15) {
            margin = Math.max(0.15, Math.abs(previousValue) * 0.02);
        }

        double percentChange = previousValue == 0.0
            ? 0.0
            : ((predictedValue - previousValue) / Math.abs(previousValue)) * 100.0;
        double trendStrength = Math.abs(predictedValue - previousValue) / (margin + EPSILON);
        double modelAccuracy = evaluation.getProductionMetrics().isValid()
            ? evaluation.getProductionMetrics().getAccuracyScore()
            : 0.55;
        double benchmarkAccuracy = evaluation.getBenchmarkMetrics().isValid()
            ? evaluation.getBenchmarkMetrics().getAccuracyScore()
            : modelAccuracy;
        double probability = clamp(0.51, 0.97,
            0.45 + (modelAccuracy * 0.35) + (Math.tanh(trendStrength) * 0.17));
        double confidenceLevel = clamp(0.60, 0.95,
            0.58 + (modelAccuracy * 0.32) + (Math.min(series.size(), 24) / 24.0 * 0.05));

        insights.setPredictedValue(predictedValue);
        insights.setPreviousValue(previousValue);
        insights.setPercentChange(percentChange);
        insights.setProbability(probability);
        insights.setConfidenceLevel(confidenceLevel);
        insights.setLowerBound(Math.max(0.0, predictedValue - margin));
        insights.setUpperBound(predictedValue + margin);
        insights.setModelAccuracy(modelAccuracy);
        insights.setBenchmarkAccuracy(benchmarkAccuracy);
        insights.setMethod(evaluation.getProductionMethod());
        insights.setMlMethod(evaluation.getSelectedMlMethod());
        insights.setTestPoints(evaluation.getProductionMetrics().getTestPoints());
        return insights;
    }

    public MethodComparison buildBacktestComparison(List<Double> rawSeries,
                                                    String baselineMethod,
                                                    int movingWindow,
                                                    int knnWindow) {
        List<Double> series = sanitize(rawSeries);
        MethodEvaluation evaluation = evaluateMethods(series, baselineMethod, 1, movingWindow, knnWindow);

        MethodComparison comparison = new MethodComparison();
        comparison.setProductionMethod(evaluation.getProductionMethod());
        comparison.setBaselineMethod(baselineMethod);
        comparison.setMlMethod(evaluation.getSelectedMlMethod());
        comparison.setProductionMetrics(evaluation.getProductionMetrics());
        comparison.setBenchmarkMetrics(evaluation.getBenchmarkMetrics());
        comparison.setMlMetrics(evaluation.getSelectedMlMetrics());
        comparison.setTrainingWindow(Math.max(series.size() - evaluation.getProductionMetrics().getTestPoints(), 0));
        comparison.setTestWindow(evaluation.getProductionMetrics().getTestPoints());
        return comparison;
    }

    private MethodEvaluation evaluateMethods(List<Double> series,
                                             String baselineMethod,
                                             int forecastHorizon,
                                             int movingWindow,
                                             int knnWindow) {
        MethodEvaluation evaluation = new MethodEvaluation();
        BacktestMetrics invalidMetrics = BacktestMetrics.invalid();

        double baselinePrediction = forecastByMethod(series, baselineMethod, forecastHorizon, movingWindow, knnWindow);
        BacktestMetrics baselineMetrics = backtestMethod(series, baselineMethod, movingWindow, knnWindow);

        MlCandidate bestMlCandidate = selectBestMlCandidate(
            buildMlCandidate(METHOD_KNN, series, forecastHorizon, movingWindow, knnWindow),
            buildMlCandidate(METHOD_RIDGE, series, forecastHorizon, movingWindow, knnWindow)
        );

        evaluation.setBenchmarkMetrics(baselineMetrics);
        evaluation.setSelectedMlMethod(bestMlCandidate.getMethod());
        evaluation.setSelectedMlMetrics(bestMlCandidate.getMetrics());

        if (baselineMetrics.isValid() && bestMlCandidate.getMetrics().isValid()) {
            double baselineWeight = inverseErrorWeight(baselineMetrics.getMape());
            double mlWeight = inverseErrorWeight(bestMlCandidate.getMetrics().getMape());

            BacktestMetrics hybridMetrics = scoreHybridMetrics(
                baselineMetrics,
                bestMlCandidate.getMetrics(),
                baselineWeight,
                mlWeight
            );

            evaluation.setProductionMethod(METHOD_HYBRID_AI);
            evaluation.setProductionPrediction(weightedAverage(
                baselinePrediction,
                bestMlCandidate.getPrediction(),
                baselineWeight,
                mlWeight
            ));
            evaluation.setProductionMetrics(hybridMetrics);
            return evaluation;
        }

        if (baselineMetrics.isValid()) {
            evaluation.setProductionMethod(baselineMethod);
            evaluation.setProductionPrediction(baselinePrediction);
            evaluation.setProductionMetrics(baselineMetrics);
            return evaluation;
        }

        if (bestMlCandidate.getMetrics().isValid()) {
            evaluation.setProductionMethod(bestMlCandidate.getMethod());
            evaluation.setProductionPrediction(bestMlCandidate.getPrediction());
            evaluation.setProductionMetrics(bestMlCandidate.getMetrics());
            return evaluation;
        }

        evaluation.setProductionMethod(baselineMethod);
        evaluation.setProductionPrediction(baselinePrediction);
        evaluation.setProductionMetrics(invalidMetrics);
        return evaluation;
    }

    private MlCandidate buildMlCandidate(String method,
                                         List<Double> series,
                                         int forecastHorizon,
                                         int movingWindow,
                                         int knnWindow) {
        double prediction = forecastByMethod(series, method, forecastHorizon, movingWindow, knnWindow);
        BacktestMetrics metrics = backtestMethod(series, method, movingWindow, knnWindow);
        return new MlCandidate(method, prediction, metrics);
    }

    private MlCandidate selectBestMlCandidate(MlCandidate... candidates) {
        MlCandidate fallback = new MlCandidate(null, 0.0, BacktestMetrics.invalid());
        MlCandidate best = fallback;

        for (MlCandidate candidate : candidates) {
            if (candidate == null || !candidate.getMetrics().isValid()) {
                continue;
            }

            if (!best.getMetrics().isValid() || compareMlCandidates(candidate, best) < 0) {
                best = candidate;
            }
        }

        return best;
    }

    private int compareMlCandidates(MlCandidate left, MlCandidate right) {
        int mape = Double.compare(left.getMetrics().getMape(), right.getMetrics().getMape());
        if (mape != 0) {
            return mape;
        }

        int rmse = Double.compare(left.getMetrics().getRmse(), right.getMetrics().getRmse());
        if (rmse != 0) {
            return rmse;
        }

        return Double.compare(right.getMetrics().getDirectionalAccuracy(), left.getMetrics().getDirectionalAccuracy());
    }

    private BacktestMetrics backtestMethod(List<Double> series,
                                           String method,
                                           int movingWindow,
                                           int knnWindow) {
        List<Double> cleaned = sanitize(series);
        int minTrainWindow = minimumTrainingWindow(cleaned, movingWindow, knnWindow);
        if (cleaned.size() < minTrainWindow + 2) {
            return BacktestMetrics.invalid();
        }

        int defaultTestWindow = Math.max(3, (int) Math.ceil(cleaned.size() * 0.30));
        int startIndex = Math.max(minTrainWindow, cleaned.size() - defaultTestWindow);

        List<Double> predictions = new ArrayList<>();
        List<Double> actuals = new ArrayList<>();
        List<Double> previousValues = new ArrayList<>();

        for (int i = startIndex; i < cleaned.size(); i++) {
            List<Double> trainingSlice = new ArrayList<>(cleaned.subList(0, i));
            double predicted = forecastByMethod(trainingSlice, method, 1, movingWindow, knnWindow);
            predictions.add(predicted);
            actuals.add(cleaned.get(i));
            previousValues.add(cleaned.get(i - 1));
        }

        return scorePredictions(method, predictions, actuals, previousValues);
    }

    private BacktestMetrics scoreHybridMetrics(BacktestMetrics baselineMetrics,
                                               BacktestMetrics mlMetrics,
                                               double baselineWeight,
                                               double mlWeight) {
        int commonSize = Math.min(
            Math.min(baselineMetrics.getPredictions().size(), mlMetrics.getPredictions().size()),
            Math.min(baselineMetrics.getActuals().size(), baselineMetrics.getPreviousValues().size())
        );
        if (commonSize == 0) {
            return BacktestMetrics.invalid();
        }

        List<Double> hybridPredictions = combinePredictions(
            takeLast(baselineMetrics.getPredictions(), commonSize),
            takeLast(mlMetrics.getPredictions(), commonSize),
            baselineWeight,
            mlWeight
        );

        return scorePredictions(
            METHOD_HYBRID_AI,
            hybridPredictions,
            takeLast(baselineMetrics.getActuals(), commonSize),
            takeLast(baselineMetrics.getPreviousValues(), commonSize)
        );
    }

    private BacktestMetrics scorePredictions(String method,
                                             List<Double> predictions,
                                             List<Double> actuals,
                                             List<Double> previousValues) {
        BacktestMetrics metrics = new BacktestMetrics();
        metrics.setMethod(method);
        metrics.setPredictions(predictions);
        metrics.setActuals(actuals);
        metrics.setPreviousValues(previousValues);
        metrics.setTestPoints(predictions.size());

        if (predictions.isEmpty() || actuals.isEmpty() || predictions.size() != actuals.size()) {
            metrics.setValid(false);
            metrics.setMae(0.0);
            metrics.setMape(0.0);
            metrics.setRmse(0.0);
            metrics.setDirectionalAccuracy(0.0);
            metrics.setAccuracyScore(0.0);
            return metrics;
        }

        double absErrorSum = 0.0;
        double pctErrorSum = 0.0;
        double squaredErrorSum = 0.0;
        int pctCount = 0;
        int correctDirections = 0;

        for (int i = 0; i < predictions.size(); i++) {
            double predicted = predictions.get(i);
            double actual = actuals.get(i);
            double previous = previousValues.get(i);
            double error = predicted - actual;
            absErrorSum += Math.abs(error);
            squaredErrorSum += error * error;

            if (Math.abs(actual) > EPSILON) {
                pctErrorSum += Math.abs(error / actual) * 100.0;
                pctCount++;
            }

            double predictedDirection = Math.signum(predicted - previous);
            double actualDirection = Math.signum(actual - previous);
            if (predictedDirection == actualDirection) {
                correctDirections++;
            }
        }

        double mae = absErrorSum / predictions.size();
        double mape = pctCount == 0 ? 0.0 : pctErrorSum / pctCount;
        double rmse = Math.sqrt(squaredErrorSum / predictions.size());
        double directionalAccuracy = (double) correctDirections / predictions.size();
        double accuracyScore = clamp(0.0, 1.0,
            ((1.0 - Math.min(mape, 100.0) / 100.0) * 0.75) + (directionalAccuracy * 0.25));

        metrics.setValid(true);
        metrics.setMae(mae);
        metrics.setMape(mape);
        metrics.setRmse(rmse);
        metrics.setDirectionalAccuracy(directionalAccuracy);
        metrics.setAccuracyScore(accuracyScore);
        return metrics;
    }

    private List<Double> combinePredictions(List<Double> baselinePredictions,
                                            List<Double> mlPredictions,
                                            double baselineWeight,
                                            double mlWeight) {
        List<Double> combined = new ArrayList<>();
        int size = Math.min(baselinePredictions.size(), mlPredictions.size());
        for (int i = 0; i < size; i++) {
            combined.add(weightedAverage(
                baselinePredictions.get(i),
                mlPredictions.get(i),
                baselineWeight,
                mlWeight
            ));
        }
        return combined;
    }

    private double forecastByMethod(List<Double> series,
                                    String method,
                                    int forecastHorizon,
                                    int movingWindow,
                                    int knnWindow) {
        List<Double> working = new ArrayList<>(sanitize(series));
        if (working.isEmpty()) {
            return 0.0;
        }

        int horizon = Math.max(1, forecastHorizon);
        double nextValue = working.get(working.size() - 1);
        for (int step = 0; step < horizon; step++) {
            nextValue = predictOneStep(working, method, movingWindow, knnWindow);
            working.add(nextValue);
        }
        return nextValue;
    }

    private double predictOneStep(List<Double> series,
                                  String method,
                                  int movingWindow,
                                  int knnWindow) {
        if (series.isEmpty()) {
            return 0.0;
        }

        return switch (method) {
            case "METHOD_WEIGHTED" -> predictWeightedAverage(series);
            case "METHOD_SMA" -> predictMovingAverage(series, movingWindow);
            case METHOD_KNN -> predictKnn(series, knnWindow);
            case METHOD_RIDGE -> predictRidgeRegression(series, movingWindow, knnWindow);
            case "METHOD_LINEAR" -> predictLinearRegression(series);
            default -> series.get(series.size() - 1);
        };
    }

    private double predictWeightedAverage(List<Double> series) {
        double weightedSum = 0.0;
        double weightSum = 0.0;
        for (int i = 0; i < series.size(); i++) {
            double weight = i + 1;
            weightedSum += series.get(i) * weight;
            weightSum += weight;
        }
        return weightSum == 0.0 ? series.get(series.size() - 1) : weightedSum / weightSum;
    }

    private double predictMovingAverage(List<Double> series, int movingWindow) {
        int limit = Math.max(2, Math.min(movingWindow, series.size()));
        double sum = 0.0;
        for (int i = series.size() - limit; i < series.size(); i++) {
            sum += series.get(i);
        }
        return sum / limit;
    }

    private double predictLinearRegression(List<Double> series) {
        if (series.size() < 2) {
            return series.get(series.size() - 1);
        }

        SimpleRegression regression = new SimpleRegression(true);
        for (int i = 0; i < series.size(); i++) {
            regression.addData(i, series.get(i));
        }
        return regression.predict(series.size());
    }

    private double predictKnn(List<Double> series, int knnWindow) {
        int window = Math.max(2, Math.min(knnWindow, series.size() - 1));
        if (series.size() < window + 2) {
            return series.get(series.size() - 1);
        }

        List<Double> targetSignature = signature(series.subList(series.size() - window, series.size()));
        List<Neighbor> neighbors = new ArrayList<>();

        for (int i = 0; i + window < series.size(); i++) {
            List<Double> candidateWindow = series.subList(i, i + window);
            double candidateTarget = series.get(i + window);
            double distance = euclideanDistance(signature(candidateWindow), targetSignature);
            neighbors.add(new Neighbor(distance, candidateTarget));
        }

        neighbors.sort(Comparator.comparingDouble(Neighbor::getDistance));
        int k = Math.min(3, neighbors.size());
        if (k == 0) {
            return series.get(series.size() - 1);
        }

        double weightedSum = 0.0;
        double weightSum = 0.0;
        for (int i = 0; i < k; i++) {
            Neighbor neighbor = neighbors.get(i);
            double weight = 1.0 / (neighbor.getDistance() + EPSILON);
            weightedSum += neighbor.getTargetValue() * weight;
            weightSum += weight;
        }

        return weightSum == 0.0 ? series.get(series.size() - 1) : weightedSum / weightSum;
    }

    private double predictRidgeRegression(List<Double> series,
                                          int movingWindow,
                                          int knnWindow) {
        int lagCount = determineRidgeLagCount(series, movingWindow, knnWindow);
        if (series.size() < lagCount + 3) {
            return series.get(series.size() - 1);
        }

        List<double[]> featureRows = new ArrayList<>();
        List<Double> targets = new ArrayList<>();

        for (int i = lagCount; i < series.size(); i++) {
            List<Double> historyWindow = series.subList(i - lagCount, i);
            featureRows.add(buildRidgeFeatures(historyWindow));
            targets.add(series.get(i));
        }

        RidgeModel model = fitRidgeModel(featureRows, targets);
        if (model == null) {
            return series.get(series.size() - 1);
        }

        double prediction = model.predict(buildRidgeFeatures(series.subList(series.size() - lagCount, series.size())));
        return Double.isFinite(prediction) ? prediction : series.get(series.size() - 1);
    }

    private RidgeModel fitRidgeModel(List<double[]> features, List<Double> targets) {
        if (features.isEmpty() || features.size() != targets.size()) {
            return null;
        }

        int rowCount = features.size();
        int featureCount = features.get(0).length;
        double[] means = new double[featureCount];
        double[] stdDevs = new double[featureCount];

        for (int column = 0; column < featureCount; column++) {
            double sum = 0.0;
            for (double[] row : features) {
                sum += row[column];
            }
            means[column] = sum / rowCount;

            double variance = 0.0;
            for (double[] row : features) {
                double diff = row[column] - means[column];
                variance += diff * diff;
            }
            stdDevs[column] = Math.sqrt(variance / rowCount);
            if (stdDevs[column] < EPSILON) {
                stdDevs[column] = 1.0;
            }
        }

        double[][] design = new double[rowCount][featureCount + 1];
        for (int i = 0; i < rowCount; i++) {
            design[i][0] = 1.0;
            for (int column = 0; column < featureCount; column++) {
                design[i][column + 1] = (features.get(i)[column] - means[column]) / stdDevs[column];
            }
        }

        RealMatrix x = new Array2DRowRealMatrix(design);
        RealVector y = new ArrayRealVector(targets.stream().mapToDouble(Double::doubleValue).toArray());
        RealMatrix xtx = x.transpose().multiply(x);

        for (int i = 1; i < xtx.getRowDimension(); i++) {
            xtx.addToEntry(i, i, RIDGE_LAMBDA);
        }

        RealVector xty = x.transpose().operate(y);
        DecompositionSolver solver = new SingularValueDecomposition(xtx).getSolver();
        if (!solver.isNonSingular()) {
            return null;
        }

        RealVector coefficients = solver.solve(xty);
        return new RidgeModel(coefficients.toArray(), means, stdDevs);
    }

    private double[] buildRidgeFeatures(List<Double> window) {
        int size = window.size();
        double[] features = new double[size + 4];

        for (int i = 0; i < size; i++) {
            features[i] = window.get(size - 1 - i);
        }

        features[size] = average(window);
        features[size + 1] = window.get(size - 1) - window.get(0);
        features[size + 2] = averageAbsoluteChange(window);
        features[size + 3] = computeWindowSlope(window);
        return features;
    }

    private int determineRidgeLagCount(List<Double> series, int movingWindow, int knnWindow) {
        int preferredWindow = Math.min(6, Math.max(4, Math.max(knnWindow + 1, Math.min(movingWindow, 6))));
        return Math.max(3, Math.min(preferredWindow, Math.max(3, series.size() - 2)));
    }

    private int minimumTrainingWindow(List<Double> series, int movingWindow, int knnWindow) {
        int ridgeLagCount = determineRidgeLagCount(series, movingWindow, knnWindow);
        return Math.max(8, Math.max(Math.max(movingWindow, knnWindow + 2), ridgeLagCount + 2));
    }

    private double average(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        for (Double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    private double averageAbsoluteChange(List<Double> values) {
        if (values.size() < 2) {
            return 0.0;
        }

        double sum = 0.0;
        for (int i = 1; i < values.size(); i++) {
            sum += Math.abs(values.get(i) - values.get(i - 1));
        }
        return sum / (values.size() - 1);
    }

    private double computeWindowSlope(List<Double> values) {
        if (values.size() < 2) {
            return 0.0;
        }

        SimpleRegression regression = new SimpleRegression(true);
        for (int i = 0; i < values.size(); i++) {
            regression.addData(i, values.get(i));
        }
        return regression.getSlope();
    }

    private List<Double> signature(List<Double> window) {
        List<Double> signature = new ArrayList<>();
        if (window.size() < 2) {
            signature.add(0.0);
            return signature;
        }

        for (int i = 1; i < window.size(); i++) {
            double previous = window.get(i - 1);
            double current = window.get(i);
            signature.add((current - previous) / (Math.abs(previous) + EPSILON));
        }
        return signature;
    }

    private double euclideanDistance(List<Double> left, List<Double> right) {
        int size = Math.min(left.size(), right.size());
        double sum = 0.0;
        for (int i = 0; i < size; i++) {
            double diff = left.get(i) - right.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private double estimateNoise(List<Double> series) {
        if (series.size() < 2) {
            return Math.max(0.25, Math.abs(series.get(series.size() - 1)) * 0.02);
        }

        double sum = 0.0;
        for (int i = 1; i < series.size(); i++) {
            sum += Math.abs(series.get(i) - series.get(i - 1));
        }
        return sum / (series.size() - 1);
    }

    private List<Double> sanitize(List<Double> rawSeries) {
        List<Double> cleaned = new ArrayList<>();
        if (rawSeries == null) {
            return cleaned;
        }

        for (Double value : rawSeries) {
            if (Objects.nonNull(value)) {
                cleaned.add(value);
            }
        }
        return cleaned;
    }

    private List<Double> takeLast(List<Double> values, int count) {
        if (values == null || values.isEmpty() || count <= 0) {
            return new ArrayList<>();
        }
        int start = Math.max(0, values.size() - count);
        return new ArrayList<>(values.subList(start, values.size()));
    }

    private double inverseErrorWeight(double error) {
        return 1.0 / Math.max(error, 0.25);
    }

    private double weightedAverage(double left, double right, double leftWeight, double rightWeight) {
        double totalWeight = leftWeight + rightWeight;
        if (totalWeight == 0.0) {
            return (left + right) / 2.0;
        }
        return ((left * leftWeight) + (right * rightWeight)) / totalWeight;
    }

    private double clamp(double min, double max, double value) {
        return Math.max(min, Math.min(max, value));
    }

    @Data
    public static class ForecastInsights {
        private double predictedValue;
        private double previousValue;
        private double percentChange;
        private double probability;
        private double confidenceLevel;
        private double lowerBound;
        private double upperBound;
        private double modelAccuracy;
        private double benchmarkAccuracy;
        private int forecastHorizon;
        private int testPoints;
        private String method;
        private String baselineMethod;
        private String mlMethod;
    }

    @Data
    public static class MethodComparison {
        private String productionMethod;
        private String baselineMethod;
        private String mlMethod;
        private int trainingWindow;
        private int testWindow;
        private BacktestMetrics productionMetrics = BacktestMetrics.invalid();
        private BacktestMetrics benchmarkMetrics = BacktestMetrics.invalid();
        private BacktestMetrics mlMetrics = BacktestMetrics.invalid();
    }

    @Data
    public static class BacktestMetrics {
        private boolean valid;
        private String method;
        private double mae;
        private double mape;
        private double rmse;
        private double directionalAccuracy;
        private double accuracyScore;
        private int testPoints;
        private List<Double> predictions = new ArrayList<>();
        private List<Double> actuals = new ArrayList<>();
        private List<Double> previousValues = new ArrayList<>();

        public static BacktestMetrics invalid() {
            BacktestMetrics metrics = new BacktestMetrics();
            metrics.setValid(false);
            metrics.setMae(0.0);
            metrics.setMape(0.0);
            metrics.setRmse(0.0);
            metrics.setDirectionalAccuracy(0.0);
            metrics.setAccuracyScore(0.0);
            metrics.setTestPoints(0);
            return metrics;
        }
    }

    @Data
    private static class MethodEvaluation {
        private String productionMethod;
        private double productionPrediction;
        private String selectedMlMethod;
        private BacktestMetrics productionMetrics = BacktestMetrics.invalid();
        private BacktestMetrics benchmarkMetrics = BacktestMetrics.invalid();
        private BacktestMetrics selectedMlMetrics = BacktestMetrics.invalid();
    }

    @Data
    private static class Neighbor {
        private final double distance;
        private final double targetValue;
    }

    @Data
    private static class MlCandidate {
        private final String method;
        private final double prediction;
        private final BacktestMetrics metrics;
    }

    private static class RidgeModel {
        private final double[] coefficients;
        private final double[] featureMeans;
        private final double[] featureStdDevs;

        private RidgeModel(double[] coefficients, double[] featureMeans, double[] featureStdDevs) {
            this.coefficients = coefficients;
            this.featureMeans = featureMeans;
            this.featureStdDevs = featureStdDevs;
        }

        private double predict(double[] rawFeatures) {
            double prediction = coefficients[0];
            for (int i = 0; i < rawFeatures.length; i++) {
                double standardized = (rawFeatures[i] - featureMeans[i]) / featureStdDevs[i];
                prediction += coefficients[i + 1] * standardized;
            }
            return prediction;
        }
    }
}
