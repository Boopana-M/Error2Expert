/**
 * Error2Expert Extension API Client
 * Sends structured attempt and mistake reports to Spring Boot backend.
 * Dynamically resolves target backend URL from Chrome local storage.
 */

const DEFAULT_BACKEND_URL = 'http://localhost:8080/api';
const DEFAULT_DASHBOARD_URL = 'http://localhost:5173';

export async function getBackendUrl() {
  try {
    if (typeof chrome !== 'undefined' && chrome.storage && chrome.storage.local) {
      const res = await chrome.storage.local.get(['settings']);
      if (res.settings && res.settings.backendUrl) {
        return res.settings.backendUrl.replace(/\/+$/, '');
      }
    }
  } catch (e) {
    console.warn('Error reading backendUrl from storage:', e);
  }
  return DEFAULT_BACKEND_URL;
}

export async function getDashboardUrl() {
  try {
    if (typeof chrome !== 'undefined' && chrome.storage && chrome.storage.local) {
      const res = await chrome.storage.local.get(['settings']);
      if (res.settings && res.settings.dashboardUrl) {
        return res.settings.dashboardUrl.replace(/\/+$/, '');
      }
    }
  } catch (e) {
    console.warn('Error reading dashboardUrl from storage:', e);
  }
  return DEFAULT_DASHBOARD_URL;
}

export async function getAuthToken() {
  try {
    if (typeof chrome !== 'undefined' && chrome.storage && chrome.storage.local) {
      const res = await chrome.storage.local.get(['settings']);
      if (res.settings && res.settings.authToken) {
        let token = res.settings.authToken.trim();
        if (token.startsWith('Bearer ')) token = token.substring(7).trim();
        return token;
      }
    }
  } catch (e) {
    console.warn('Error reading authToken from storage:', e);
  }
  return null;
}

export const ExtensionApi = {
  /**
   * Posts submission and mistake explanation to Spring Boot backend
   */
  async submitAttempt(payload) {
    const baseUrl = await getBackendUrl();
    const token = await getAuthToken();
    try {
      const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const response = await fetch(`${baseUrl}/submissions`, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      return { success: true, data };
    } catch (error) {
      console.warn(`[Error2Expert API] Backend at ${baseUrl} unreachable:`, error);
      // Return realistic mock response if backend is offline during browser testing
      return {
        success: false,
        error: error.message,
        simulated: {
          mistakeCategory: payload.userExplanation?.toLowerCase().includes('bound') 
            ? 'BOUNDARY_ERROR' 
            : 'LOGIC_ERROR',
          confidence: 0.89,
          recurring: true
        }
      };
    }
  },

  /**
   * Health check for Spring Boot API
   */
  async checkHealth(customUrl = null) {
    const baseUrl = customUrl ? customUrl.replace(/\/+$/, '') : await getBackendUrl();
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 4000);
      const res = await fetch(`${baseUrl}/submissions?limit=1`, { 
        method: 'GET',
        signal: controller.signal
      });
      clearTimeout(timeoutId);
      return res.ok;
    } catch {
      return false;
    }
  }
};

