import React, { useState } from 'react';
import { Brain, Cpu, Database, PlusCircle, LogOut, LogIn, User, RotateCcw, Download, Key } from 'lucide-react';

export default function Navbar({ user, onLogout, onResetDashboard, onOpenAuthModal, onOpenSubmissionModal }) {
  const initial = user?.name ? user.name.charAt(0).toUpperCase() : '?';
  const [syncStatus, setSyncStatus] = useState(false);

  const handleSyncExtension = () => {
    const token = localStorage.getItem('codememory_token');
    if (token) {
      window.postMessage({ type: 'ERROR2EXPERT_AUTH_SYNC', token, user }, '*');
      window.dispatchEvent(new CustomEvent('ERROR2EXPERT_AUTH_SYNC', { detail: { token, user } }));
      navigator.clipboard?.writeText(token);
      setSyncStatus(true);
      setTimeout(() => setSyncStatus(false), 3000);
    }
  };

  return (
    <header className="navbar-container">
      <div className="navbar-left">
        <div className="logo-badge">
          <Brain className="logo-icon pulsing-indicator" size={26} />
        </div>
        <div>
          <div className="logo-text">Error2Expert</div>
          <div className="logo-subtext">AI Longitudinal Error Modeling & Second Brain</div>
        </div>
      </div>

      <div className="navbar-right">
        <a
          href="/error2expert-extension.zip"
          download="error2expert-extension.zip"
          className="btn-secondary btn-sm"
          style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', color: '#38bdf8', borderColor: 'rgba(56, 189, 248, 0.3)' }}
          title="Download Chrome Extension (ZIP) for LeetCode"
        >
          <Download size={14} />
          <span>Get Extension</span>
        </a>

        {user && (
          <button
            onClick={handleSyncExtension}
            className="btn-secondary btn-sm"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              color: syncStatus ? '#34d399' : '#a855f7',
              borderColor: syncStatus ? 'rgba(52, 211, 153, 0.4)' : 'rgba(168, 85, 247, 0.3)'
            }}
            title="Click to sync JWT token with Error2Expert Chrome Extension or copy to clipboard"
          >
            <Key size={14} />
            <span>{syncStatus ? 'Token Copied & Synced ✓' : 'Sync Extension'}</span>
          </button>
        )}

        <div className="engine-status" title="FAISS FlatIP L2 Unit-Vector Memory">
          <Database size={15} className="status-icon-purple" />
          <span>Vector Memory: <strong className="text-purple">User-Isolated</strong></span>
        </div>
        <div className="engine-status" title="FastAPI Rule & Embedding Classifier">
          <Cpu size={15} className="status-icon-emerald" />
          <span>ML Service: <strong className="text-emerald">Online</strong></span>
        </div>

        {user ? (
          <>
            <div className="user-profile-badge" title={`Signed in as ${user.email}`}>
              <span className="user-avatar-circle">{initial}</span>
              <span className="user-name-label">{user.name}</span>
            </div>

            <button
              id="btn-quick-submit"
              onClick={onOpenSubmissionModal}
              className="btn-primary"
              title="Simulate or Log a new LeetCode attempt"
            >
              <PlusCircle size={16} />
              <span>Log Attempt</span>
            </button>

            {onResetDashboard && (
              <button
                id="btn-reset-dashboard"
                onClick={onResetDashboard}
                className="btn-secondary btn-sm"
                style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', color: '#94a3b8' }}
                title="Wipe mock/demo data and reset dashboard to completely empty"
              >
                <RotateCcw size={14} />
                <span>Reset to Empty</span>
              </button>
            )}

            <button
              id="btn-logout"
              onClick={onLogout}
              className="btn-secondary btn-sm"
              style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', color: '#94a3b8' }}
              title="Sign out of current account"
            >
              <LogOut size={14} />
              <span>Logout</span>
            </button>
          </>
        ) : (
          <button
            id="btn-open-auth"
            onClick={onOpenAuthModal}
            className="btn-primary"
            title="Sign in or register a new user"
          >
            <LogIn size={16} />
            <span>Sign In / Register</span>
          </button>
        )}
      </div>
    </header>
  );
}
