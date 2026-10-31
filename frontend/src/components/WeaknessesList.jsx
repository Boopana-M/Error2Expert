import React from 'react';
import { ShieldAlert, AlertCircle, ArrowUpRight, Flame, Award, CheckCircle2, TrendingUp } from 'lucide-react';

export default function WeaknessesList({ weaknesses }) {
  const list = weaknesses || [];

  const getLearningStateBadge = (state, severity) => {
    switch (state?.toUpperCase()) {
      case 'MASTERED':
        return <span className="badge badge-mastered"><Award size={12} /> Mastered</span>;
      case 'IMPROVING':
        return <span className="badge badge-improving"><ArrowUpRight size={12} /> Improving</span>;
      case 'RECURRING_WEAKNESS':
        return <span className="badge badge-high"><Flame size={12} /> Recurring Weakness</span>;
      case 'LEARNING_IN_PROGRESS':
      default:
        if (severity?.toUpperCase() === 'HIGH') {
          return <span className="badge badge-high"><Flame size={12} /> Needs Practice</span>;
        }
        return <span className="badge badge-medium"><TrendingUp size={12} /> In Progress</span>;
    }
  };

  return (
    <div className="glass-panel weaknesses-container">
      <div className="section-title-row">
        <div className="flex items-center gap-2">
          <ShieldAlert size={20} className="text-cyan" />
          <h2 className="section-title">Concept Learning & Recovery Monitor</h2>
        </div>
        <span className="text-muted text-sm">Longitudinal Error-to-Mastery Tracking</span>
      </div>

      <p className="section-subtitle">
        Tracks whether coding mistakes are actively resolved or recurring across exercises.
      </p>

      {list.length === 0 ? (
        <div className="empty-state-box">
          <CheckCircle2 size={30} className="text-emerald" style={{ opacity: 0.7, marginBottom: '8px' }} />
          <div style={{ color: '#94a3b8', fontWeight: 500, fontSize: '0.9rem' }}>No active conceptual hurdles detected</div>
          <p style={{ color: '#64748b', fontSize: '0.8rem', marginTop: '4px' }}>
            As you practice on LeetCode, Error2Expert tracks mistake resolution and rewards successful recoveries.
          </p>
        </div>
      ) : (
        <div className="weakness-cards-stack">
          {list.map((item) => {
            const recoveryPct = item.recoveryRate != null ? item.recoveryRate : (item.resolvedCount && item.frequency ? Math.round((item.resolvedCount / item.frequency) * 100) : 0);
            const isHighRecovery = recoveryPct >= 70;

            return (
              <div key={item.id} className="weakness-item-card">
                <div className="weakness-header">
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="weakness-concept-title">{item.concept}</h3>
                      {getLearningStateBadge(item.learningState, item.severity)}
                    </div>
                    <div className="weakness-category-sub">{item.mistakeCategory}</div>
                  </div>

                  {/* Recovery Rate Gauge */}
                  <div className="concept-recovery-stat">
                    <div className="concept-recovery-header">
                      <span className="text-xs text-muted">Recovery Rate:</span>
                      <strong className={isHighRecovery ? "text-emerald" : recoveryPct >= 40 ? "text-amber" : "text-rose"}>
                        {recoveryPct}%
                      </strong>
                    </div>
                    <div className="concept-recovery-bar">
                      <div 
                        className="concept-recovery-fill" 
                        style={{ 
                          width: `${Math.min(100, Math.max(0, recoveryPct))}%`,
                          background: isHighRecovery 
                            ? 'linear-gradient(90deg, #10b981, #06b6d4)' 
                            : recoveryPct >= 40 
                              ? 'linear-gradient(90deg, #f59e0b, #10b981)' 
                              : 'linear-gradient(90deg, #f43f5e, #f59e0b)'
                        }}
                      />
                    </div>
                    <div className="text-xs text-muted text-right mt-1">
                      {item.resolvedCount || 0} Resolved / {item.frequency || 1} Total
                    </div>
                  </div>
                </div>

                {/* AI Learning Narrative */}
                {item.learningNarrative && (
                  <div className="concept-narrative-box">
                    <p className="narrative-text">
                      {item.learningNarrative}
                    </p>
                  </div>
                )}

                <div className="affected-problems-container">
                  <span className="affected-label">Exercises:</span>
                  <div className="problem-tags-row">
                    {item.affectedProblems?.map((prob, i) => (
                      <span key={i} className="problem-tag">
                        {prob}
                      </span>
                    ))}
                  </div>
                </div>

                <div className="weakness-footer">
                  <div className="flex items-center gap-4 text-xs text-muted">
                    <span>First Seen: <strong>{item.firstObserved ? item.firstObserved.substring(0, 10) : '—'}</strong></span>
                    <span>Recent Attempt: <strong>{item.lastObserved ? item.lastObserved.substring(0, 10) : '—'}</strong></span>
                  </div>
                  <div className="flex items-center gap-2 text-xs">
                    <span className="text-muted">Mastery Score:</span>
                    <strong className={item.masteryScore > 75 ? 'text-emerald' : item.masteryScore > 50 ? 'text-amber' : 'text-rose'}>
                      {Math.round(item.masteryScore || 0)}%
                    </strong>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
