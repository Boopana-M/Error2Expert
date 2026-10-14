import React from 'react';
import { Calendar, TrendingDown, Check } from 'lucide-react';

export default function TimelineTrends({ timeline }) {
  const data = timeline || [];

  return (
    <div className="glass-panel timeline-container">
      <div className="section-title-row">
        <div className="flex items-center gap-2">
          <Calendar size={20} className="text-primary-light" />
          <h2 className="section-title">Mistake Evolution Timeline</h2>
        </div>
        <div className="trend-badge text-emerald inline-flex items-center gap-1">
          <TrendingDown size={14} /> -71% Mistake Reduction
        </div>
      </div>

      <p className="section-subtitle">
        Tracking error frequency over consecutive practice periods to verify longitudinal retention.
      </p>

      <div className="timeline-grid">
        {data.map((item, idx) => (
          <div key={item.period || idx} className="timeline-period-card">
            <div className="period-title">{item.period}</div>
            
            <div className="period-total">
              <span className="total-num">{item.total}</span>
              <span className="total-sub">Total Mistakes</span>
            </div>

            <div className="period-breakdown">
              <div className="breakdown-row">
                <span className="text-muted">Boundary:</span>
                <span className="font-mono text-rose">{item.boundary}</span>
              </div>
              <div className="breakdown-row">
                <span className="text-muted">Complexity:</span>
                <span className="font-mono text-amber">{item.complexity}</span>
              </div>
              <div className="breakdown-row">
                <span className="text-muted">Edge Case:</span>
                <span className="font-mono text-cyan">{item.edgeCase}</span>
              </div>
            </div>

            {idx === data.length - 1 && (
              <div className="improvement-notice">
                <Check size={14} className="text-emerald" />
                <span>Significant Boundary Mastery</span>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
