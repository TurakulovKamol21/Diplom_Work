import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TrendingUp, Lock, User, Eye, EyeOff, AlertCircle } from 'lucide-react';
import { GoogleLogin } from '@react-oauth/google';
import { authService } from '../api';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';

export default function AuthPage() {
  const [mode, setMode]         = useState('login'); // 'login' | 'register'
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPw, setShowPw]     = useState(false);
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState('');
  const [success, setSuccess]   = useState('');
  const { login }               = useAuth();
  const { lang, setLang, t }     = useLanguage();
  const navigate                = useNavigate();

  const handle = async (e) => {
    e.preventDefault();
    setError(''); setSuccess('');
    setLoading(true);
    try {
      if (mode === 'register') {
        await authService.register({ username, password });
        setSuccess('Account created! Please log in.');
        setMode('login');
        setUsername('');
        setPassword('');
      } else {
        const res = await authService.login({ username, password });
        login(res.data.token, username);
        navigate('/dashboard');
      }
    } catch (err) {
      setError(err.response?.data || 'Something went wrong. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSuccess = async (credentialResponse) => {
    setError('');
    setLoading(true);
    try {
      const res = await authService.google(credentialResponse.credential);
      // Assuming Google returns the internal JWT. Decode username or fetch profile later.
      // For now, we will decode the username from the internal JWT if possible, but the backend sends
      // just the token.
      login(res.data.token, 'GoogleUser'); // Simplified for UI
      navigate('/dashboard');
    } catch (err) {
      setError('Google authentication failed. ' + (err.response?.data || err.message));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        {/* Logo */}
        <div style={{ display:'flex', alignItems:'center', gap:12, marginBottom:28 }}>
          <div style={{
            width:48, height:48,
            background:'linear-gradient(135deg,#3b82f6,#06b6d4)',
            borderRadius:14, display:'flex', alignItems:'center', justifyContent:'center',
            boxShadow:'0 0 24px rgba(59,130,246,0.4)'
          }}>
            <TrendingUp size={26} color="#fff" />
          </div>
          <div>
            <div style={{ fontWeight:800, fontSize:16, lineHeight:1.2 }}>AI Finance</div>
            <div style={{ fontSize:11, color:'var(--text-secondary)' }}>{t('predictions')}</div>
          </div>
          
          <div className="lang-selector" style={{ display:'flex', gap:6, marginLeft:'auto' }}>
            {['en','ru','uz'].map(l => (
              <button
                key={l}
                className={`btn-lang ${lang === l ? 'active' : ''}`}
                onClick={() => setLang(l)}
                style={{
                  background: lang === l ? 'var(--accent-glow)' : 'transparent',
                  border: '1px solid var(--border)',
                  padding: '2px 8px',
                  borderRadius: 6,
                  fontSize: 11,
                  cursor: 'pointer',
                  color: 'var(--text-primary)',
                  textTransform: 'uppercase'
                }}
              >
                {l}
              </button>
            ))}
          </div>
        </div>

        <h2>{mode === 'login' ? t('welcomeBack') : t('createAccount')}</h2>
        <p>{mode === 'login' ? t('signInToAccess') : t('registerToStart')}</p>

        {error   && <div className="alert alert-error"><AlertCircle size={14} style={{marginRight:6,verticalAlign:'middle'}}/>{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}

        <div style={{ marginBottom: 20 }}>
          <GoogleLogin
            onSuccess={handleGoogleSuccess}
            onError={() => setError('Google Login Failed')}
            useOneTap
            width="100%"
            shape="rectangular"
          />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', margin: '20px 0' }}>
            <div style={{ flex: 1, height: '1px', background: 'var(--border)' }} />
            <span style={{ padding: '0 10px', fontSize: 13, color: 'var(--text-secondary)' }}>OR</span>
            <div style={{ flex: 1, height: '1px', background: 'var(--border)' }} />
        </div>

        <form onSubmit={handle}>
          <div className="form-group">
            <label htmlFor="auth-username">{t('username')}</label>
            <div style={{ position:'relative' }}>
              <User size={15} style={{ position:'absolute', left:12, top:'50%', transform:'translateY(-50%)', color:'var(--text-secondary)' }} />
              <input
                id="auth-username"
                className="form-input"
                style={{ paddingLeft:36 }}
                placeholder="e.g. analyst1"
                value={username}
                onChange={e => setUsername(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="auth-password">{t('password')}</label>
            <div style={{ position:'relative' }}>
              <Lock size={15} style={{ position:'absolute', left:12, top:'50%', transform:'translateY(-50%)', color:'var(--text-secondary)' }} />
              <input
                id="auth-password"
                className="form-input"
                style={{ paddingLeft:36, paddingRight:40 }}
                type={showPw ? 'text' : 'password'}
                placeholder="••••••••"
                value={password}
                onChange={e => setPassword(e.target.value)}
                required
              />
              <button
                type="button"
                onClick={() => setShowPw(v => !v)}
                style={{ position:'absolute', right:12, top:'50%', transform:'translateY(-50%)',
                  background:'none', border:'none', cursor:'pointer', color:'var(--text-secondary)' }}
              >
                {showPw ? <EyeOff size={15}/> : <Eye size={15}/>}
              </button>
            </div>
          </div>

          <button
            id="auth-submit"
            type="submit"
            className="btn btn-primary"
            style={{ width:'100%', justifyContent:'center', marginTop:8 }}
            disabled={loading}
          >
            {loading ? <span className="spinner"/> : (mode === 'login' ? t('signIn') : t('createAccount'))}
          </button>
        </form>

        <p style={{ textAlign:'center', marginTop:20, fontSize:13, color:'var(--text-secondary)' }}>
          {mode === 'login' ? t('dontHaveAccount') : t('alreadyHaveAccount')}
          <button
            id="auth-toggle"
            onClick={() => { setMode(m => m==='login'?'register':'login'); setError(''); }}
            style={{ background:'none', border:'none', cursor:'pointer', color:'var(--accent-blue)', fontWeight:600, marginLeft:6 }}
          >
            {mode === 'login' ? t('register') : t('signIn')}
          </button>
        </p>
      </div>
    </div>
  );
}
