import { ExtensionApi, getBackendUrl, getDashboardUrl } from '../services/api.js';

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

  // Settings Elements
  const btnToggleSettings = document.getElementById('btn-toggle-settings');
  const settingsPanel = document.getElementById('settings-panel');
  const inputBackendUrl = document.getElementById('input-backend-url');
  const inputDashboardUrl = document.getElementById('input-dashboard-url');
  const inputAuthToken = document.getElementById('input-auth-token');
  const btnSaveSettings = document.getElementById('btn-save-settings');
  const settingsStatusMsg = document.getElementById('settings-status-msg');

  let currentContext = {
    problemTitle: 'Binary Search',
    difficulty: 'EASY',
    result: 'WRONG_ANSWER',
    code: '// Submitted binary search attempt',
    url: 'https://leetcode.com/problems/binary-search/'
  };

  // 1. Clear alert badge
  chrome.runtime?.sendMessage?.({ action: 'CLEAR_BADGE' });

  // 2. Load stored settings and initialize health check
  const currentBackendUrl = await getBackendUrl();
  const currentDashboardUrl = await getDashboardUrl();
  if (inputBackendUrl) inputBackendUrl.value = currentBackendUrl;
  if (inputDashboardUrl) inputDashboardUrl.value = currentDashboardUrl;
  chrome.storage?.local?.get(['settings'], (res) => {
    if (res.settings && res.settings.authToken && inputAuthToken) {
      inputAuthToken.value = res.settings.authToken;
    }
  });

  // Auto-sync login token from open dashboard tab if present
  try {
    const dashTabs = await chrome.tabs?.query?.({ url: ['*://error2expert-frontend.onrender.com/*', 'http://localhost:5173/*', 'http://localhost:3000/*'] });
    if (dashTabs && dashTabs.length > 0 && dashTabs[0].id) {
      chrome.scripting?.executeScript?.({
        target: { tabId: dashTabs[0].id },
        func: () => localStorage.getItem('codememory_token')
      }, (results) => {
        if (results && results[0] && results[0].result) {
          const autoToken = results[0].result;
          chrome.storage?.local?.get(['settings'], (res) => {
            const current = res.settings || {};
            if (!current.authToken || current.authToken !== autoToken) {
              chrome.storage?.local?.set({
                settings: { ...current, authToken: autoToken }
              });
              if (inputAuthToken) inputAuthToken.value = autoToken;
            }
          });
        }
      });
    }
  } catch (e) {
    console.debug('Dashboard token auto-sync skipped:', e);
  }

  const updateHealthIndicator = async (url) => {
    const isOnline = await ExtensionApi.checkHealth(url);
    if (isOnline) {
      backendStatusEl.classList.add('online');
      statusTextEl.textContent = 'API Connected';
    } else {
      backendStatusEl.classList.remove('online');
      statusTextEl.textContent = 'Offline (Mock)';
    }
    return isOnline;
  };

  updateHealthIndicator(currentBackendUrl);

  // Settings Panel Toggle
  btnToggleSettings?.addEventListener('click', () => {
    settingsPanel.classList.toggle('hidden');
  });

  // Save Settings & Re-test Connection
  btnSaveSettings?.addEventListener('click', async () => {
    const newBackend = (inputBackendUrl.value || 'http://localhost:8080/api').trim().replace(/\/+$/, '');
    const newDashboard = (inputDashboardUrl.value || 'http://localhost:5173').trim().replace(/\/+$/, '');
    const newAuthToken = (inputAuthToken?.value || '').trim();

    btnSaveSettings.disabled = true;
    btnSaveSettings.textContent = 'Testing...';
    settingsStatusMsg.textContent = '';

    await chrome.storage?.local?.set({
      settings: {
        backendUrl: newBackend,
        dashboardUrl: newDashboard,
        authToken: newAuthToken
      }
    });

    const isOnline = await updateHealthIndicator(newBackend);
    btnSaveSettings.disabled = false;
    btnSaveSettings.textContent = 'Save & Test Connection';

    if (isOnline) {
      settingsStatusMsg.textContent = 'Connected ✓';
      settingsStatusMsg.style.color = '#10b981';
      setTimeout(() => settingsPanel.classList.add('hidden'), 1200);
    } else {
      settingsStatusMsg.textContent = 'Saved (Unreachable)';
      settingsStatusMsg.style.color = '#f59e0b';
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
    console.warn('[Error2Expert Popup] Could not query active tab:', err);
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
      const data = res.data.data || res.data;
      const mistake = data.mistake || data;
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
  btnOpenDashboard.addEventListener('click', async () => {
    const dashboardUrl = await getDashboardUrl();
    chrome.tabs.create({ url: dashboardUrl });
  });
});

