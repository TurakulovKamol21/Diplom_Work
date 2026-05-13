import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  TrendingUp, LayoutDashboard, Shield, BarChart2,
  LogOut, Sun, Moon, Database, Settings2
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';

export default function AppShell({ children, theme, toggleTheme }) {
  const { user, logout } = useAuth();
  const { lang, setLang, t } = useLanguage();
  const navigate = useNavigate();
  const location = useLocation();

  const navItems = [
    { label: t('dashboard'),    icon: LayoutDashboard, path: '/dashboard' },
    { label: t('marketData'),   icon: Database,        path: '/market-data' },
    { label: t('predictions'),  icon: BarChart2,        path: '/predictions' },
    { label: t('riskAnalysis'),icon: Shield,           path: '/risk' },
    { label: t('settings'),     icon: Settings2,        path: '/settings' },
  ];

  const handleLogout = () => { logout(); navigate('/'); };

  return (
    <div className="app-shell">
      {/* ── Sidebar ── */}
      <aside className="sidebar">
        <div className="sidebar-logo">
          <div className="logo-icon"><TrendingUp size={18}/></div>
          <span>{t('macroBrand')}<small>{t('macroBrandSubtitle')}</small></span>
        </div>

        {navItems.map(item => (
          <div
            key={item.path}
            id={`nav-${item.label.replace(/\s+/g,'-').toLowerCase()}`}
            className={`nav-item ${location.pathname === item.path ? 'active' : ''}`}
            onClick={() => navigate(item.path)}
          >
            <item.icon size={17}/>
            {item.label}
          </div>
        ))}

        <div className="sidebar-footer">
          <div style={{ marginBottom:10, fontSize:13, fontWeight:600, color:'var(--text-primary)' }}>
            {user?.username}
          </div>
          <div style={{ display:'flex', gap:8 }}>
            <button className="btn btn-ghost" style={{ padding:'6px 10px', fontSize:12 }} onClick={handleLogout}>
              <LogOut size={13}/> {t('logout')}
            </button>
          </div>
        </div>
      </aside>

      {/* ── Main ── */}
      <div className="main-content">
        {/* Topbar */}
        <header className="topbar">
          <h1>{t('macroPlatformTitle')}</h1>
          <div className="topbar-right">
            <div className="lang-selector" style={{ display:'flex', gap:4, marginRight:10 }}>
              {['en','ru','uz'].map(l => (
                <button
                  key={l}
                  className={`btn-lang ${lang === l ? 'active' : ''}`}
                  onClick={() => setLang(l)}
                  style={{
                    background: lang === l ? 'var(--accent-glow)' : 'transparent',
                    border: '1px solid var(--border)',
                    padding: '2px 6px',
                    borderRadius: 4,
                    fontSize: 10,
                    cursor: 'pointer',
                    color: 'var(--text-primary)',
                    textTransform: 'uppercase'
                  }}
                >
                  {l}
                </button>
              ))}
            </div>
            <span style={{ fontSize:12, color:'var(--text-secondary)' }}>
              {t('signedInAs')} <strong style={{ color:'var(--text-accent)' }}>{user?.username}</strong>
            </span>
            <button
              id="btn-theme-toggle"
              className="theme-toggle"
              onClick={toggleTheme}
              title="Toggle theme"
            >
              {theme === 'dark' ? <Sun size={16}/> : <Moon size={16}/>}
            </button>
          </div>
        </header>

        {/* Page body */}
        <main className="page-body">
          {children}
        </main>
      </div>
    </div>
  );
}
