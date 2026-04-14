import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { LanguageProvider, useLanguage } from './context/LanguageContext';
import AppShell from './components/AppShell';
import AuthPage from './pages/AuthPage';
import Dashboard from './pages/Dashboard';
import { TrendingUp, BarChart2, Shield, Activity, PieChart } from 'lucide-react';
import './index.css';

// ── Market Data Center ───────────────────────────────────────────
const MarketDataPage = () => {
    const { t } = useLanguage();
    const [data, setData] = useState(null);
    useEffect(() => {
        import('./api').then(({ dashboardService }) =>
            dashboardService.marketData().then(r => setData(r.data)).catch(() => {})
        );
    }, []);

    if (!data) return <div className="fade-in card" style={{ padding: 48, textAlign: 'center' }}>{t('noData')}</div>;

    return (
        <div className="fade-in">
            <div className="card" style={{ marginBottom: 24, borderLeft: '4px solid var(--accent-blue)' }}>
                <h3 style={{ fontSize: 18, fontWeight: 700 }}>📊 {t('marketData')}</h3>
                <p style={{ fontSize: 13, color: 'var(--text-secondary)' }}>O'zbekiston Respublikasi rasmiy statistik ma'lumotlarining real-vaqt rejimidagi holati.</p>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(450px, 1fr))', gap: 24 }}>
                <div className="card">
                    <div className="card-title" style={{ marginBottom: 16 }}><Activity size={16} /> {t('ExchangeRate')}</div>
                    <div style={{ maxHeight: 400, overflowY: 'auto' }}>
                        <table className="data-table">
                            <thead><tr><th>Sana</th><th>Kurs</th><th>O'zgarish</th></tr></thead>
                            <tbody>
                                {data.exchangeRates?.map((d, i) => (
                                    <tr key={i}>
                                        <td>{d.date}</td>
                                        <td style={{ fontWeight: 600 }}>{d.rate?.toFixed(2)}</td>
                                        <td style={{ color: d.diff >= 0 ? 'var(--accent-red)' : 'var(--accent-green)' }}>{d.diff > 0 ? '+' : ''}{d.diff?.toFixed(2)}</td>
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
                                {data.gdp?.map((d, i) => (
                                    <tr key={i}>
                                        <td>{d.year} - Q{d.quarter}</td>
                                        <td style={{ color: 'var(--accent-green)', fontWeight: 600 }}>+{d.growthRate?.toFixed(1)}%</td>
                                        <td>{d.volumeBillionUzs?.toFixed(1)}</td>
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

// ── AI Predictions ───────────────────────────────────────────────
const PredictionsPage = () => {
    const { t } = useLanguage();
    const [forecast, setForecast] = useState(null);
    useEffect(() => {
        import('./api').then(({ dashboardService }) =>
            dashboardService.predictions().then(r => setForecast(r.data)).catch(() => {})
        );
    }, []);

    if (!forecast) return <div className="fade-in card" style={{ padding: 48, textAlign: 'center' }}>...</div>;

    const items = [
        { label: t('GdpGrowth'), data: forecast.gdpForecast, icon: <PieChart size={20} color="var(--accent-green)"/> },
        { label: t('Inflation'), data: forecast.inflationForecast, icon: <Activity size={20} color="var(--accent-amber)"/> },
        { label: t('ExchangeRate'), data: forecast.exchangeForecast, icon: <TrendingUp size={20} color="var(--accent-blue)"/> },
        { label: t('PolicyRate'), data: forecast.policyRateForecast, icon: <Shield size={20} color="var(--accent-purple)"/> }
    ];

    return (
        <div className="fade-in">
            <div className="card" style={{ marginBottom: 24 }}>
                <h3 style={{ fontSize:18, fontWeight:700 }}>🧠 {t('predictions')}</h3>
                <p style={{ fontSize:13, color:'var(--text-secondary)' }}>AI / ML Regression Models.</p>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: 20 }}>
                {items.map((item, i) => (
                    <div key={i} className="card" style={{ borderTop: `4px solid ${item.icon?.props?.color}` }}>
                        <div style={{ display:'flex', alignItems:'center', gap:10, marginBottom:16 }}>
                            {item.icon}
                            <span style={{ fontWeight:700 }}>{item.label}</span>
                        </div>
                        <div style={{ fontSize: 28, fontWeight: 800, marginBottom: 4 }}>
                            {item.data?.value?.toFixed(2)}{item.label?.includes('Kurs') ? '' : '%'}
                        </div>
                        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
                            <span className={`risk-badge ${item.data?.trend?.includes('HIGH') ? 'HIGH' : 'LOW'}`} style={{ fontSize:10 }}>{t(item.data?.trend)}</span>
                            <span style={{ fontSize:10, color:'var(--text-secondary)' }}>Model: {t(item.data?.method)}</span>
                        </div>
                        <p style={{ marginTop:12, fontSize:12, color:'var(--text-secondary)', lineHeight:1.4 }}>{t(item.data?.description)}</p>
                    </div>
                ))}
            </div>
        </div>
    );
};

// ── Risk Analysis ────────────────────────────────────────────────
const RiskPage = () => {
    const { t } = useLanguage();
    const [risk, setRisk] = useState(null);
    useEffect(() => {
        import('./api').then(({ dashboardService }) =>
            dashboardService.risk().then(r => setRisk(r.data)).catch(() => {})
        );
    }, []);

    const getRiskSummary = (risk) => {
        if (!risk) return "";
        let template = t(risk.summary);
        return template
          .replace("{score}", risk.economicStabilityScore?.toFixed(1))
          .replace("{inf}", (risk.inflationSurgeProbability * 100).toFixed(0))
          .replace("{fx}", (risk.currencyDevaluationProbability * 100).toFixed(0))
          .replace("{rec}", (risk.recessionProbability * 100).toFixed(0));
    };

    if (!risk) return <div className="fade-in card" style={{ textAlign: 'center', padding: 48 }}>{t('noData')}</div>;

    return (
        <div className="fade-in">
            <div className="kpi-grid">
                <div className="kpi-card">
                    <div className="kpi-label">{t('stabilityScore')}</div>
                    <div className="kpi-val" style={{ color: 'var(--accent-green)' }}>{risk.economicStabilityScore?.toFixed(1)}%</div>
                </div>
                <div className="kpi-card">
                    <div className="kpi-label">{t('marketRisk')}</div>
                    <div className="kpi-val"><span className={`risk-badge ${risk.marketRiskLevel}`}>{t(risk.marketRiskLevel)}</span></div>
                </div>
                <div className="kpi-card">
                    <div className="kpi-label">{t('recessionProb')}</div>
                    <div className="kpi-val">{(risk.recessionProbability * 100).toFixed(1)}%</div>
                </div>
            </div>

            <div className="card" style={{ marginTop: 24 }}>
                <div className="card-title">{t('Recommendation')}</div>
                <p style={{ fontSize: 16, lineHeight: 1.6, marginTop: 12 }}>{getRiskSummary(risk)}</p>
            </div>
        </div>
    );
};

const SettingsPage = () => (
    <div className="fade-in card" style={{ maxWidth: 600 }}>
        <h3>⚙️ Settings</h3>
        <p style={{ color: 'var(--text-secondary)', marginTop: 8 }}>Multilingual AI Analysis Core v2.0</p>
    </div>
);

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

    const toggleTheme = () => setTheme(t => t === 'dark' ? 'light' : 'dark');
    const { user } = useAuth();

    return (
        <Routes>
            <Route path="/" element={user ? <Navigate to="/dashboard"/> : <AuthPage/>} />
            <Route path="/dashboard" element={<Protected><AppShell theme={theme} toggleTheme={toggleTheme}><Dashboard/></AppShell></Protected>} />
            <Route path="/market-data" element={<Protected><AppShell theme={theme} toggleTheme={toggleTheme}><MarketDataPage/></AppShell></Protected>} />
            <Route path="/predictions" element={<Protected><AppShell theme={theme} toggleTheme={toggleTheme}><PredictionsPage/></AppShell></Protected>} />
            <Route path="/risk" element={<Protected><AppShell theme={theme} toggleTheme={toggleTheme}><RiskPage/></AppShell></Protected>} />
            <Route path="/settings" element={<Protected><AppShell theme={theme} toggleTheme={toggleTheme}><SettingsPage/></AppShell></Protected>} />
            <Route path="*" element={<Navigate to="/" replace/>} />
        </Routes>
    );
}

export default function App() {
    return (
        <BrowserRouter>
            <LanguageProvider>
                <AuthProvider>
                    <AppRoutes/>
                </AuthProvider>
            </LanguageProvider>
        </BrowserRouter>
    );
}
