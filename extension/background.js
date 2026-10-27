/**
 * CodeMemory Background Service Worker (Manifest V3)
 * Coordinates extension events, badges, and storage state.
 */

import { ExtensionApi } from './services/api.js';

chrome.runtime.onInstalled.addListener(() => {
  console.log('[Error2Expert] Extension successfully installed.');
  chrome.storage.local.get(['settings'], (res) => {
    if (!res.settings) {
      chrome.storage.local.set({
        settings: {
          backendUrl: 'http://localhost:8080/api',
          dashboardUrl: 'http://localhost:5173'
        }
      });
    }
  });
});

// Handle incoming messages from content scripts or popup
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.action === 'NEW_SUBMISSION_DETECTED') {
    const submission = message.data;
    console.log('[CodeMemory Background] Recorded attempt:', submission);

    if (submission.result !== 'ACCEPTED') {
      // Set indicator badge on extension icon to remind user to explain mistake
      chrome.action.setBadgeText({ text: '!' });
      chrome.action.setBadgeBackgroundColor({ color: '#f43f5e' });
    } else {
      chrome.action.setBadgeText({ text: '✓' });
      chrome.action.setBadgeBackgroundColor({ color: '#10b981' });
      setTimeout(() => chrome.action.setBadgeText({ text: '' }), 4000);
    }

    sendResponse({ received: true });
  }

  if (message.action === 'CLEAR_BADGE') {
    chrome.action.setBadgeText({ text: '' });
    sendResponse({ cleared: true });
  }

  return true;
});
