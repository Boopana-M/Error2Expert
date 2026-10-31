import React, { useState } from 'react';
import { X, Send, AlertCircle, CheckCircle } from 'lucide-react';
import { api } from '../services/api';

export default function SubmissionModal({ isOpen, onClose, onSuccess }) {
  const [formData, setFormData] = useState({
    problemTitle: 'Binary Search',
    problemUrl: 'https://leetcode.com/problems/binary-search/',
    difficulty: 'EASY',
    result: 'WRONG_ANSWER',
    language: 'Java',
    code: `class Solution {\n    public int search(int[] nums, int target) {\n        int left = 0, right = nums.length - 1;\n        while (left < right) {\n            int mid = left + (right - left) / 2;\n            if (nums[mid] == target) return mid;\n            else if (nums[mid] < target) left = mid;\n            else right = mid;\n        }\n        return -1;\n    }\n}`,
    userExplanation: 'I updated left = mid instead of left = mid + 1, causing an infinite loop on boundary condition.'
  });

  const [loading, setLoading] = useState(false);
  const [statusMessage, setStatusMessage] = useState(null);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setStatusMessage(null);

    try {
      await api.submitAttempt(formData);
      setStatusMessage({ type: 'success', text: 'Attempt analyzed & stored successfully!' });
      setTimeout(() => {
        onSuccess();
        onClose();
      }, 1000);
    } catch (err) {
      setStatusMessage({ type: 'error', text: err.message || 'Failed to submit attempt. Check backend connection.' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-dialog glass-panel">
        <div className="modal-header">
          <div className="flex items-center gap-2">
            <h2 className="modal-title">Log LeetCode Attempt</h2>
            <span className="badge badge-medium">Simulation / API Test</span>
          </div>
          <button id="btn-close-modal" onClick={onClose} className="modal-close-btn">
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-form">
          <div className="form-row">
            <div className="form-group flex-1">
              <label htmlFor="input-problem-title" className="form-label">Problem Title</label>
              <input
                id="input-problem-title"
                type="text"
                className="form-input"
                value={formData.problemTitle}
                onChange={(e) => setFormData({ ...formData, problemTitle: e.target.value })}
                required
              />
            </div>
            <div className="form-group w-36">
              <label htmlFor="select-difficulty" className="form-label">Difficulty</label>
              <select
                id="select-difficulty"
                className="form-select"
                value={formData.difficulty}
                onChange={(e) => setFormData({ ...formData, difficulty: e.target.value })}
              >
                <option value="EASY">Easy</option>
                <option value="MEDIUM">Medium</option>
                <option value="HARD">Hard</option>
              </select>
            </div>
          </div>

          <div className="form-row">
            <div className="form-group flex-1">
              <label htmlFor="select-result" className="form-label">Result</label>
              <select
                id="select-result"
                className="form-select"
                value={formData.result}
                onChange={(e) => setFormData({ ...formData, result: e.target.value })}
              >
                <option value="WRONG_ANSWER">Wrong Answer</option>
                <option value="TIME_LIMIT_EXCEEDED">Time Limit Exceeded (TLE)</option>
                <option value="RUNTIME_ERROR">Runtime Error</option>
                <option value="COMPILE_ERROR">Compile Error</option>
                <option value="ACCEPTED">Accepted</option>
              </select>
            </div>
            <div className="form-group w-36">
              <label htmlFor="input-language" className="form-label">Language</label>
              <input
                id="input-language"
                type="text"
                className="form-input"
                value={formData.language}
                onChange={(e) => setFormData({ ...formData, language: e.target.value })}
              />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="textarea-user-explanation" className="form-label">
              {formData.result === 'ACCEPTED' ? (
                <span className="text-emerald font-semibold">Solution Reflection / Fix Note (Optional)</span>
              ) : (
                <>Why do you think this failed? <span className="text-primary-light">(Crucial for AI ML model)</span></>
              )}
            </label>
            <input
              id="textarea-user-explanation"
              type="text"
              className="form-input"
              placeholder={formData.result === 'ACCEPTED' ? "e.g. Corrected boundary update using high = mid - 1" : "e.g. I updated high = mid instead of high = mid - 1"}
              value={formData.userExplanation}
              onChange={(e) => setFormData({ ...formData, userExplanation: e.target.value })}
            />
            {formData.result === 'ACCEPTED' && (
              <span className="text-xs text-emerald" style={{ marginTop: '4px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                <CheckCircle size={12} /> Logging an ACCEPTED solution automatically resolves open mistakes on this problem and boosts concept mastery!
              </span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="textarea-code" className="form-label">Submitted Code</label>
            <textarea
              id="textarea-code"
              rows={6}
              className="form-textarea font-mono text-xs"
              value={formData.code}
              onChange={(e) => setFormData({ ...formData, code: e.target.value })}
              required
            />
          </div>

          {statusMessage && (
            <div className={`status-banner ${statusMessage.type}`}>
              {statusMessage.type === 'success' ? <CheckCircle size={16} /> : <AlertCircle size={16} />}
              <span>{statusMessage.text}</span>
            </div>
          )}

          <div className="modal-actions">
            <button type="button" onClick={onClose} className="btn-secondary">
              Cancel
            </button>
            <button
              id="btn-submit-attempt"
              type="submit"
              disabled={loading}
              className="btn-primary"
            >
              <Send size={15} />
              <span>{loading ? 'Analyzing with ML...' : 'Analyze & Save Attempt'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
