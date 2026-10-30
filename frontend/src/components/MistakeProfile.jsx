import React from 'react';
import { PieChart, Info, CheckCircle2 } from 'lucide-react';

export default function MistakeProfile({ profile }) {
  const items = profile || [];

  return (
    <div className="glass-panel profile-container">
      <div className="section-title-row">
        <div className="flex items-center gap-2">
          <PieChart size={20} className="text-cyan" />
          <h2 className="section-title">Your Mistake Profile</h2>
        </div>
        <span className="info-tag" title="Aggregated from longitudinal LeetCode failure analyses">
          <Info size={14} /> Semantic Category Breakdown
        </span>
      </div>

      <p className="section-subtitle">
        Categorization inferred by rule-based heuristic & FAISS semantic similarity matching.
      </p>

      {items.length === 0 ? (
        <div className="empty-state-box">
          <CheckCircle2 size={30} className="text-muted" style={{ opacity: 0.5, marginBottom: '8px' }} />
          <div style={{ color: '#94a3b8', fontWeight: 500, fontSize: '0.9rem' }}>No mistakes recorded yet</div>
          <p style={{ color: '#64748b', fontSize: '0.8rem', marginTop: '4px' }}>
            Log your first problem attempt to begin building your personal semantic mistake profile.
          </p>
        </div>
      ) : (
        <div className="profile-bars-list">
          {items.map((item, idx) => (
            <div key={item.category || idx} className="profile-item">
              <div className="profile-item-header">
                <span className="profile-item-label">{item.label || item.category}</span>
                <div className="profile-item-stats">
                  <span className="profile-count">{item.count} mistakes</span>
                  <span className="profile-pct" style={{ color: item.color || '#818cf8' }}>
                    {item.percentage}%
                  </span>
                </div>
              </div>
              <div className="bar-track">
                <div
                  className="bar-fill"
                  style={{
                    width: `${item.percentage}%`,
                    backgroundColor: item.color || '#6366f1',
                    boxShadow: `0 0 10px ${item.color || '#6366f1'}44`
                  }}
                />
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
