/**
 * Error2Expert Background Service Worker (Manifest V3)
 * Coordinates extension events, badges, token auto-synchronization, and automatic attempt logging.
 */

import { ExtensionApi, DEFAULT_BACKEND_URL, DEFAULT_DASHBOARD_URL } from './services/api.js';

function enforceCloudSettings() {
  chrome.storage.local.get(['settings'], (res) => {
    const existing = res.settings || {};
    const backend = (!existing.backendUrl || existing.backendUrl.includes('localhost') || existing.backendUrl.includes('127.0.0.1'))
      ? DEFAULT_BACKEND_URL
      : existing.backendUrl;
    const dashboard = (!existing.dashboardUrl || existing.dashboardUrl.includes('localhost') || existing.dashboardUrl.includes('127.0.0.1'))
      ? DEFAULT_DASHBOARD_URL
      : existing.dashboardUrl;

    chrome.storage.local.set({
      settings: {
        ...existing,
        backendUrl: backend,
        dashboardUrl: dashboard
      }
    }, () => {
      console.log('[Error2Expert Background] Endpoints configured:', backend, dashboard);
    });
  });
}

chrome.runtime.onInstalled.addListener(() => {
  console.log('[Error2Expert] Extension installed / updated.');
  enforceCloudSettings();
  syncTokenFromOpenDashboardTabs();
});

chrome.runtime.onStartup.addListener(() => {
  enforceCloudSettings();
  syncTokenFromOpenDashboardTabs();
});

// Periodic token sync or on wake
async function syncTokenFromOpenDashboardTabs() {
  try {
    const tabs = await chrome.tabs.query({
      url: [
        'https://error2expert-frontend.onrender.com/*',
        'http://localhost:5173/*',
        'http://localhost:3000/*'
      ]
    });

    if (tabs && tabs.length > 0) {
      for (const tab of tabs) {
        if (!tab.id) continue;
        chrome.scripting.executeScript({
          target: { tabId: tab.id },
          func: () => ({
            token: localStorage.getItem('codememory_token'),
            user: localStorage.getItem('codememory_user')
          })
        }, (results) => {
          if (results && results[0] && results[0].result && results[0].result.token) {
            const { token, user } = results[0].result;
            let parsedUser = null;
            try { parsedUser = JSON.parse(user); } catch (e) {}

            chrome.storage.local.get(['settings'], (storageRes) => {
              const current = storageRes.settings || {};
              chrome.storage.local.set({
                settings: {
                  ...current,
                  authToken: token,
                  authUser: parsedUser
                }
              });
              console.log('[Error2Expert Background] Auto-synced token from dashboard tab.');
            });
          }
        });
      }
    }
  } catch (err) {
    console.debug('[Error2Expert Background] Dashboard tab query skipped:', err);
  }
}

// Handle incoming messages from content scripts or popup
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.action === 'SYNC_AUTH_TOKEN') {
    const { token, user } = message;
    chrome.storage.local.get(['settings'], (res) => {
      const current = res.settings || {};
      chrome.storage.local.set({
        settings: {
          ...current,
          authToken: token,
          authUser: user || current.authUser
        }
      }, () => {
        console.log('[Error2Expert Background] Auth token updated via bridge.');
        sendResponse({ success: true });
      });
    });
    return true;
  }

  if (message.action === 'NEW_SUBMISSION_DETECTED') {
    const submission = message.data;
    console.log('[Error2Expert Background] Submission detected from LeetCode:', submission);

    // Prepare payload for backend
    const payload = {
      problemTitle: submission.problemTitle,
      problemUrl: submission.problemUrl,
      difficulty: submission.difficulty,
      topics: submission.topics || ['Algorithms'],
      code: submission.code,
      language: submission.language || 'Java',
      result: submission.result,
      userExplanation: submission.result === 'ACCEPTED' 
        ? null 
        : (submission.userExplanation || `Attempt result: ${submission.result}`)
    };

    // Auto-save attempt to Spring Boot backend in background
    ExtensionApi.submitAttempt(payload).then((res) => {
      if (res.success && res.data) {
        console.log('[Error2Expert Background] Successfully logged attempt to Second Brain:', res.data);
        const data = res.data;
        chrome.storage.local.set({
          lastSubmission: {
            ...submission,
            cloudSaved: true,
            submissionId: data.id,
            mistake: data.mistake || null
          }
        });

        if (submission.result === 'ACCEPTED') {
          chrome.action.setBadgeText({ text: '✓' });
          chrome.action.setBadgeBackgroundColor({ color: '#10b981' });
          setTimeout(() => chrome.action.setBadgeText({ text: '' }), 5000);
        } else {
          chrome.action.setBadgeText({ text: '!' });
          chrome.action.setBadgeBackgroundColor({ color: '#f43f5e' });
        }
        sendResponse({ success: true, cloudSaved: true, data: res.data });
      } else {
        console.warn('[Error2Expert Background] Attempt could not be auto-saved to backend:', res.message || res.error);
        chrome.storage.local.set({
          lastSubmission: {
            ...submission,
            cloudSaved: false,
            cloudError: res.message || res.error
          }
        });

        chrome.action.setBadgeText({ text: '!' });
        chrome.action.setBadgeBackgroundColor({ color: '#f59e0b' });
        sendResponse({ success: false, cloudSaved: false, error: res.message });
      }
    }).catch((err) => {
      console.error('[Error2Expert Background] Exception logging attempt:', err);
      sendResponse({ success: false, error: err.message });
    });

    return true; // Keep message channel open for async response
  }

  if (message.action === 'CLEAR_BADGE') {
    chrome.action.setBadgeText({ text: '' });
    sendResponse({ cleared: true });
    return false;
  }

  if (message.action === 'FORCE_TOKEN_SYNC') {
    syncTokenFromOpenDashboardTabs().then(() => sendResponse({ triggered: true }));
    return true;
  }

  return true;
});
