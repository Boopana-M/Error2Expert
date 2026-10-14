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
      sendResponse({ metadata, code, success: true });
    }
    return true; // async response
  });

  // Watch for submit button clicks
  document.addEventListener('click', (event) => {
    const target = event.target.closest('button');
    if (!target) return;

    const btnText = (target.textContent || '').trim().toLowerCase();
    const isSubmit = btnText.includes('submit') || 
                     target.getAttribute('data-e2e-locator') === 'console-submit-button';

    if (isSubmit) {
      console.log('[CodeMemory] Submission action detected. Monitoring for verdict...');
      const metadata = window.CodeMemoryParser.getProblemMetadata();
      const code = window.CodeMemoryParser.getSubmittedCode();

      // Monitor DOM for submission result container
      observeSubmissionResult(metadata, code);
    }
  }, true);

  function observeSubmissionResult(metadata, code) {
    let attempts = 0;
    const maxAttempts = 30; // poll for up to 15 seconds

    const pollInterval = setInterval(() => {
      attempts++;
      // Search for result badges
      const resultEl = document.querySelector('[data-e2e-locator="submission-result"]') 
        || document.querySelector('span[class*="text-green-s"]') // Accepted
        || document.querySelector('span[class*="text-red-s"]')   // Wrong Answer
        || document.querySelector('span[class*="text-yellow-s"]'); // TLE

      if (resultEl && resultEl.textContent.trim()) {
        clearInterval(pollInterval);
        const rawResult = resultEl.textContent.trim();
        const normalized = window.CodeMemoryParser.normalizeResult(rawResult);

        console.log(`[CodeMemory] Detected submission verdict: ${normalized}`);

        const submissionData = {
          problemTitle: metadata.title,
          problemUrl: metadata.url,
          difficulty: metadata.difficulty,
          topics: metadata.topics,
          code: code,
          result: normalized,
          timestamp: new Date().toISOString()
        };

        // Cache in local storage for popup
        chrome.storage.local.set({ lastSubmission: submissionData }, () => {
          console.log('[CodeMemory] Cached last submission data in storage.');
        });

        // Notify background service worker
        chrome.runtime.sendMessage({
          action: 'NEW_SUBMISSION_DETECTED',
          data: submissionData
        });

        // If failed submission, display subtle floating reminder to explain mistake
        if (normalized !== 'ACCEPTED') {
          showFloatingFeedbackReminder(metadata.title, normalized);
        }
      }

      if (attempts >= maxAttempts) {
        clearInterval(pollInterval);
      }
    }, 500);
  }

  function showFloatingFeedbackReminder(title, result) {
    // Avoid duplicate reminder banners
    if (document.getElementById('codememory-floating-banner')) return;

    const banner = document.createElement('div');
    banner.id = 'codememory-floating-banner';
    banner.style.cssText = `
      position: fixed;
      bottom: 24px;
      right: 24px;
      z-index: 999999;
      background: #111827;
      color: #f8fafc;
      border: 1px solid rgba(99, 102, 241, 0.4);
      box-shadow: 0 10px 30px rgba(0,0,0,0.7);
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
      <div style="font-size: 20px;">🧠</div>
      <div>
        <div style="font-weight: 700; color: #fb7185;">${result.replace('_', ' ')} on ${title}</div>
        <div style="color: #94a3b8; font-size: 11px;">Click the CodeMemory extension icon to record why this failed!</div>
      </div>
      <button id="cm-banner-close" style="background: none; border: none; color: #64748b; font-size: 16px; cursor: pointer; padding: 2px 6px;">✕</button>
    `;

    document.body.appendChild(banner);

    document.getElementById('cm-banner-close').addEventListener('click', () => {
      banner.remove();
    });

    // Auto dismiss after 8 seconds
    setTimeout(() => {
      if (banner.parentNode) banner.remove();
    }, 8000);
  }
})();
