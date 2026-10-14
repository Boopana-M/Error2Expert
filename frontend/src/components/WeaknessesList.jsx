import React from 'react';
import { ShieldAlert, AlertCircle, ArrowUpRight, Flame } from 'lucide-react';

export default function WeaknessesList({ weaknesses }) {
  const list = weaknesses || [];

  const getSeverityBadge = (severity) => {
    switch (severity?.toUpperCase()) {
      case 'HIGH':
        return <span className="badge badge-high"><Flame size={12} /> High Recurrence</span>;
      case 'MEDIUM':
        return <span className="badge badge-medium"><AlertCircle size={12} /> Medium</span>;
      case 'IMPROVING':
      default:
        return <span className="badge badge-improving"><ArrowUpRight size={12} /> Improving</span>;
    }
  };

  return (
    <div className="glass-panel weaknesses-container">
      <div className="section-title-row">
        <div className="flex items-center gap-2">
          <ShieldAlert size={20} className="text-rose" />
          <h2 className="section-title">Longitudinal Weaknesses</h2>
        </div>
        <span className="text-muted text-sm">Ranked by Cross-Problem Recurrence</span>
      </div>

      <p className="section-subtitle">
        Patterns detected across multiple distinct problems using FAISS dense vector clustering.
      </p>

      <div className="weakness-cards-stack">
        {list.map((item) => (
          <div key={item.id} className="weakness-item-card">
            <div className="weakness-header">
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="weakness-concept-title">{item.concept}</h3>
                  {getSeverityBadge(item.severity)}
                </div>
                <div className="weakness-category-sub">{item.mistakeCategory}</div>
              </div>
              <div className="weakness-stat-box">
                <div className="frequency-num">{item.frequency}x</div>
                <div className="text-muted text-xs">Occurrences</div>
              </div>
            </div>

            <p className="weakness-desc">{item.description}</p>

            <div className="affected-problems-container">
              <span className="affected-label">Identified in:</span>
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
                <span>First: <strong>{item.firstObserved}</strong></span>
                <span>Latest: <strong>{item.lastObserved}</strong></span>
              </div>
              <div className="flex items-center gap-2 text-xs">
                <span className="text-muted">Concept Mastery:</span>
                <strong className={item.masteryScore > 75 ? 'text-emerald' : item.masteryScore > 50 ? 'text-amber' : 'text-rose'}>
                  {item.masteryScore}%
                </strong>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
