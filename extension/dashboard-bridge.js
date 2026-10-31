/**
 * Error2Expert Dashboard Bridge Content Script
 * Automatically syncs the authenticated user's JWT token and profile
 * between the web dashboard and the Chrome extension without requiring manual copy-paste.
 */

(() => {
  console.log('[Error2Expert Bridge] Dashboard bridge script active.');

  // Notify the React dashboard that the extension is active and installed
  document.documentElement.setAttribute('data-error2expert-extension', '1.0.0');
  window.postMessage({ type: 'ERROR2EXPERT_EXTENSION_INSTALLED', version: '1.0.0' }, '*');

  function syncTokenToExtension() {
    try {
      const token = localStorage.getItem('codememory_token');
      const userRaw = localStorage.getItem('codememory_user');
      let user = null;
      if (userRaw) {
        try { user = JSON.parse(userRaw); } catch (e) {}
      }

      if (token && token.trim()) {
        chrome.runtime.sendMessage({
          action: 'SYNC_AUTH_TOKEN',
          token: token.trim(),
          user: user
        }, (res) => {
          if (chrome.runtime.lastError) {
            console.debug('[Error2Expert Bridge] Extension runtime communication:', chrome.runtime.lastError.message);
          } else {
            console.log('[Error2Expert Bridge] Auth token successfully synchronized to extension.');
          }
        });
      }
    } catch (err) {
      console.debug('[Error2Expert Bridge] Error reading dashboard localStorage:', err);
    }
  }

  // 1. Initial sync on page load
  syncTokenToExtension();

  // 2. Poll for token on initial render (React state initializes shortly after bundle loads)
  let attempts = 0;
  const pollInterval = setInterval(() => {
    attempts++;
    syncTokenToExtension();
    if (attempts >= 8) clearInterval(pollInterval);
  }, 1000);

  // 3. Listen for storage changes (login/logout events in another tab)
  window.addEventListener('storage', (event) => {
    if (event.key === 'codememory_token' || event.key === 'codememory_user') {
      syncTokenToExtension();
    }
  });

  // 4. Listen for explicit window messages dispatched by React auth handler
  window.addEventListener('message', (event) => {
    if (event.data && (event.data.type === 'CODEMEMORY_AUTH_SYNC' || event.data.type === 'ERROR2EXPERT_AUTH_SYNC')) {
      const { token, user } = event.data;
      if (token) {
        chrome.runtime.sendMessage({
          action: 'SYNC_AUTH_TOKEN',
          token: token.trim(),
          user: user || null
        });
      }
    }
  });
})();
