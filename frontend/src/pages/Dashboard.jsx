import React, { useEffect, useState } from 'react';
import {
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
  Legend
} from 'recharts';
import {
  Activity,
  Shield,
  TrendingUp,
  RefreshCw,
  BarChart2,
  FlaskConical,
  Target
} from 'lucide-react';
import { dashboardService } from '../api';
import { useLanguage } from '../context/LanguageContext';

const formatValue = (value, digits = 1) => (
  typeof value === 'number' && Number.isFinite(value) ? value.toFixed(digits) : '--'
);

const formatPercent = (value, digits = 1) => (
  typeof value === 'number' && Number.isFinite(value) ? `${value.toFixed(digits)}%` : '--'
);

const formatProbability = (value, digits = 0) => (
  typeof value === 'number' && Number.isFinite(value) ? `${(value * 100).toFixed(digits)}%` : '--'
);

const formatComparison = (modelValue, benchmarkValue, formatter) => (
  `${formatter(modelValue)} / ${formatter(benchmarkValue)}`
);

const getRiskBadgeClass = (riskLevel) => {
  if (!riskLevel) return 'RISK_MEDIUM';
  return riskLevel;
};

export default function Dashboard() {
  const { t } = useLanguage();
  const [data, setData] = useState(null);
  const [forecast, setForecast] = useState(null);
  const [risk, setRisk] = useState(null);
  const [scenarios, setScenarios] = useState(null);
  const [backtest, setBacktest] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchDashboard();
  }, []);

  const fetchDashboard = async () => {
    try {
      setLoading(true);
      const results = await Promise.allSettled([
        dashboardService.marketData(),
        dashboardService.predictions(),
        dashboardService.risk(),
        dashboardService.scenarioAnalysis(),
        dashboardService.backtest()
      ]);

      const [marketData, predictions, riskData, scenarioData, backtestData] = results;
      if (marketData.status === 'fulfilled') setData(marketData.value.data);
      if (predictions.status === 'fulfilled') setForecast(predictions.value.data);
      if (riskData.status === 'fulfilled') setRisk(riskData.value.data);
      if (scenarioData.status === 'fulfilled') setScenarios(scenarioData.value.data);
      if (backtestData.status === 'fulfilled') setBacktest(backtestData.value.data);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const simulateData = async () => {
    try {
      setLoading(true);
      await dashboardService.simulate();
      await fetchDashboard();
    } catch (error) {
      console.error(error);
      setLoading(false);
    }
  };

  const getRiskSummary = (riskPayload) => {
    if (!riskPayload) return '';
    return t(riskPayload.summary)
      .replace('{score}', formatValue(riskPayload.economicStabilityScore, 1))
      .replace('{inf}', formatProbability(riskPayload.inflationSurgeProbability, 0).replace('%', ''))
      .replace('{fx}', formatProbability(riskPayload.currencyDevaluationProbability, 0).replace('%', ''))
      .replace('{rec}', formatProbability(riskPayload.recessionProbability, 0).replace('%', ''));
  };

  const forecastCards = forecast ? [
    {
      key: 'gdp',
      label: t('GdpGrowth'),
      color: 'var(--accent-green)',
      suffix: '%',
      data: forecast.gdpForecast
    },
    {
      key: 'inflation',
      label: t('Inflation'),
      color: 'var(--accent-amber)',
      suffix: '%',
      data: forecast.inflationForecast
    },
    {
      key: 'policy',
      label: t('PolicyRate'),
      color: 'var(--accent-purple)',
      suffix: '%',
      data: forecast.policyRateForecast
    },
    {
      key: 'fx',
      label: t('ExchangeRate'),
      color: 'var(--accent-blue)',
      suffix: ' UZS',
      digits: 0,
      data: forecast.exchangeForecast
    }
  ] : [];

  const scenarioCards = scenarios
    ? [scenarios.bestCase, scenarios.baselineCase, scenarios.worstCase].filter(Boolean)
    : [];

  if (!data && !loading) {
    return (
      <div className="fade-in card" style={{ textAlign: 'center', padding: '60px 20px' }}>
        <p style={{ color: 'var(--text-secondary)' }}>{t('noData')}</p>
        <button className="btn btn-primary" onClick={simulateData} style={{ marginTop: 20 }}>
          {t('simulateData')}
        </button>
      </div>
    );
  }

  return (
    <div className="fade-in">
      <div
        className="card"
        style={{
          marginBottom: 24,
          borderLeft: '4px solid var(--accent-blue)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: 16,
          flexWrap: 'wrap'
        }}
      >
        <div>
          <h2 style={{ fontSize: 22, fontWeight: 700, marginBottom: 8, display: 'flex', alignItems: 'center', gap: 10 }}>
            <Activity className="icon-blue" />
            {t('macroPlatformTitle')}
          </h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: 14, maxWidth: 720 }}>
            {t('macroPlatformSubtitle')}
          </p>
        </div>
        <button className="btn btn-primary" onClick={simulateData} disabled={loading}>
          <RefreshCw size={16} className={loading ? 'spin' : ''} /> {t('refresh')}
        </button>
      </div>

      {forecast && (
        <div className="kpi-grid" style={{ marginBottom: 24 }}>
          {forecastCards.map((item) => (
            <div key={item.key} className="kpi-card" style={{ borderTop: `4px solid ${item.color}` }}>
              <div className="kpi-label">{item.label}</div>
              <div className="kpi-val" style={{ color: item.color }}>
                {formatValue(item.data?.value, item.digits ?? 1)}
                <span style={{ fontSize: 14, marginLeft: 4 }}>{item.suffix}</span>
              </div>
              <div className="kpi-sub">{t(item.data?.trend)}</div>
              <div className="metric-list" style={{ marginTop: 14 }}>
                <div className="metric-row">
                  <span>{t('percentChange')}</span>
                  <strong>{formatPercent(item.data?.percentChange, 2)}</strong>
                </div>
                <div className="metric-row">
                  <span>{t('forecastProbability')}</span>
                  <strong>{formatProbability(item.data?.probability, 0)}</strong>
                </div>
                <div className="metric-row">
                  <span>{t('confidenceInterval')}</span>
                  <strong>
                    {formatValue(item.data?.lowerBound, item.digits ?? 1)} - {formatValue(item.data?.upperBound, item.digits ?? 1)}
                  </strong>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {risk && (
        <div className="card" style={{ marginBottom: 24 }}>
          <div className="card-title" style={{ marginBottom: 16 }}>
            <Shield size={18} className="icon-purple" /> {t('riskAnalysis')}
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'minmax(220px, 1.2fr) minmax(300px, 1fr)', gap: 20 }}>
            <div>
              <div style={{ fontSize: 14, color: 'var(--text-secondary)', marginBottom: 8 }}>{t('stabilityScore')}</div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
                <span
                  style={{
                    fontSize: 42,
                    fontWeight: 800,
                    color: risk.economicStabilityScore > 70 ? 'var(--accent-green)' : 'var(--accent-amber)'
                  }}
                >
                  {formatValue(risk.economicStabilityScore, 1)}
                </span>
                <span className={`risk-badge ${getRiskBadgeClass(risk.marketRiskLevel)}`}>
                  {t(risk.marketRiskLevel)}
                </span>
              </div>
              <p style={{ marginTop: 16, color: 'var(--text-primary)', lineHeight: 1.6, fontSize: 15 }}>
                <strong>{t('Summary')}:</strong> {getRiskSummary(risk)}
              </p>
            </div>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
                gap: 12,
                background: 'var(--bg-surface)',
                padding: 16,
                borderRadius: 12,
                border: '1px solid var(--border)'
              }}
            >
              <div className="mini-stat">
                <div className="mini-stat-label">{t('recessionProb')}</div>
                <div className="mini-stat-value">{formatProbability(risk.recessionProbability, 1)}</div>
              </div>
              <div className="mini-stat">
                <div className="mini-stat-label">{t('Inflation')}</div>
                <div className="mini-stat-value">{formatProbability(risk.inflationSurgeProbability, 1)}</div>
              </div>
              <div className="mini-stat">
                <div className="mini-stat-label">{t('volatilityExposure')}</div>
                <div className="mini-stat-value">{formatProbability(risk.currencyDevaluationProbability, 1)}</div>
              </div>
              <div className="mini-stat">
                <div className="mini-stat-label">{t('instabilityProbability')}</div>
                <div className="mini-stat-value">{formatProbability(risk.instabilityProbability, 1)}</div>
              </div>
            </div>
          </div>
        </div>
      )}

      {scenarioCards.length > 0 && (
        <div className="card" style={{ marginBottom: 24 }}>
          <div className="card-title" style={{ marginBottom: 18 }}>
            <FlaskConical size={18} className="icon-blue" /> {t('scenarioAnalysis')}
          </div>
          <div className="scenario-grid">
            {scenarioCards.map((scenario) => (
              <div key={scenario.name} className="scenario-card">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
                  <div>
                    <h3 style={{ fontSize: 17, fontWeight: 700 }}>{t(scenario.name)}</h3>
                    <p style={{ color: 'var(--text-secondary)', fontSize: 13, marginTop: 6 }}>
                      {t(scenario.description)}
                    </p>
                  </div>
                  <span className={`risk-badge ${getRiskBadgeClass(scenario.risk?.marketRiskLevel)}`}>
                    {t(scenario.risk?.marketRiskLevel)}
                  </span>
                </div>

                <div className="scenario-shocks">
                  <span>{t('GdpGrowth')}: {formatValue(scenario.gdpDeltaPercentagePoints, 1)} pp</span>
                  <span>{t('Inflation')}: {formatValue(scenario.inflationDeltaPercentagePoints, 1)} pp</span>
                  <span>{t('ExchangeRate')}: {formatPercent(scenario.exchangeRateDeltaPercent, 1)}</span>
                  <span>{t('PolicyRate')}: {formatValue(scenario.policyRateDeltaPercentagePoints, 2)} pp</span>
                </div>

                <div className="metric-list" style={{ marginTop: 16 }}>
                  <div className="metric-row">
                    <span>{t('stabilityScore')}</span>
                    <strong>{formatValue(scenario.risk?.economicStabilityScore, 1)}</strong>
                  </div>
                  <div className="metric-row">
                    <span>{t('forecastProbability')}</span>
                    <strong>{formatProbability(scenario.macroForecast?.gdpForecast?.probability, 0)}</strong>
                  </div>
                  <div className="metric-row">
                    <span>{t('instabilityProbability')}</span>
                    <strong>{formatProbability(scenario.risk?.instabilityProbability, 0)}</strong>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {data && (
        <div className="charts-grid">
          <div className="card chart-container">
            <div className="card-title">
              <BarChart2 size={18} className="icon-blue" /> {t('sectorGrowthTitle')}
            </div>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={data.sectorGrowth || []}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                <XAxis dataKey="quarter" stroke="var(--text-secondary)" tickFormatter={(value) => `Q${value}`} />
                <YAxis stroke="var(--text-secondary)" tickFormatter={(value) => `${value}%`} />
                <Tooltip contentStyle={{ backgroundColor: 'var(--bg-card)', borderColor: 'var(--border)' }} />
                <Legend />
                <Bar dataKey="industryGrowth" name="Sanoat" fill="var(--accent-blue)" radius={[4, 4, 0, 0]} />
                <Bar dataKey="agricultureGrowth" name="Qishloq Xo'jaligi" fill="var(--accent-green)" radius={[4, 4, 0, 0]} />
                <Bar dataKey="servicesGrowth" name="Xizmatlar" fill="var(--accent-purple)" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>

          <div className="card chart-container">
            <div className="card-title">
              <TrendingUp size={18} className="icon-amber" /> {t('inflationTrendTitle')}
            </div>
            <ResponsiveContainer width="100%" height={280}>
              <AreaChart data={data.inflation || []}>
                <defs>
                  <linearGradient id="colorInf" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="var(--accent-amber)" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="var(--accent-amber)" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                <XAxis dataKey="period" stroke="var(--text-secondary)" tick={{ fontSize: 12 }} />
                <YAxis stroke="var(--text-secondary)" domain={['dataMin - 1', 'dataMax + 1']} />
                <Tooltip contentStyle={{ backgroundColor: 'var(--bg-card)', borderColor: 'var(--border)' }} />
                <Area
                  type="monotone"
                  dataKey="annualRate"
                  stroke="var(--accent-amber)"
                  fillOpacity={1}
                  fill="url(#colorInf)"
                  strokeWidth={3}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {backtest?.indicators?.length > 0 && (
        <div className="card" style={{ marginTop: 24 }}>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              gap: 16,
              marginBottom: 16,
              flexWrap: 'wrap'
            }}
          >
            <div className="card-title" style={{ marginBottom: 0 }}>
              <Target size={18} className="icon-green" /> {t('backtest')}
            </div>
            <div style={{ color: 'var(--text-secondary)', fontSize: 12 }}>
              {t(backtest.summary)} {backtest.trainRatio ? `(${Math.round(backtest.trainRatio * 100)}/${Math.round((1 - backtest.trainRatio) * 100)})` : ''} {t('comparisonFormat')}
            </div>
          </div>

          <div style={{ overflowX: 'auto' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>{t('predictions')}</th>
                  <th>{t('productionMethod')}</th>
                  <th>{t('mlMethod')}</th>
                  <th>{t('modelAccuracy')}</th>
                  <th>MAE</th>
                  <th>MAPE</th>
                  <th>RMSE</th>
                  <th>{t('baselineMethod')}</th>
                </tr>
              </thead>
              <tbody>
                {backtest.indicators.map((item) => (
                  <tr key={item.indicator}>
                    <td>{t(item.indicator)}</td>
                    <td>{t(item.productionMethod || item.baselineMethod)}</td>
                    <td>{item.mlMethod ? t(item.mlMethod) : '--'}</td>
                    <td>{formatComparison(item.modelAccuracy, item.benchmarkAccuracy, (value) => formatProbability(value, 1))}</td>
                    <td>{formatComparison(item.mae, item.benchmarkMae, (value) => formatValue(value, item.indicator === 'ExchangeRate' ? 0 : 2))}</td>
                    <td>{formatComparison(item.mape, item.benchmarkMape, (value) => formatPercent(value, 2))}</td>
                    <td>{formatComparison(item.rmse, item.benchmarkRmse, (value) => formatValue(value, item.indicator === 'ExchangeRate' ? 0 : 2))}</td>
                    <td>{t(item.baselineMethod)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
