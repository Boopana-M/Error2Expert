import React from 'react';
import { Target, CheckCircle2, AlertTriangle, RefreshCw, Award, TrendingDown } from 'lucide-react';

export default function OverviewCards({ summary }) {
  const {
    problemsAttempted = 0,
    problemsAccepted = 0,
    totalMistakes = 0,
    recurringMistakes = 0,
    strongestConcept = "N/A",
    weakestConcept = "N/A",
    currentMastery = 70
  } = summary || {};

  const acceptanceRate = problemsAttempted > 0 
    ? Math.round((problemsAccepted / problemsAttempted) * 100) 
    : 0;

  return (
    <div className="overview-grid">
      {/* Mastery Score Card */}
      <div className="glass-panel stat-card mastery-card">
        <div className="stat-header">
          <span className="stat-label">Longitudinal Mastery</span>
          <Award size={18} className="stat-icon-indigo" />
        </div>
        <div className="mastery-container">
          <div className="mastery-score-display">
            <span className="mastery-val">{currentMastery}</span>
            <span className="mastery-max">/100</span>
          </div>
          <div className="mastery-bar-track">
            <div 
              className="mastery-bar-fill" 
              style={{ width: `${Math.min(100, Math.max(0, currentMastery))}%` }}
            />
          </div>
        </div>
        <p className="stat-subtext">
          Targeted practice bonus applied. <strong>+{currentMastery > 70 ? '8' : '3'} pts</strong> this month.
        </p>
      </div>

      {/* Attempts & Solves */}
      <div className="glass-panel stat-card">
        <div className="stat-header">
          <span className="stat-label">Problems Attempted</span>
          <Target size={18} className="stat-icon-cyan" />
        </div>
        <div className="stat-value">{problemsAttempted}</div>
        <div className="stat-sub-row">
          <span className="text-emerald inline-flex items-center gap-1">
            <CheckCircle2 size={13} /> {problemsAccepted} Solved ({acceptanceRate}%)
          </span>
        </div>
      </div>

      {/* Mistakes & Recurring */}
      <div className="glass-panel stat-card">
        <div className="stat-header">
          <span className="stat-label">Mistakes Tracked</span>
          <AlertTriangle size={18} className="stat-icon-rose" />
        </div>
        <div className="stat-value text-rose">{totalMistakes}</div>
        <div className="stat-sub-row">
          <span className="text-amber inline-flex items-center gap-1" title="Mistakes repeating across multiple problems">
            <RefreshCw size={13} /> <strong>{recurringMistakes} Recurring Patterns</strong>
          </span>
        </div>
      </div>

      {/* Focus & Weakness Highlight */}
      <div className="glass-panel stat-card">
        <div className="stat-header">
          <span className="stat-label">Weakest Concept</span>
          <TrendingDown size={18} className="stat-icon-rose" />
        </div>
        <div className="concept-pill text-rose">
          {weakestConcept}
        </div>
        <div className="stat-sub-row">
          <span className="text-muted">Strongest: <strong className="text-emerald">{strongestConcept}</strong></span>
        </div>
      </div>
    </div>
  );
}
