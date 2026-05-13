import React, { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';
import { AuthProvider, useAuth } from './context/AuthContext';
import { LanguageProvider, useLanguage } from './context/LanguageContext';
import AppShell from './components/AppShell';
import AuthPage from './pages/AuthPage';
import Dashboard from './pages/Dashboard';
import {
  TrendingUp,
  BarChart2,
  Shield,
  Activity,
  PieChart,
  Target,
  FlaskConical,
  AlertTriangle,
  X
} from 'lucide-react';
import './index.css';

const isFiniteNumber = (value) => typeof value === 'number' && Number.isFinite(value);

const formatValue = (value, digits = 1) => (
  isFiniteNumber(value) ? value.toFixed(digits) : '--'
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

const getPredictionHistory = (marketData, key) => {
  if (!marketData) {
    return [];
  }

  if (key === 'gdp') {
    return (marketData.gdp ?? [])
      .slice(-6)
      .map((item) => ({
        label: `${item.year} - Q${item.quarter}`,
        value: item.growthRate
      }))
      .filter((item) => isFiniteNumber(item.value));
  }

  if (key === 'inflation') {
    return (marketData.inflation ?? [])
      .filter((item) => {
        const category = `${item.category ?? ''}`.toLowerCase();
        return !category || category.includes('official') || category.includes('general');
      })
      .slice(-6)
      .map((item) => ({
        label: item.period,
        value: item.annualRate
      }))
      .filter((item) => isFiniteNumber(item.value));
  }

  if (key === 'exchange') {
    return (marketData.exchangeRates ?? [])
      .slice(-7)
      .map((item) => ({
        label: item.date,
        value: item.rate
      }))
      .filter((item) => isFiniteNumber(item.value));
  }

  if (key === 'policy') {
    return (marketData.policyRates ?? [])
      .slice(-7)
      .map((item) => ({
        label: item.date,
        value: item.rate
      }))
      .filter((item) => isFiniteNumber(item.value));
  }

  return [];
};

const MarketDataPage = () => {
  const { t } = useLanguage();
  const [data, setData] = useState(null);

  useEffect(() => {
    import('./api').then(({ dashboardService }) =>
      dashboardService.marketData().then((response) => setData(response.data)).catch(() => {})
    );
  }, []);

  if (!data) {
    return <div className="fade-in card" style={{ padding: 48, textAlign: 'center' }}>{t('noData')}</div>;
  }

  return (
    <div className="fade-in">
      <div className="card" style={{ marginBottom: 24, borderLeft: '4px solid var(--accent-blue)' }}>
        <h3 style={{ fontSize: 18, fontWeight: 700 }}>{t('marketData')}</h3>
        <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 8 }}>{t('marketDataSubtitle')}</p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(450px, 1fr))', gap: 24 }}>
        <div className="card">
          <div className="card-title" style={{ marginBottom: 16 }}><Activity size={16} /> {t('ExchangeRate')}</div>
          <div style={{ maxHeight: 400, overflowY: 'auto' }}>
            <table className="data-table">
              <thead><tr><th>Sana</th><th>Kurs</th><th>O'zgarish</th></tr></thead>
              <tbody>
                {data.exchangeRates?.map((item, index) => (
                  <tr key={index}>
                    <td>{item.date}</td>
                    <td style={{ fontWeight: 600 }}>{formatValue(item.rate, 2)}</td>
                    <td style={{ color: item.diff >= 0 ? 'var(--accent-red)' : 'var(--accent-green)' }}>
                      {item.diff > 0 ? '+' : ''}{formatValue(item.diff, 2)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="card">
          <div className="card-title" style={{ marginBottom: 16 }}><BarChart2 size={16} /> {t('GdpGrowth')}</div>
          <div style={{ maxHeight: 400, overflowY: 'auto' }}>
            <table className="data-table">
              <thead><tr><th>Yil / Chorak</th><th>O'sish (%)</th><th>Hajm (mlrd. UZS)</th></tr></thead>
              <tbody>
                {data.gdp?.map((item, index) => (
                  <tr key={index}>
                    <td>{item.year} - Q{item.quarter}</td>
                    <td style={{ color: 'var(--accent-green)', fontWeight: 600 }}>+{formatValue(item.growthRate, 1)}%</td>
                    <td>{formatValue(item.volumeBillionUzs, 1)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};

const PredictionsPage = () => {
  const { t } = useLanguage();
  const [forecast, setForecast] = useState(null);
  const [backtest, setBacktest] = useState(null);
  const [marketData, setMarketData] = useState(null);
  const [selectedCardKey, setSelectedCardKey] = useState(null);

  useEffect(() => {
    import('./api').then(({ dashboardService }) => {
      Promise.allSettled([
        dashboardService.predictions(),
        dashboardService.backtest(),
        dashboardService.marketData()
      ]).then(([forecastResult, backtestResult, marketDataResult]) => {
        if (forecastResult.status === 'fulfilled') setForecast(forecastResult.value.data);
        if (backtestResult.status === 'fulfilled') setBacktest(backtestResult.value.data);
        if (marketDataResult.status === 'fulfilled') setMarketData(marketDataResult.value.data);
      });
    });
  }, []);

  useEffect(() => {
    if (!selectedCardKey) {
      return undefined;
    }

    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        setSelectedCardKey(null);
      }
    };

    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [selectedCardKey]);

  if (!forecast) {
    return <div className="fade-in card" style={{ padding: 48, textAlign: 'center' }}>...</div>;
  }

  const cards = [
    {
      key: 'gdp',
      label: t('GdpGrowth'),
      data: forecast.gdpForecast,
      icon: <PieChart size={20} color="var(--accent-green)" />,
      suffix: '%',
      historyLabel: t('recentQuarters'),
      history: getPredictionHistory(marketData, 'gdp')
    },
    {
      key: 'inflation',
      label: t('Inflation'),
      data: forecast.inflationForecast,
      icon: <Activity size={20} color="var(--accent-amber)" />,
      suffix: '%',
      historyLabel: t('recentPeriods'),
      history: getPredictionHistory(marketData, 'inflation')
    },
    {
      key: 'exchange',
      label: t('ExchangeRate'),
      data: forecast.exchangeForecast,
      icon: <TrendingUp size={20} color="var(--accent-blue)" />,
      suffix: ' UZS',
      digits: 0,
      historyLabel: t('recentPeriods'),
      history: getPredictionHistory(marketData, 'exchange')
    },
    {
      key: 'policy',
      label: t('PolicyRate'),
      data: forecast.policyRateForecast,
      icon: <Shield size={20} color="var(--accent-purple)" />,
      suffix: '%',
      historyLabel: t('recentPeriods'),
      history: getPredictionHistory(marketData, 'policy')
    }
  ];
  const selectedCard = cards.find((item) => item.key === selectedCardKey);

  return (
    <div className="fade-in">
      <div className="card" style={{ marginBottom: 24 }}>
        <h3 style={{ fontSize: 18, fontWeight: 700 }}>AI / ML {t('predictions')}</h3>
        <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 8 }}>{t('predictionSubtitle')}</p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 20 }}>
        {cards.map((item, index) => (
          <button
            key={index}
            type="button"
            className="card prediction-card-button"
            style={{ borderTop: `4px solid ${item.icon.props.color}` }}
            onClick={() => setSelectedCardKey(item.key)}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
              {item.icon}
              <span style={{ fontWeight: 700 }}>{item.label}</span>
            </div>
            <div style={{ fontSize: 28, fontWeight: 800, marginBottom: 6 }}>
              {formatValue(item.data?.value, item.digits ?? 2)}{item.suffix}
            </div>
            <div className="metric-list">
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
                  {formatValue(item.data?.lowerBound, item.digits ?? 2)} - {formatValue(item.data?.upperBound, item.digits ?? 2)}
                </strong>
              </div>
              <div className="metric-row">
                <span>{t('modelAccuracy')}</span>
                <strong>{formatProbability(item.data?.modelAccuracy, 1)}</strong>
              </div>
            </div>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 14 }}>
              <span className="method-chip">{t(item.data?.method)}</span>
              {item.data?.baselineMethod && <span className="method-chip secondary">{t(item.data?.baselineMethod)}</span>}
              {item.data?.mlMethod && <span className="method-chip secondary">{t(item.data?.mlMethod)}</span>}
            </div>
            <p style={{ marginTop: 12, fontSize: 12, color: 'var(--text-secondary)', lineHeight: 1.5 }}>
              {t(item.data?.description)}
            </p>
            <div className="prediction-card-hint">
              <span>{t('clickToInspect')}</span>
              <strong>{item.historyLabel}</strong>
            </div>
          </button>
        ))}
      </div>

      {selectedCard && (
        <div
          className="modal-overlay"
          role="presentation"
          onClick={() => setSelectedCardKey(null)}
        >
          <div
            className="modal-card prediction-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby={`prediction-detail-${selectedCard.key}`}
            onClick={(event) => event.stopPropagation()}
          >
            <div className="modal-header">
              <div>
                <div className="card-title">{t('forecastDetails')}</div>
                <h3 id={`prediction-detail-${selectedCard.key}`} style={{ fontSize: 22, fontWeight: 800 }}>
                  {selectedCard.label}
                </h3>
                <p style={{ marginTop: 8, color: 'var(--text-secondary)', fontSize: 14 }}>
                  {t('historicalSeries')}: {selectedCard.historyLabel}
                </p>
              </div>
              <button
                type="button"
                className="modal-close"
                onClick={() => setSelectedCardKey(null)}
                aria-label={t('close')}
              >
                <X size={18} />
              </button>
            </div>

            <div className="prediction-detail-grid">
              <div className="mini-stat">
                <div className="mini-stat-label">{t('forecastValue')}</div>
                <div className="mini-stat-value">
                  {formatValue(selectedCard.data?.value, selectedCard.digits ?? 2)}
                  {selectedCard.suffix}
                </div>
              </div>
              <div className="mini-stat">
                <div className="mini-stat-label">{t('previousActual')}</div>
                <div className="mini-stat-value">
                  {formatValue(selectedCard.data?.previousValue, selectedCard.digits ?? 2)}
                  {selectedCard.suffix}
                </div>
              </div>
              <div className="mini-stat">
                <div className="mini-stat-label">{t('forecastHorizon')}</div>
                <div className="mini-stat-value">{selectedCard.data?.forecastHorizon ?? '--'}</div>
              </div>
              <div className="mini-stat">
                <div className="mini-stat-label">{t('modelAccuracy')}</div>
                <div className="mini-stat-value">{formatProbability(selectedCard.data?.modelAccuracy, 1)}</div>
              </div>
            </div>

            {selectedCard.history.length > 0 ? (
              <>
                <div className="prediction-chart-card">
                  <div className="card-title" style={{ marginBottom: 12 }}>{t('historicalSeries')}</div>
                  <div style={{ height: 280 }}>
                    <ResponsiveContainer width="100%" height="100%">
                      <LineChart data={selectedCard.history}>
                        <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" />
                        <XAxis
                          dataKey="label"
                          stroke="var(--text-secondary)"
                          tick={{ fontSize: 11 }}
                          minTickGap={12}
                        />
                        <YAxis
                          stroke="var(--text-secondary)"
                          tick={{ fontSize: 11 }}
                          width={56}
                          tickFormatter={(value) => formatValue(value, selectedCard.digits ?? 1)}
                        />
                        <Tooltip
                          formatter={(value) => `${formatValue(value, selectedCard.digits ?? 2)}${selectedCard.suffix}`}
                          contentStyle={{
                            background: 'var(--bg-surface)',
                            border: '1px solid var(--border)',
                            borderRadius: 12,
                            color: 'var(--text-primary)'
                          }}
                        />
                        <Line
                          type="monotone"
                          dataKey="value"
                          stroke={selectedCard.icon.props.color}
                          strokeWidth={3}
                          dot={{ r: 4 }}
                          activeDot={{ r: 6 }}
                        />
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                </div>

                <div style={{ overflowX: 'auto' }}>
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>{t('period')}</th>
                        <th>{t('value')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedCard.history.slice().reverse().map((row) => (
                        <tr key={`${selectedCard.key}-${row.label}`}>
                          <td>{row.label}</td>
                          <td style={{ fontWeight: 700 }}>
                            {formatValue(row.value, selectedCard.digits ?? 2)}
                            {selectedCard.suffix}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </>
            ) : (
              <div className="card" style={{ marginTop: 12, textAlign: 'center', color: 'var(--text-secondary)' }}>
                {t('historyUnavailable')}
              </div>
            )}
          </div>
        </div>
      )}

      {backtest?.indicators?.length > 0 && (
        <div className="card" style={{ marginTop: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center', flexWrap: 'wrap', marginBottom: 14 }}>
            <div className="card-title" style={{ marginBottom: 0 }}>
              <Target size={18} className="icon-green" /> {t('backtest')}
            </div>
            <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
              {t('comparisonFormat')}
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
};

const RiskPage = () => {
  const { t } = useLanguage();
  const [risk, setRisk] = useState(null);
  const [scenarios, setScenarios] = useState(null);

  useEffect(() => {
    import('./api').then(({ dashboardService }) => {
      Promise.allSettled([dashboardService.risk(), dashboardService.scenarioAnalysis()]).then(([riskResult, scenarioResult]) => {
        if (riskResult.status === 'fulfilled') setRisk(riskResult.value.data);
        if (scenarioResult.status === 'fulfilled') setScenarios(scenarioResult.value.data);
      });
    });
  }, []);

  const getRiskSummary = (riskPayload) => {
    if (!riskPayload) return '';
    return t(riskPayload.summary)
      .replace('{score}', formatValue(riskPayload.economicStabilityScore, 1))
      .replace('{inf}', formatProbability(riskPayload.inflationSurgeProbability, 0).replace('%', ''))
      .replace('{fx}', formatProbability(riskPayload.currencyDevaluationProbability, 0).replace('%', ''))
      .replace('{rec}', formatProbability(riskPayload.recessionProbability, 0).replace('%', ''));
  };

  if (!risk) {
    return <div className="fade-in card" style={{ textAlign: 'center', padding: 48 }}>{t('noData')}</div>;
  }

  const scenarioCards = scenarios
    ? [scenarios.bestCase, scenarios.baselineCase, scenarios.worstCase].filter(Boolean)
    : [];

  return (
    <div className="fade-in">
      <div className="kpi-grid">
        <div className="kpi-card">
          <div className="kpi-label">{t('stabilityScore')}</div>
          <div className="kpi-val" style={{ color: 'var(--accent-green)' }}>{formatValue(risk.economicStabilityScore, 1)}</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">{t('marketRisk')}</div>
          <div className="kpi-val"><span className={`risk-badge ${getRiskBadgeClass(risk.marketRiskLevel)}`}>{t(risk.marketRiskLevel)}</span></div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">{t('recessionProb')}</div>
          <div className="kpi-val">{formatProbability(risk.recessionProbability, 1)}</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">{t('instabilityProbability')}</div>
          <div className="kpi-val">{formatProbability(risk.instabilityProbability, 1)}</div>
        </div>
      </div>

      <div className="card" style={{ marginTop: 24 }}>
        <div className="card-title">{t('Recommendation')}</div>
        <p style={{ fontSize: 16, lineHeight: 1.6, marginTop: 12 }}>{getRiskSummary(risk)}</p>
      </div>

      {scenarioCards.length > 0 && (
        <div className="card" style={{ marginTop: 24 }}>
          <div className="card-title" style={{ marginBottom: 16 }}>
            <FlaskConical size={18} className="icon-blue" /> {t('scenarioAnalysis')}
          </div>
          <div className="scenario-grid">
            {scenarioCards.map((scenario) => (
              <div key={scenario.name} className="scenario-card">
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
                  <div>
                    <h3 style={{ fontSize: 16, fontWeight: 700 }}>{t(scenario.name)}</h3>
                    <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 6 }}>{t(scenario.description)}</p>
                  </div>
                  <AlertTriangle size={18} color="var(--accent-amber)" />
                </div>
                <div className="metric-list" style={{ marginTop: 16 }}>
                  <div className="metric-row">
                    <span>{t('stabilityScore')}</span>
                    <strong>{formatValue(scenario.risk?.economicStabilityScore, 1)}</strong>
                  </div>
                  <div className="metric-row">
                    <span>{t('volatilityExposure')}</span>
                    <strong>{formatProbability(scenario.risk?.currencyDevaluationProbability, 0)}</strong>
                  </div>
                  <div className="metric-row">
                    <span>{t('recessionProb')}</span>
                    <strong>{formatProbability(scenario.risk?.recessionProbability, 0)}</strong>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

const SettingsPage = () => {
  const { t } = useLanguage();
  return (
    <div className="fade-in card" style={{ maxWidth: 600 }}>
      <h3>{t('settings')}</h3>
      <p style={{ color: 'var(--text-secondary)', marginTop: 8 }}>{t('settingsSubtitle')}</p>
    </div>
  );
};

function Protected({ children }) {
  const { user, loading } = useAuth();
  if (loading) return null;
  return user ? children : <Navigate to="/" replace />;
}

function AppRoutes() {
  const [theme, setTheme] = useState(() => localStorage.getItem('theme') || 'dark');

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = () => setTheme((currentTheme) => currentTheme === 'dark' ? 'light' : 'dark');
  const { user } = useAuth();

  return (
    <Routes>
      <Route path="/" element={user ? <Navigate to="/dashboard" /> : <AuthPage />} />
      <Route path="/dashboard" element={<Protected><AppShell theme={theme} toggleTheme={toggleTheme}><Dashboard /></AppShell></Protected>} />
      <Route path="/market-data" element={<Protected><AppShell theme={theme} toggleTheme={toggleTheme}><MarketDataPage /></AppShell></Protected>} />
      <Route path="/predictions" element={<Protected><AppShell theme={theme} toggleTheme={toggleTheme}><PredictionsPage /></AppShell></Protected>} />
      <Route path="/risk" element={<Protected><AppShell theme={theme} toggleTheme={toggleTheme}><RiskPage /></AppShell></Protected>} />
      <Route path="/settings" element={<Protected><AppShell theme={theme} toggleTheme={toggleTheme}><SettingsPage /></AppShell></Protected>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <LanguageProvider>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </LanguageProvider>
    </BrowserRouter>
  );
}
