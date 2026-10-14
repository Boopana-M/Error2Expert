import React from 'react';
import { Target, ExternalLink, Compass, CheckCircle } from 'lucide-react';

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

  return (
    <div className="glass-panel recommendations-container">
      {/* Today's Focus Banner */}
      <div className="focus-hero-banner">
        <div className="focus-hero-header">
          <div className="flex items-center gap-2">
            <span className="focus-pill">
              <Target size={14} /> Today's Adaptive Focus
            </span>
            <h2 className="focus-concept-heading">{weakness || "Targeted Practice"}</h2>
          </div>
          <Compass size={22} className="text-cyan pulsing-indicator" />
        </div>

        <div className="focus-why-box">
          <strong className="text-primary-light">Why this topic? </strong>
          <span className="text-secondary">{why}</span>
        </div>
      </div>

      {/* Recommended Practice Problems */}
      <div className="recommendations-list-wrapper">
        <h3 className="rec-section-heading">Targeted Practice Curriculum</h3>
        <p className="rec-section-sub">
          Problems selected specifically to reinforce boundary termination invariants without recurrence.
        </p>

        <div className="problems-grid">
          {recommendedProblems.map((problem) => (
            <div key={problem.id} className="problem-card">
              <div className="problem-top-row">
                <div className="problem-number-title">
                  <span className="problem-num font-mono">#{problem.id}</span>
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
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
