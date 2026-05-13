package com.ai.financial.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BacktestResponse {

    private String generatedAt;
    private Double trainRatio;
    private Integer trainingWindow;
    private Integer testWindow;
    private String summary;
    private List<IndicatorBacktestResult> indicators = new ArrayList<>();

    @Data
    public static class IndicatorBacktestResult {
        private String indicator;
        private String productionMethod;
        private String baselineMethod;
        private String mlMethod;
        private Double mlMae;
        private Double mlMape;
        private Double mlRmse;
        private Double mlAccuracy;
        private Double mae;
        private Double mape;
        private Double rmse;
        private Double directionalAccuracy;
        private Double modelAccuracy;
        private Double benchmarkMae;
        private Double benchmarkMape;
        private Double benchmarkRmse;
        private Double benchmarkAccuracy;
        private Integer testPoints;
    }
}
