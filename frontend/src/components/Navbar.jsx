import React from 'react';
import { Brain, Cpu, Database, PlusCircle } from 'lucide-react';

export default function Navbar({ onOpenSubmissionModal }) {
  return (
    <header className="navbar-container">
      <div className="navbar-left">
        <div className="logo-badge">
          <Brain className="logo-icon pulsing-indicator" size={26} />
        </div>
        <div>
          <div className="logo-text">CodeMemory</div>
          <div className="logo-subtext">AI Longitudinal Error Modeling & Second Brain</div>
        </div>
      </div>

      <div className="navbar-right">
        <div className="engine-status" title="FAISS FlatIP L2 Unit-Vector Memory">
          <Database size={15} className="status-icon-purple" />
          <span>FAISS Vector Memory: <strong className="text-purple">Active (384-d)</strong></span>
        </div>
        <div className="engine-status" title="FastAPI Rule & Embedding Classifier">
          <Cpu size={15} className="status-icon-emerald" />
          <span>ML Service: <strong className="text-emerald">Online</strong></span>
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
      </div>
    </header>
  );
}
