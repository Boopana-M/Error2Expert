import { ExtensionApi } from '../services/api.js';

document.addEventListener('DOMContentLoaded', async () => {
  const problemTitleEl = document.getElementById('problem-title');
  const problemDifficultyEl = document.getElementById('problem-difficulty');
  const submissionResultEl = document.getElementById('submission-result');
  const pageStatusEl = document.getElementById('page-status');
  const backendStatusEl = document.getElementById('backend-status');
  const statusTextEl = document.getElementById('status-text');
  const explanationInput = document.getElementById('user-explanation');
  const mistakeForm = document.getElementById('mistake-form');
  const btnAnalyze = document.getElementById('btn-analyze');
  const aiVerdictBox = document.getElementById('ai-verdict');
  const verdictCategoryEl = document.getElementById('verdict-category');
  const verdictConfidenceEl = document.getElementById('verdict-confidence');
  const verdictFeedbackEl = document.getElementById('verdict-feedback');
  const btnOpenDashboard = document.getElementById('btn-open-dashboard');

  let currentContext = {
    problemTitle: 'Binary Search',
    difficulty: 'EASY',
    result: 'WRONG_ANSWER',
    code: '// Submitted binary search attempt',
    url: 'https://leetcode.com/problems/binary-search/'
  };

  // 1. Clear alert badge
  chrome.runtime?.sendMessage?.({ action: 'CLEAR_BADGE' });

  // 2. Health check backend
  ExtensionApi.checkHealth().then(isOnline => {
    if (isOnline) {
      backendStatusEl.classList.add('online');
      statusTextEl.textContent = 'API Connected';
    } else {
      backendStatusEl.classList.remove('online');
      statusTextEl.textContent = 'Offline (Mock)';
    }
  });

  // 3. Query active tab for live LeetCode data
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (tab && tab.url && tab.url.includes('leetcode.com/problems/')) {
      chrome.tabs.sendMessage(tab.id, { action: 'GET_PROBLEM_CONTEXT' }, (response) => {
        if (response && response.success) {
          const { metadata, code } = response;
          currentContext.problemTitle = metadata.title;
          currentContext.difficulty = metadata.difficulty;
          currentContext.code = code;
          currentContext.url = metadata.url;

          problemTitleEl.textContent = metadata.title;
          problemDifficultyEl.textContent = metadata.difficulty.substring(0, 3);
          problemDifficultyEl.className = `badge badge-${metadata.difficulty.toLowerCase()}`;
          pageStatusEl.textContent = 'Active LeetCode Session';
        }
      });
    } else {
      // Check last cached submission
      chrome.storage.local.get(['lastSubmission'], (res) => {
        if (res.lastSubmission) {
          currentContext = { ...currentContext, ...res.lastSubmission };
          problemTitleEl.textContent = currentContext.problemTitle;
          problemDifficultyEl.textContent = currentContext.difficulty.substring(0, 3);
          problemDifficultyEl.className = `badge badge-${currentContext.difficulty.toLowerCase()}`;
          submissionResultEl.textContent = currentContext.result;
          pageStatusEl.textContent = 'Last Captured Attempt';
        } else {
          problemTitleEl.textContent = 'Binary Search (Demo)';
          pageStatusEl.textContent = 'Ready for submission';
        }
      });
    }
  } catch (err) {
    console.warn('[CodeMemory Popup] Could not query active tab:', err);
  }

  // 4. Handle Mistake Analysis Submission
  mistakeForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const selectedRadio = document.querySelector('input[name="reasonCategory"]:checked');
    const selectedReason = selectedRadio ? selectedRadio.value : 'Boundary / index issue';
    const explanationText = explanationInput.value.trim();

    const fullExplanation = `${selectedReason}: ${explanationText}`;

    btnAnalyze.disabled = true;
    btnAnalyze.textContent = 'Analyzing with AI Second Brain...';

    const payload = {
      problemTitle: currentContext.problemTitle,
      problemUrl: currentContext.url,
      difficulty: currentContext.difficulty,
      topics: ['Algorithms'],
      code: currentContext.code,
      language: 'Java',
      result: currentContext.result,
      userExplanation: fullExplanation
    };

    const res = await ExtensionApi.submitAttempt(payload);

    btnAnalyze.disabled = false;
    btnAnalyze.textContent = '⚡ Analyze & Remember Mistake';

    aiVerdictBox.classList.remove('hidden');

    if (res.success && res.data) {
      const mistake = res.data.mistake || {};
      verdictCategoryEl.textContent = mistake.category || 'BOUNDARY_ERROR';
      verdictConfidenceEl.textContent = `${Math.round((mistake.confidence || 0.92) * 100)}% Confidence`;
      verdictFeedbackEl.textContent = mistake.aiExplanation || 
        'Pattern matched with previous boundary management errors across binary-search topics.';
    } else {
      // Simulated response
      const sim = res.simulated;
      verdictCategoryEl.textContent = sim.mistakeCategory;
      verdictConfidenceEl.textContent = `${Math.round(sim.confidence * 100)}% Confidence`;
      verdictFeedbackEl.textContent = 'Recurring pattern confirmed: Boundary adjustment failure across multiple attempts.';
    }
  });

  // 5. Open React Dashboard in new tab
  btnOpenDashboard.addEventListener('click', () => {
    chrome.tabs.create({ url: 'http://localhost:5173' });
  });
});
