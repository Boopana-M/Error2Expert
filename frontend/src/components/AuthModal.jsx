import React, { useState } from 'react';
import { Brain, Lock, Mail, User, AlertCircle, ArrowRight, ShieldCheck } from 'lucide-react';
import { api } from '../services/api';

export default function AuthModal({ isOpen, onClose, onSuccess }) {
  const [isRegister, setIsRegister] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [coldStartNotice, setColdStartNotice] = useState(false);

  React.useEffect(() => {
    let timer;
    if (loading) {
      timer = setTimeout(() => {
        setColdStartNotice(true);
      }, 3000);
    } else {
      setColdStartNotice(false);
    }
    return () => clearTimeout(timer);
  }, [loading]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    setColdStartNotice(false);

    try {
      let userData;
      if (isRegister) {
        if (!name.trim()) throw new Error('Please enter your name.');
        userData = await api.register(name, email, password);
      } else {
        userData = await api.login(email, password);
      }
      onSuccess(userData);
      onClose();
    } catch (err) {
      setError(err.message || 'Authentication failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-dialog glass-panel auth-dialog" style={{ maxWidth: '420px' }}>
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <div className="logo-badge" style={{ display: 'inline-flex', marginBottom: '12px' }}>
            <Brain className="logo-icon pulsing-indicator" size={32} />
          </div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 700, color: '#f8fafc', margin: 0 }}>
            {isRegister ? 'Create Your Account' : 'Welcome to Error2Expert'}
          </h2>
          <p style={{ color: '#94a3b8', fontSize: '0.85rem', marginTop: '6px' }}>
            {isRegister 
              ? 'Start fresh with an empty personal dashboard and AI second brain.' 
              : 'Sign in to access your personal mistake history and mastery analytics.'}
          </p>
        </div>

        {/* Tab Switcher */}
        <div style={{ display: 'flex', background: 'rgba(15, 23, 42, 0.6)', borderRadius: '8px', padding: '4px', marginBottom: '20px', border: '1px solid rgba(255, 255, 255, 0.08)' }}>
          <button
            type="button"
            onClick={() => { setIsRegister(false); setError(null); }}
            style={{
              flex: 1,
              padding: '8px',
              borderRadius: '6px',
              border: 'none',
              background: !isRegister ? 'rgba(99, 102, 241, 0.25)' : 'transparent',
              color: !isRegister ? '#ffffff' : '#94a3b8',
              fontWeight: !isRegister ? 600 : 400,
              fontSize: '0.85rem',
              cursor: 'pointer',
              transition: 'all 0.2s ease'
            }}
          >
            Sign In
          </button>
          <button
            type="button"
            onClick={() => { setIsRegister(true); setError(null); }}
            style={{
              flex: 1,
              padding: '8px',
              borderRadius: '6px',
              border: 'none',
              background: isRegister ? 'rgba(99, 102, 241, 0.25)' : 'transparent',
              color: isRegister ? '#ffffff' : '#94a3b8',
              fontWeight: isRegister ? 600 : 400,
              fontSize: '0.85rem',
              cursor: 'pointer',
              transition: 'all 0.2s ease'
            }}
          >
            Register Fresh
          </button>
        </div>

        {error && (
          <div className="status-banner error" style={{ marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertCircle size={16} />
            <span style={{ fontSize: '0.85rem' }}>{error}</span>
          </div>
        )}

        {coldStartNotice && (
          <div className="status-banner" style={{ marginBottom: '16px', background: 'rgba(56, 189, 248, 0.12)', border: '1px solid rgba(56, 189, 248, 0.3)', color: '#38bdf8', padding: '10px 12px', borderRadius: '8px', fontSize: '0.8rem', display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
            <span style={{ fontSize: '1rem', lineHeight: 1 }}>⏳</span>
            <div>
              <strong style={{ display: 'block', color: '#7dd3fc', marginBottom: '2px' }}>Waking backend from sleep...</strong>
              Render free-tier service is booting up. Initial cold-start takes ~30–45s, then all requests are instant.
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="modal-form">
          {isRegister && (
            <div className="form-group">
              <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <User size={13} className="text-primary-light" /> Full Name
              </label>
              <input
                type="text"
                className="form-input"
                placeholder="e.g. Alice"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>
          )}

          <div className="form-group">
            <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Mail size={13} className="text-primary-light" /> Email Address
            </label>
            <input
              type="email"
              className="form-input"
              placeholder="e.g. alice@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Lock size={13} className="text-primary-light" /> Password
            </label>
            <input
              type="password"
              className="form-input"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          <div style={{ marginTop: '20px' }}>
            <button
              id="btn-auth-submit"
              type="submit"
              disabled={loading}
              className="btn-primary"
              style={{ width: '100%', justifyContent: 'center', padding: '10px' }}
            >
              <span>{loading ? 'Authenticating...' : isRegister ? 'Register & Start Fresh' : 'Sign In'}</span>
              <ArrowRight size={16} />
            </button>
          </div>
        </form>

        <div style={{ marginTop: '16px', textAlign: 'center', fontSize: '0.75rem', color: '#64748b', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}>
          <ShieldCheck size={14} className="text-emerald" />
          <span>Multi-User Isolated JWT Security Context</span>
        </div>
      </div>
    </div>
  );
}
