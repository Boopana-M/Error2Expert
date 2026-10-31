/**
 * Error2Expert Extension API Client
 * Sends structured attempt and mistake reports to Spring Boot backend.
 * Dynamically resolves target backend URL from Chrome local storage.
 */

export const DEFAULT_BACKEND_URL = 'http://localhost:8080/api';
export const DEFAULT_DASHBOARD_URL = 'http://localhost:5173';

export async function getBackendUrl() {
  try {
    if (typeof chrome !== 'undefined' && chrome.storage && chrome.storage.local) {
      const res = await chrome.storage.local.get(['settings']);
      const url = res.settings?.backendUrl?.trim();
      if (url) {
        return url.replace(/\/+$/, '');
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
      const url = res.settings?.dashboardUrl?.trim();
      if (url) {
        return url.replace(/\/+$/, '');
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

export async function getAuthUser() {
  try {
    if (typeof chrome !== 'undefined' && chrome.storage && chrome.storage.local) {
      const res = await chrome.storage.local.get(['settings']);
      return res.settings?.authUser || null;
    }
  } catch (e) {
    console.warn('Error reading authUser from storage:', e);
  }
  return null;
}

export const ExtensionApi = {
  /**
   * Posts submission and mistake explanation to Spring Boot backend.
   * Requires an authenticated JWT user token.
   */
  async submitAttempt(payload) {
    const baseUrl = await getBackendUrl();
    const token = await getAuthToken();

    if (!token) {
      console.warn('[Error2Expert API] No authentication token found.');
      return {
        success: false,
        error: 'NO_AUTH_TOKEN',
        message: 'No login token found. Please open or log in to the Error2Expert dashboard to sync your account.'
      };
    }

    try {
      const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Authorization': `Bearer ${token}`
      };

      const response = await fetch(`${baseUrl}/submissions`, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        const errorBody = await response.text().catch(() => '');
        console.error(`[Error2Expert API] HTTP ${response.status}:`, errorBody);
        return {
          success: false,
          error: `HTTP_${response.status}`,
          status: response.status,
          message: response.status === 401 || response.status === 403
            ? 'Authentication expired or invalid. Please open the dashboard to re-sync.'
            : `Server returned error (${response.status}).`
        };
      }

      const data = await response.json();
      return { success: true, data: data.data || data };
    } catch (error) {
      console.warn(`[Error2Expert API] Backend at ${baseUrl} unreachable:`, error);
      return {
        success: false,
        error: error.name === 'AbortError' ? 'TIMEOUT' : 'NETWORK_ERROR',
        message: `Could not reach backend (${error.message}). Check connection.`
      };
    }
  },

  /**
   * Health check for Spring Boot API with generous timeout for server wake-up.
   */
  async checkHealth(customUrl = null) {
    const baseUrl = customUrl ? customUrl.replace(/\/+$/, '') : await getBackendUrl();
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 12000);
      const res = await fetch(`${baseUrl}/health`, { 
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
