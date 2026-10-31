import { ExtensionApi, getBackendUrl, getDashboardUrl, getAuthToken, getAuthUser } from '../services/api.js';

document.addEventListener('DOMContentLoaded', async () => {
  const problemTitleEl = document.getElementById('problem-title');
  const problemDifficultyEl = document.getElementById('problem-difficulty');
  const submissionResultEl = document.getElementById('submission-result');
  const cloudSyncStatusEl = document.getElementById('cloud-sync-status');
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
  const noticeBanner = document.getElementById('notice-banner');

  // Account Status Elements
  const accountStatusBar = document.getElementById('account-status-bar');
  const accountTextEl = document.getElementById('account-text');
  const btnSyncToken = document.getElementById('btn-sync-token');

  // Settings Elements
  const btnToggleSettings = document.getElementById('btn-toggle-settings');
  const settingsPanel = document.getElementById('settings-panel');
  const inputBackendUrl = document.getElementById('input-backend-url');
  const inputDashboardUrl = document.getElementById('input-dashboard-url');
  const inputAuthToken = document.getElementById('input-auth-token');
  const btnSaveSettings = document.getElementById('btn-save-settings');
  const settingsStatusMsg = document.getElementById('settings-status-msg');

  let currentContext = {
    problemTitle: '',
    difficulty: '',
    result: '',
    code: '',
    url: ''
  };

  // 1. Clear alert badge
  chrome.runtime?.sendMessage?.({ action: 'CLEAR_BADGE' });

  // 2. Load stored settings and initialize health check
  const currentBackendUrl = await getBackendUrl();
  const currentDashboardUrl = await getDashboardUrl();
  if (inputBackendUrl) inputBackendUrl.value = currentBackendUrl;
  if (inputDashboardUrl) inputDashboardUrl.value = currentDashboardUrl;

  const refreshAccountStatus = async () => {
    const token = await getAuthToken();
    const user = await getAuthUser();
    if (token) {
      if (accountStatusBar) {
        accountStatusBar.className = 'account-status-bar connected';
      }
      if (accountTextEl) {
        accountTextEl.textContent = user?.name ? `Signed in: ${user.name}` : 'Signed in (Token Active)';
      }
      if (btnSyncToken) {
        btnSyncToken.textContent = '✓ Synced';
      }
      if (inputAuthToken) inputAuthToken.value = token;
      hideNotice();
      return true;
    } else {
      if (accountStatusBar) {
        accountStatusBar.className = 'account-status-bar disconnected';
      }
      if (accountTextEl) {
        accountTextEl.textContent = 'Not connected to Dashboard';
      }
      if (btnSyncToken) {
        btnSyncToken.textContent = '⚡ One-Click Sync';
      }
      return false;
    }
  };

  const showNotice = (msg, isError = true) => {
    if (!noticeBanner) return;
    noticeBanner.textContent = msg;
    noticeBanner.className = isError ? 'status-banner-error' : 'status-banner-error cloud-save-tag';
    noticeBanner.classList.remove('hidden');
  };

  const hideNotice = () => {
    if (noticeBanner) noticeBanner.classList.add('hidden');
  };

  // Attempt auto-sync of token from any open dashboard tab
  const syncFromDashboardTabs = async (openTabIfMissing = false) => {
    btnSyncToken.textContent = 'Syncing...';
    try {
      const tabs = await chrome.tabs?.query?.({
        url: [
          'https://error2expert-frontend.onrender.com/*',
          'http://localhost:5173/*',
          'http://localhost:3000/*'
        ]
      });

      if (tabs && tabs.length > 0 && tabs[0].id) {
        chrome.scripting?.executeScript?.({
          target: { tabId: tabs[0].id },
          func: () => ({
            token: localStorage.getItem('codememory_token'),
            user: localStorage.getItem('codememory_user')
          })
        }, async (results) => {
          if (results && results[0] && results[0].result && results[0].result.token) {
            const { token, user } = results[0].result;
            let parsedUser = null;
            try { parsedUser = JSON.parse(user); } catch (e) {}

            chrome.storage?.local?.get(['settings'], async (res) => {
              const current = res.settings || {};
              await chrome.storage?.local?.set({
                settings: {
                  ...current,
                  authToken: token,
                  authUser: parsedUser
                }
              });
              await refreshAccountStatus();
            });
          } else {
            showNotice('Found dashboard tab, but no login session found. Please sign in.');
            btnSyncToken.textContent = 'Sign In';
          }
        });
      } else {
        if (openTabIfMissing) {
          const dashUrl = await getDashboardUrl();
          chrome.tabs.create({ url: dashUrl });
        } else {
          showNotice('Dashboard tab not found. Click "One-Click Sync" to open Dashboard and login.');
          btnSyncToken.textContent = 'Open Dashboard';
        }
      }
    } catch (e) {
      console.warn('Dashboard token auto-sync skipped:', e);
      btnSyncToken.textContent = 'Retry Sync';
    }
  };

  // Initial token check and sync
  const isAuth = await refreshAccountStatus();
  if (!isAuth) {
    await syncFromDashboardTabs(false);
  }

  // 1-Click Sync button handler
  btnSyncToken?.addEventListener('click', async () => {
    await syncFromDashboardTabs(true);
  });

  const updateHealthIndicator = async (url) => {
    if (statusTextEl) statusTextEl.textContent = 'Checking...';
    const isOnline = await ExtensionApi.checkHealth(url);
    if (isOnline) {
      backendStatusEl?.classList.add('online');
      if (statusTextEl) statusTextEl.textContent = 'API Connected';
    } else {
      backendStatusEl?.classList.remove('online');
      if (statusTextEl) statusTextEl.textContent = 'Offline (Unreachable)';
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
    const newBackend = (inputBackendUrl.value || currentBackendUrl).trim().replace(/\/+$/, '');
    const newDashboard = (inputDashboardUrl.value || currentDashboardUrl).trim().replace(/\/+$/, '');
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

    await refreshAccountStatus();
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

  const updateResultBadge = (result, cloudSaved = null, cloudError = null) => {
    if (!result) {
      if (submissionResultEl) submissionResultEl.style.display = 'none';
      if (cloudSyncStatusEl) cloudSyncStatusEl.style.display = 'none';
      return;
    }
    currentContext.result = result;
    if (submissionResultEl) {
      submissionResultEl.style.display = 'inline-block';
      submissionResultEl.textContent = result;
      if (result === 'ACCEPTED') {
        submissionResultEl.className = 'result-tag result-accepted';
      } else {
        submissionResultEl.className = 'result-tag result-failed';
      }
    }

    // Update Cloud Sync Status tag
    if (cloudSyncStatusEl) {
      cloudSyncStatusEl.style.display = 'inline-block';
      if (cloudSaved) {
        cloudSyncStatusEl.className = 'cloud-save-tag';
        cloudSyncStatusEl.textContent = '✓ Saved to Cloud';
      } else if (cloudError) {
        cloudSyncStatusEl.className = 'cloud-save-tag pending';
        cloudSyncStatusEl.textContent = '⚠️ Not Saved';
        cloudSyncStatusEl.title = cloudError;
      } else {
        cloudSyncStatusEl.className = 'cloud-save-tag pending';
        cloudSyncStatusEl.textContent = 'Syncing...';
      }
    }

    const formTitle = document.querySelector('.form-section-title');
    if (result === 'ACCEPTED') {
      if (formTitle) formTitle.textContent = '🎉 Problem Solved — Log Solution Reflection';
      if (btnAnalyze) {
        btnAnalyze.textContent = '✓ Record Solved Attempt & Boost Mastery';
        btnAnalyze.style.background = 'linear-gradient(135deg, #10b981 0%, #059669 100%)';
      }
    } else {
      if (formTitle) formTitle.textContent = 'Why do you think this failed?';
      if (btnAnalyze) {
        btnAnalyze.textContent = '⚡ Analyze & Remember Mistake';
        btnAnalyze.style.background = '';
      }
    }
  };

  // 3. Query active tab for live LeetCode data
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (tab && tab.url && tab.url.includes('leetcode.com/problems/')) {
      chrome.tabs.sendMessage(tab.id, { action: 'GET_PROBLEM_CONTEXT' }, (response) => {
        if (response && response.success) {
          const { metadata, code, liveVerdict } = response;
          currentContext.problemTitle = metadata.title;
          currentContext.difficulty = metadata.difficulty;
          currentContext.code = code;
          currentContext.url = metadata.url;

          problemTitleEl.textContent = metadata.title;
          if (problemDifficultyEl) {
            problemDifficultyEl.style.display = 'inline-block';
            problemDifficultyEl.textContent = metadata.difficulty.substring(0, 3);
            problemDifficultyEl.className = `badge badge-${metadata.difficulty.toLowerCase()}`;
          }
          pageStatusEl.textContent = 'Active LeetCode Session';

          // Check if cached submission exists
          chrome.storage?.local?.get(['lastSubmission'], (res) => {
            const cached = res.lastSubmission;
            if (cached && cached.problemTitle === metadata.title) {
              updateResultBadge(cached.result, cached.cloudSaved, cached.cloudError);
            } else if (liveVerdict) {
              updateResultBadge(liveVerdict, null, null);
            } else {
              updateResultBadge(null);
            }
          });
        }
      });
    } else {
      // Check last cached submission
      chrome.storage?.local?.get(['lastSubmission'], (res) => {
        if (res.lastSubmission && res.lastSubmission.problemTitle) {
          currentContext = { ...currentContext, ...res.lastSubmission };
          problemTitleEl.textContent = currentContext.problemTitle;
          if (problemDifficultyEl) {
            problemDifficultyEl.style.display = 'inline-block';
            problemDifficultyEl.textContent = (currentContext.difficulty || 'MED').substring(0, 3);
            problemDifficultyEl.className = `badge badge-${(currentContext.difficulty || 'medium').toLowerCase()}`;
          }
          updateResultBadge(
            currentContext.result || 'SUBMISSION',
            res.lastSubmission.cloudSaved,
            res.lastSubmission.cloudError
          );
          pageStatusEl.textContent = 'Last Captured Attempt';
        } else {
          problemTitleEl.textContent = 'No LeetCode Problem Active';
          if (problemDifficultyEl) problemDifficultyEl.style.display = 'none';
          if (submissionResultEl) submissionResultEl.style.display = 'none';
          if (cloudSyncStatusEl) cloudSyncStatusEl.style.display = 'none';
          pageStatusEl.textContent = 'Open leetcode.com/problems/* to capture';
          btnAnalyze.disabled = true;
          btnAnalyze.textContent = 'Navigate to a LeetCode problem';
        }
      });
    }
  } catch (err) {
    console.warn('[Error2Expert Popup] Could not query active tab:', err);
  }

  // 4. Handle Mistake Analysis Form Submission
  mistakeForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    hideNotice();

    // Ensure token is present before submitting
    const token = await getAuthToken();
    if (!token) {
      showNotice('Please sign in to the Error2Expert Dashboard first to sync your account.');
      await syncFromDashboardTabs(false);
      return;
    }

    const selectedRadio = document.querySelector('input[name="reasonCategory"]:checked');
    const selectedReason = selectedRadio ? selectedRadio.value : 'Boundary / index issue';
    const explanationText = (explanationInput.value || '').trim();

    const fullExplanation = explanationText 
      ? `${selectedReason}: ${explanationText}`
      : selectedReason;

    btnAnalyze.disabled = true;
    btnAnalyze.textContent = 'Analyzing with AI Second Brain...';

    const payload = {
      problemTitle: currentContext.problemTitle,
      problemUrl: currentContext.url,
      difficulty: currentContext.difficulty,
      topics: ['Algorithms'],
      code: currentContext.code,
      language: 'Java',
      result: currentContext.result || 'WRONG_ANSWER',
      userExplanation: fullExplanation
    };

    const res = await ExtensionApi.submitAttempt(payload);

    btnAnalyze.disabled = false;
    btnAnalyze.textContent = currentContext.result === 'ACCEPTED'
      ? '✓ Record Solved Attempt & Boost Mastery'
      : '⚡ Analyze & Remember Mistake';

    if (res.success && res.data) {
      const data = res.data;
      const mistake = data.mistake || data;

      // Update storage status
      chrome.storage?.local?.set({
        lastSubmission: {
          ...currentContext,
          cloudSaved: true,
          submissionId: data.id,
          mistake: mistake
        }
      });

      updateResultBadge(currentContext.result, true, null);

      aiVerdictBox.classList.remove('hidden');
      verdictCategoryEl.textContent = mistake.category || 'BOUNDARY_ERROR';
      verdictConfidenceEl.textContent = `${Math.round((mistake.confidence || 0.92) * 100)}% Match`;
      verdictFeedbackEl.textContent = mistake.aiExplanation || 
        'Pattern analyzed and longitudinal weakness index updated in your dashboard.';
    } else {
      showNotice(`Failed to save attempt: ${res.message || 'Server error'}. Please check your connection.`);
      updateResultBadge(currentContext.result, false, res.message || 'Server error');
    }
  });

  // 5. Open React Dashboard in new tab
  btnOpenDashboard.addEventListener('click', async () => {
    const dashboardUrl = await getDashboardUrl();
    chrome.tabs.create({ url: dashboardUrl });
  });
});
