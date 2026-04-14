package com.ai.financial.service;

import com.ai.financial.entity.*;
import com.ai.financial.repository.*;
import com.ai.financial.dto.MacroForecastResponse;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatisticalForecastingUzService {

    private final GDPDataRepository gdpRepo;
    private final InflationDataRepository inflationRepo;
    private final ExchangeRateRepository exchangeRepo;
    private final PolicyRateDataRepository policyRepo;

    public StatisticalForecastingUzService(GDPDataRepository gdpRepo,
                                           InflationDataRepository inflationRepo,
                                           ExchangeRateRepository exchangeRepo,
                                           PolicyRateDataRepository policyRepo) {
        this.gdpRepo = gdpRepo;
        this.inflationRepo = inflationRepo;
        this.exchangeRepo = exchangeRepo;
        this.policyRepo = policyRepo;
    }

    public MacroForecastResponse generateMacroForecast() {
        MacroForecastResponse res = new MacroForecastResponse();
        res.setGdpForecast(forecastGDP());
        res.setInflationForecast(forecastInflation());
        res.setExchangeForecast(forecastExchange());
        res.setPolicyRateForecast(forecastPolicyRate());
        return res;
    }

    private MacroForecastResponse.ForecastResult forecastGDP() {
        List<GDPData> gdp = gdpRepo.findAllByOrderByYearAscQuarterAsc();
        MacroForecastResponse.ForecastResult r = new MacroForecastResponse.ForecastResult();
        r.setMethod("METHOD_WEIGHTED");
        if (gdp.isEmpty()) {
            r.setValue(0.0); r.setTrend("NO_DATA"); r.setDescription("DESC_NO_DATA");
            return r;
        }
        
        double weightSum = 0;
        double weightedGrowth = 0;
        for (int i = 0; i < gdp.size(); i++) {
            double w = (i + 1) * 0.1;
            weightedGrowth += (gdp.get(i).getGrowthRate() * w);
            weightSum += w;
        }
        double forecast = weightedGrowth / weightSum;
        
        r.setValue(forecast);
        r.setTrend(forecast > 5.5 ? "TREND_HIGH_GROWTH" : (forecast > 0 ? "TREND_STABLE_GROWTH" : "TREND_RECESSION"));
        r.setDescription("DESC_GDP");
        return r;
    }

    private MacroForecastResponse.ForecastResult forecastInflation() {
        List<InflationData> inf = inflationRepo.findAllByOrderByPeriodAsc();
        MacroForecastResponse.ForecastResult r = new MacroForecastResponse.ForecastResult();
        r.setMethod("METHOD_LINEAR");
        if (inf.size() < 3) {
            r.setValue(0.0); r.setTrend("NO_DATA"); r.setDescription("DESC_NO_DATA");
            return r;
        }

        SimpleRegression req = new SimpleRegression(true);
        for (int i = 0; i < inf.size(); i++) {
            req.addData(i, inf.get(i).getAnnualRate());
        }
        double nextVal = req.predict(inf.size());
        
        r.setValue(nextVal);
        r.setTrend(req.getSlope() > 0 ? "TREND_ACCELERATING" : "TREND_DECELERATING");
        r.setDescription("DESC_INF");
        return r;
    }

    private MacroForecastResponse.ForecastResult forecastExchange() {
        List<ExchangeRate> fx = exchangeRepo.findByCurrencyCodeOrderByDateAsc("USD");
        MacroForecastResponse.ForecastResult r = new MacroForecastResponse.ForecastResult();
        r.setMethod("METHOD_LINEAR");
        if (fx.size() < 5) {
            r.setValue(0.0); r.setTrend("NO_DATA"); r.setDescription("DESC_NO_DATA");
            return r;
        }

        SimpleRegression req = new SimpleRegression(true);
        for (int i = 0; i < fx.size(); i++) {
            req.addData(i, fx.get(i).getRate());
        }
        double nextVal = req.predict(fx.size() + 7); 
        
        r.setValue(nextVal);
        r.setTrend(req.getSlope() > 0 ? "TREND_DEPRECIATION" : "TREND_APPRECIATION");
        r.setDescription("DESC_FX");
        return r;
    }

    private MacroForecastResponse.ForecastResult forecastPolicyRate() {
        List<PolicyRateData> rates = policyRepo.findAllByOrderByDateAsc();
        MacroForecastResponse.ForecastResult r = new MacroForecastResponse.ForecastResult();
        r.setMethod("METHOD_SMA");
        if (rates.size() < 10) {
            r.setValue(13.5); r.setTrend("NO_DATA"); r.setDescription("DESC_NO_DATA");
            return r;
        }
        
        double sum = 0;
        int limit = Math.min(14, rates.size());
        for (int i = rates.size() - limit; i < rates.size(); i++) {
            sum += rates.get(i).getRate();
        }
        double sma = sum / limit;
        
        r.setValue(sma);
        r.setTrend(sma > 13.5 ? "TREND_TIGHTENING" : "TREND_EASING");
        r.setDescription("DESC_POLICY");
        return r;
    }
}
