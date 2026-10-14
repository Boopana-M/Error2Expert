import React from 'react';
import { PieChart, Info } from 'lucide-react';

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
    </div>
  );
}
