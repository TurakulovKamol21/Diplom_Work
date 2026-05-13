import React from 'react';

export default class AppErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Application crashed:', error, errorInfo);
  }

  handleReload = () => {
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      return (
        <div
          style={{
            minHeight: '100vh',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 24,
            background: '#eef3ff',
            color: '#0f172a',
            fontFamily: 'Inter, sans-serif'
          }}
        >
          <div
            style={{
              width: '100%',
              maxWidth: 560,
              background: '#ffffff',
              border: '1px solid rgba(59,130,246,0.14)',
              borderRadius: 20,
              padding: 28,
              boxShadow: '0 18px 40px rgba(15,23,42,0.08)'
            }}
          >
            <h1 style={{ fontSize: 24, marginBottom: 12 }}>Frontend runtime xatosi</h1>
            <p style={{ color: '#475569', lineHeight: 1.6, marginBottom: 16 }}>
              Ilova render vaqtida to‘xtab qoldi. Sahifa endi oq fon bilan qolib ketmaydi; quyidagi xabar real xatoni ko‘rsatadi.
            </p>
            <pre
              style={{
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                background: '#f8fafc',
                borderRadius: 12,
                padding: 14,
                fontSize: 13,
                color: '#334155',
                marginBottom: 18
              }}
            >
              {this.state.error?.message || 'Unknown render error'}
            </pre>
            <button
              type="button"
              onClick={this.handleReload}
              style={{
                background: 'linear-gradient(135deg, #2563eb, #0891b2)',
                color: '#fff',
                border: 'none',
                borderRadius: 10,
                padding: '10px 16px',
                cursor: 'pointer',
                fontWeight: 600
              }}
            >
              Sahifani qayta yuklash
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
