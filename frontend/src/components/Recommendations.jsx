import React from 'react';
import { Target, ExternalLink, Compass, BookOpen } from 'lucide-react';

export default function Recommendations({ focusData }) {
  const { weakness, why, recommendedProblems = [] } = focusData || {};

  const getDifficultyBadge = (diff) => {
    switch (diff?.toUpperCase()) {
      case 'EASY':
        return <span className="badge badge-easy">Easy</span>;
      case 'MEDIUM':
        return <span className="badge badge-medium-diff">Medium</span>;
      case 'HARD':
        return <span className="badge badge-hard">Hard</span>;
      default:
        return <span className="badge">{diff}</span>;
    }
  };

  const hasFocus = weakness && weakness !== '—' && weakness !== 'General Algorithmic Foundations';

  return (
    <div className="glass-panel recommendations-container">
      {/* Today's Focus Banner */}
      <div className="focus-hero-banner">
        <div className="focus-hero-header">
          <div className="flex items-center gap-2">
            <span className="focus-pill">
              <Target size={14} /> Today's Adaptive Focus
            </span>
            <h2 className="focus-concept-heading">{hasFocus ? weakness : "Targeted Curriculum"}</h2>
          </div>
          <Compass size={22} className="text-cyan pulsing-indicator" />
        </div>

        <div className="focus-why-box">
          <strong className="text-primary-light">Why this focus? </strong>
          <span className="text-secondary">
            {why || "Solve LeetCode problems to receive personalized targeted recommendations calibrated to your mistake patterns."}
          </span>
        </div>
      </div>

      {/* Recommended Practice Problems */}
      <div className="recommendations-list-wrapper">
        <h3 className="rec-section-heading">Targeted Practice Curriculum</h3>
        <p className="rec-section-sub">
          Problems selected specifically to reinforce algorithmic invariants and overcome detected failure modes.
        </p>

        {recommendedProblems.length === 0 ? (
          <div className="empty-state-box">
            <BookOpen size={30} className="text-muted" style={{ opacity: 0.5, marginBottom: '8px' }} />
            <div style={{ color: '#94a3b8', fontWeight: 500, fontSize: '0.9rem' }}>No active recommendations</div>
            <p style={{ color: '#64748b', fontSize: '0.8rem', marginTop: '4px' }}>
              When Error2Expert detects a recurring mistake pattern across multiple problems, high-yield practice exercises will appear here.
            </p>
          </div>
        ) : (
          <div className="problems-grid">
            {recommendedProblems.map((problem) => (
              <div key={problem.id} className="problem-card">
                <div className="problem-top-row">
                  <div className="problem-number-title">
                    <span className="problem-num font-mono">#{problem.leetcodeId || problem.id}</span>
                    <span className="problem-title">{problem.title}</span>
                  </div>
                  {getDifficultyBadge(problem.difficulty)}
                </div>

                <p className="problem-reason">{problem.reason}</p>

                <div className="problem-card-footer">
                  <div className="problem-topics-row">
                    {problem.topics?.map((t, idx) => (
                      <span key={idx} className="topic-pill">{t}</span>
                    ))}
                  </div>

                  {problem.url && (
                    <a
                      href={problem.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="btn-secondary btn-sm"
                      id={`link-problem-${problem.id}`}
                    >
                      <span>Solve on LeetCode</span>
                      <ExternalLink size={13} />
                    </a>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
