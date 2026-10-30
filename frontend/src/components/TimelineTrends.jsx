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
        {data.length > 1 && (
          <div className="trend-badge text-emerald inline-flex items-center gap-1">
            <TrendingDown size={14} /> Tracking Longitudinal Retention
          </div>
        )}
      </div>

      <p className="section-subtitle">
        Tracking error frequency over consecutive practice periods to verify longitudinal retention.
      </p>

      {data.length === 0 ? (
        <div className="empty-state-box">
          <Calendar size={30} className="text-muted" style={{ opacity: 0.5, marginBottom: '8px' }} />
          <div style={{ color: '#94a3b8', fontWeight: 500, fontSize: '0.9rem' }}>No timeline activity yet</div>
          <p style={{ color: '#64748b', fontSize: '0.8rem', marginTop: '4px' }}>
            As you practice across multiple days and months, error reduction trends over time will appear here.
          </p>
        </div>
      ) : (
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

              {idx === data.length - 1 && data.length > 1 && (
                <div className="improvement-notice">
                  <Check size={14} className="text-emerald" />
                  <span>Latest Active Period</span>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
