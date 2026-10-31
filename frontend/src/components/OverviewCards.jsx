import React from 'react';
import { Target, CheckCircle2, AlertTriangle, RefreshCw, Award, Sparkles, Compass, ShieldCheck } from 'lucide-react';

export default function OverviewCards({ summary }) {
  const {
    problemsAttempted = 0,
    problemsAccepted = 0,
    totalMistakes = 0,
    recurringMistakes = 0,
    resolvedMistakes = 0,
    openMistakes = 0,
    recoveryRate = 100,
    learningVerdict = "",
    strongestConcept = "—",
    weakestConcept = "—",
    currentMastery = 0
  } = summary || {};

  const acceptanceRate = problemsAttempted > 0 
    ? Math.round((problemsAccepted / problemsAttempted) * 100) 
    : 0;

  const isAllConquered = weakestConcept === "All Resolved ✓" || (totalMistakes > 0 && openMistakes === 0);

  return (
    <div className="overview-section">
      <div className="overview-grid">
        {/* 1. Longitudinal Mastery Card */}
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
            {problemsAttempted > 0 ? (
              <span>Recency-weighted trajectory. Resolving past errors accelerates score recovery.</span>
            ) : (
              <span>Solve and log attempts to track longitudinal mastery growth.</span>
            )}
          </p>
        </div>

        {/* 2. Cognitive Recovery Rate Card (New Star Metric) */}
        <div className="glass-panel stat-card recovery-card">
          <div className="stat-header">
            <span className="stat-label">Recovery Rate</span>
            <Sparkles size={18} className="text-emerald pulsing-indicator" />
          </div>
          <div className="mastery-container">
            <div className="mastery-score-display">
              <span className="recovery-val text-emerald">{totalMistakes > 0 ? `${recoveryRate}%` : "100%"}</span>
            </div>
            <div className="mastery-bar-track">
              <div 
                className="recovery-bar-fill" 
                style={{ 
                  width: `${totalMistakes > 0 ? Math.min(100, Math.max(0, recoveryRate)) : 100}%`,
                  background: recoveryRate >= 70 
                    ? 'linear-gradient(90deg, #10b981, #06b6d4)' 
                    : recoveryRate >= 40 
                      ? 'linear-gradient(90deg, #f59e0b, #10b981)' 
                      : 'linear-gradient(90deg, #f43f5e, #f59e0b)'
                }}
              />
            </div>
          </div>
          <p className="stat-subtext">
            {totalMistakes > 0 ? (
              <span className="text-secondary">
                <strong className="text-emerald">{resolvedMistakes} resolved</strong> of {totalMistakes} total mistakes ({openMistakes} open).
              </span>
            ) : (
              <span className="text-muted">Clean submissions. Zero unresolved failure modes.</span>
            )}
          </p>
        </div>

        {/* 3. Problems Attempted & Solved */}
        <div className="glass-panel stat-card">
          <div className="stat-header">
            <span className="stat-label">Problems Attempted</span>
            <Target size={18} className="stat-icon-cyan" />
          </div>
          <div className="stat-value">{problemsAttempted}</div>
          <div className="stat-sub-row">
            <span className="text-emerald inline-flex items-center gap-1 font-semibold">
              <CheckCircle2 size={14} /> {problemsAccepted} Solved ({acceptanceRate}%)
            </span>
          </div>
        </div>

        {/* 4. Active Learning Focus */}
        <div className="glass-panel stat-card">
          <div className="stat-header">
            <span className="stat-label">Active Learning Focus</span>
            {isAllConquered ? (
              <ShieldCheck size={18} className="text-emerald" />
            ) : (
              <Compass size={18} className="stat-icon-rose" />
            )}
          </div>
          <div className={`concept-pill ${isAllConquered ? 'text-emerald' : 'text-primary-light'}`}>
            {isAllConquered ? "All Conquered ✓" : (weakestConcept || "—")}
          </div>
          <div className="stat-sub-row">
            {isAllConquered ? (
              <span className="text-emerald text-xs">All detected mistake patterns successfully resolved!</span>
            ) : (
              <span className="text-muted text-xs">Strongest: <strong className="text-emerald">{strongestConcept || "—"}</strong></span>
            )}
          </div>
        </div>
      </div>

      {/* 5. Longitudinal Learning Verdict Banner */}
      {learningVerdict && (
        <div className="learning-verdict-banner glass-panel">
          <div className="verdict-icon-glow">
            <Sparkles size={18} className="text-cyan" />
          </div>
          <div className="verdict-text-box">
            <span className="verdict-badge">AI Longitudinal Analysis</span>
            <p className="verdict-sentence">{learningVerdict}</p>
          </div>
        </div>
      )}
    </div>
  );
}
