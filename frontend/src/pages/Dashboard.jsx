import React, { useState, useEffect } from 'react';
import { AreaChart, Area, BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, Legend } from 'recharts';
import { Activity, Shield, TrendingUp, RefreshCw, BarChart2 } from 'lucide-react';
import { dashboardService } from '../api';
import { useLanguage } from '../context/LanguageContext';

export default function Dashboard() {
  const { t } = useLanguage();
  const [data, setData] = useState(null);
  const [forecast, setForecast] = useState(null);
  const [risk, setRisk] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchDashboard();
  }, []);

  const fetchDashboard = async () => {
    try {
      setLoading(true);
      const [mData, fData, rData] = await Promise.all([
        dashboardService.marketData(),
        dashboardService.predictions(),
        dashboardService.risk()
      ]);
      setData(mData.data);
      setForecast(fData.data);
      setRisk(rData.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const simulateData = async () => {
    try {
      setLoading(true);
      await dashboardService.simulate();
      await fetchDashboard();
    } catch (e) {
      console.error(e);
      setLoading(false);
    }
  };

  const getRiskSummary = (risk) => {
    if (!risk) return "";
    let template = t(risk.summary);
    return template
      .replace("{score}", risk.economicStabilityScore?.toFixed(1))
      .replace("{inf}", (risk.inflationSurgeProbability * 100).toFixed(0))
      .replace("{fx}", (risk.currencyDevaluationProbability * 100).toFixed(0))
      .replace("{rec}", (risk.recessionProbability * 100).toFixed(0));
  };

  if (!data && !loading) return (
    <div className="fade-in card" style={{ textAlign: 'center', padding: '60px 20px' }}>
      <p style={{ color: 'var(--text-secondary)' }}>{t('noData')}</p>
      <button className="btn btn-primary" onClick={simulateData} style={{ marginTop: 20 }}>
        {t('simulateData')}
      </button>
    </div>
  );

  return (
    <div className="fade-in">
      {/* ── HEADER ── */}
      <div className="card" style={{ marginBottom: 24, borderLeft: '4px solid var(--accent-blue)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 style={{ fontSize: 22, fontWeight: 700, marginBottom: 8, display: 'flex', alignItems: 'center', gap: 10 }}>
            <Activity className="icon-blue" />
            Makroiqtisodiy Barqarorlik Tizimi
          </h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: 14 }}>
            O'zbekiston Respublikasi YaIM, Sanoat, Inflyatsiya va Valyuta Tahlili.
          </p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <button className="btn btn-primary" onClick={simulateData} disabled={loading}>
            <RefreshCw size={16} className={loading ? 'spin' : ''} /> {t('refresh')}
          </button>
        </div>
      </div>

      {/* ── MACRO FORECAST KPIs ── */}
      {forecast && (
        <div className="kpi-grid" style={{ marginBottom: 24 }}>
          <div className="kpi-card" style={{ borderTop: '4px solid var(--accent-green)' }}>
            <div className="kpi-label">{t('GdpGrowth')}</div>
            <div className="kpi-val" style={{ color: 'var(--accent-green)' }}>
              {forecast.gdpForecast?.value.toFixed(1)}%
            </div>
            <small style={{ color: 'var(--text-secondary)' }}>Trend: {t(forecast.gdpForecast?.trend)}</small>
          </div>
          
          <div className="kpi-card" style={{ borderTop: '4px solid var(--accent-amber)' }}>
            <div className="kpi-label">{t('Inflation')}</div>
            <div className="kpi-val" style={{ color: 'var(--accent-amber)' }}>
              {forecast.inflationForecast?.value.toFixed(1)}%
            </div>
            <small style={{ color: 'var(--text-secondary)' }}>Trend: {t(forecast.inflationForecast?.trend)}</small>
          </div>

          <div className="kpi-card" style={{ borderTop: '4px solid var(--accent-purple)' }}>
            <div className="kpi-label">{t('PolicyRate')}</div>
            <div className="kpi-val">
              {forecast.policyRateForecast?.value.toFixed(2)}%
            </div>
            <small style={{ color: 'var(--text-secondary)' }}>Trend: {t(forecast.policyRateForecast?.trend)}</small>
          </div>

          <div className="kpi-card" style={{ borderTop: '4px solid var(--accent-blue)' }}>
            <div className="kpi-label">{t('ExchangeRate')}</div>
            <div className="kpi-val" style={{ fontSize: 24 }}>
              {forecast.exchangeForecast?.value.toFixed(0)} <span style={{ fontSize:14 }}>UZS</span>
            </div>
            <small style={{ color: 'var(--text-secondary)' }}>Trend: {t(forecast.exchangeForecast?.trend)}</small>
          </div>
        </div>
      )}

      {/* ── SECURITY & RISK SUMMARY ── */}
      {risk && (
        <div className="card" style={{ marginBottom: 24 }}>
          <div className="card-title" style={{ marginBottom: 16 }}>
            <Shield size={18} className="icon-purple" /> {t('riskAnalysis')}
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '20px', alignItems: 'center' }}>
            <div style={{ flex: '1 1 300px' }}>
              <div style={{ fontSize: 14, color: 'var(--text-secondary)', marginBottom: 8 }}>{t('stabilityScore')}</div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                <span style={{ fontSize: 42, fontWeight: 800, color: risk.economicStabilityScore > 70 ? 'var(--accent-green)' : 'var(--accent-amber)' }}>
                  {risk.economicStabilityScore?.toFixed(1)}
                </span>
                <span className={`risk-badge ${risk.marketRiskLevel}`}>
                  {t(risk.marketRiskLevel)}
                </span>
              </div>
            </div>
            <div style={{ flex: '2 1 400px', display: 'flex', gap: '20px', justifyContent: 'space-between', background: 'var(--bg-surface)', padding: 16, borderRadius: 12 }}>
               <div>
                 <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{t('recessionProb')}</div>
                 <div style={{ fontSize: 18, fontWeight: 600 }}>{(risk.recessionProbability * 100).toFixed(1)}%</div>
               </div>
               <div>
                 <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{t('Inflation')}</div>
                 <div style={{ fontSize: 18, fontWeight: 600, color: 'var(--accent-amber)' }}>{(risk.inflationSurgeProbability * 100).toFixed(1)}%</div>
               </div>
               <div>
                 <div style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{t('volatilityExposure')}</div>
                 <div style={{ fontSize: 18, fontWeight: 600, color: 'var(--accent-red)' }}>{(risk.currencyDevaluationProbability * 100).toFixed(1)}%</div>
               </div>
            </div>
          </div>
          <p style={{ marginTop: 16, color: 'var(--text-primary)', lineHeight: 1.6, fontSize: 15 }}>
            <strong>{t('Summary')}:</strong> {getRiskSummary(risk)}
          </p>
        </div>
      )}

      {/* ── CHARTS: SECTOR GROWTH & MACRO TRENDS ── */}
      {data && (
        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) minmax(0, 1fr)', gap: 24, marginBottom: 24 }}>
          
          <div className="card chart-container">
            <div className="card-title">
              <BarChart2 size={18} className="icon-blue"/> Tarmoqlar O'sishi (Q/Q)
            </div>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={data.sectorGrowth || []}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                <XAxis dataKey="quarter" stroke="var(--text-secondary)" tickFormatter={(v) => `Q${v}`} />
                <YAxis stroke="var(--text-secondary)" tickFormatter={(v) => `${v}%`} />
                <Tooltip contentStyle={{ backgroundColor: 'var(--bg-card)', borderColor: 'var(--border)' }} />
                <Legend />
                <Bar dataKey="industryGrowth" name="Sanoat" fill="var(--accent-blue)" radius={[4,4,0,0]} />
                <Bar dataKey="agricultureGrowth" name="Qishloq Xo'jaligi" fill="var(--accent-green)" radius={[4,4,0,0]} />
                <Bar dataKey="servicesGrowth" name="Xizmatlar" fill="var(--accent-purple)" radius={[4,4,0,0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>

          <div className="card chart-container">
            <div className="card-title">
              <TrendingUp size={18} className="icon-amber"/> Inflyatsiya (YtY) %
            </div>
            <ResponsiveContainer width="100%" height={280}>
              <AreaChart data={data.inflation || []}>
                <defs>
                  <linearGradient id="colorInf" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="var(--accent-amber)" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="var(--accent-amber)" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                <XAxis dataKey="period" stroke="var(--text-secondary)" tick={{fontSize:12}} />
                <YAxis stroke="var(--text-secondary)" domain={['dataMin - 1', 'dataMax + 1']} />
                <Tooltip contentStyle={{ backgroundColor: 'var(--bg-card)', borderColor: 'var(--border)' }} />
                <Area type="monotone" dataKey="annualRate" stroke="var(--accent-amber)" fillOpacity={1} fill="url(#colorInf)" strokeWidth={3} />
              </AreaChart>
            </ResponsiveContainer>
          </div>

        </div>
      )}
    </div>
  );
}
