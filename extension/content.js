/**
 * CodeMemory Content Script
 * Injected on LeetCode problem pages to observe submissions and bridge metadata to popup.
 */

(() => {
  console.log('[CodeMemory] Content script initialized on LeetCode.');

  // Listen for queries from popup.js
  chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
    if (request.action === 'GET_PROBLEM_CONTEXT') {
      const metadata = window.CodeMemoryParser.getProblemMetadata();
      const code = window.CodeMemoryParser.getSubmittedCode();
      const liveVerdict = window.CodeMemoryParser.detectLiveVerdict();
      sendResponse({ metadata, code, liveVerdict, success: true });
    }
    return true; // async response
  });

  // Watch for submit button clicks
  document.addEventListener('click', (event) => {
    const target = event.target.closest('button');
    if (!target) return;

    const btnText = (target.textContent || '').trim().toLowerCase();

    // Prevent false triggers on the "Submissions" tab or navigation headers
    if (btnText === 'submissions' || btnText.includes('all submissions') || target.closest('[data-cy="submissions-tab"]')) {
      return;
    }

    // Identify authentic code submit buttons
    const isSubmit = target.getAttribute('data-e2e-locator') === 'console-submit-button' ||
                     target.getAttribute('data-cy') === 'submit-code-btn' ||
                     btnText === 'submit' ||
                     btnText === 'submit code' ||
                     Boolean(target.querySelector('svg') && btnText === 'submit');

    if (isSubmit) {
      console.log('[CodeMemory] Code submission action detected. Monitoring for live verdict...');
      const metadata = window.CodeMemoryParser.getProblemMetadata();
      const code = window.CodeMemoryParser.getSubmittedCode();

      // Monitor DOM for submission result container
      observeSubmissionResult(metadata, code);
    }
  }, true);

  function observeSubmissionResult(metadata, code) {
    let attempts = 0;
    const maxAttempts = 40; // poll for up to 20 seconds (submissions can take a few seconds)

    // Clear any previous interval if user rapidly re-submitted
    if (window._codeMemoryPollInterval) {
      clearInterval(window._codeMemoryPollInterval);
    }

    window._codeMemoryPollInterval = setInterval(() => {
      attempts++;

      // Use targeted verdict detection
      const liveVerdict = window.CodeMemoryParser.detectLiveVerdict();

      if (liveVerdict) {
        clearInterval(window._codeMemoryPollInterval);
        window._codeMemoryPollInterval = null;

        console.log(`[CodeMemory] Detected submission verdict: ${liveVerdict}`);

        const submissionData = {
          problemTitle: metadata.title,
          problemUrl: metadata.url,
          difficulty: metadata.difficulty,
          topics: metadata.topics,
          code: code,
          result: liveVerdict,
          timestamp: new Date().toISOString()
        };

        // Cache in local storage for popup
        chrome.storage.local.set({ lastSubmission: submissionData }, () => {
          console.log('[CodeMemory] Cached last submission data in storage:', submissionData);
        });

        // Notify background service worker
        chrome.runtime.sendMessage({
          action: 'NEW_SUBMISSION_DETECTED',
          data: submissionData
        });

        // Display feedback banner
        showFloatingFeedbackReminder(metadata.title, liveVerdict);
      }

      if (attempts >= maxAttempts) {
        clearInterval(window._codeMemoryPollInterval);
        window._codeMemoryPollInterval = null;
      }
    }, 500);
  }

  function showFloatingFeedbackReminder(title, result) {
    // Avoid duplicate reminder banners
    const existing = document.getElementById('codememory-floating-banner');
    if (existing) existing.remove();

    const isAccepted = result === 'ACCEPTED';
    const banner = document.createElement('div');
    banner.id = 'codememory-floating-banner';
    banner.style.cssText = `
      position: fixed;
      bottom: 24px;
      right: 24px;
      z-index: 999999;
      background: #090d16;
      color: #f8fafc;
      border: 1px solid ${isAccepted ? 'rgba(16, 185, 129, 0.5)' : 'rgba(244, 63, 94, 0.5)'};
      box-shadow: 0 10px 30px rgba(0,0,0,0.8);
      border-radius: 12px;
      padding: 14px 18px;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      font-size: 13px;
      display: flex;
      align-items: center;
      gap: 12px;
      animation: fadeIn 0.3s ease;
    `;

    banner.innerHTML = `
      <div style="font-size: 22px;">${isAccepted ? '🎉' : '🧠'}</div>
      <div>
        <div style="font-weight: 700; color: ${isAccepted ? '#34d399' : '#fb7185'};">
          ${isAccepted ? 'Accepted Solution Logged!' : result.replace('_', ' ') + ' on ' + title}
        </div>
        <div style="color: #94a3b8; font-size: 11px;">
          ${isAccepted 
            ? 'Synced to Error2Expert — open mistakes marked resolved.' 
            : 'Attempt tracked! Click Error2Expert extension icon to add your reflection.'}
        </div>
      </div>
      <button id="cm-banner-close" style="background: none; border: none; color: #64748b; font-size: 16px; cursor: pointer; padding: 2px 6px;">✕</button>
    `;

    document.body.appendChild(banner);

    document.getElementById('cm-banner-close')?.addEventListener('click', () => {
      banner.remove();
    });

    // Auto dismiss after 7 seconds
    setTimeout(() => {
      if (banner.parentNode) banner.remove();
    }, 7000);
  }
})();
