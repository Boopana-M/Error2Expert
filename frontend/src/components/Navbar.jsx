import React from 'react';
import { Brain, Cpu, Database, PlusCircle, LogOut, LogIn, User } from 'lucide-react';

export default function Navbar({ user, onLogout, onOpenAuthModal, onOpenSubmissionModal }) {
  const initial = user?.name ? user.name.charAt(0).toUpperCase() : '?';

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
