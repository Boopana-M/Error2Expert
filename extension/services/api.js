/**
 * CodeMemory Extension API Client
 * Sends structured attempt and mistake reports to Spring Boot backend.
 */

const BACKEND_URL = 'http://localhost:8080/api';

export const ExtensionApi = {
  /**
   * Posts submission and mistake explanation to Spring Boot backend
   */
  async submitAttempt(payload) {
    try {
      const response = await fetch(`${BACKEND_URL}/submissions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      return { success: true, data };
    } catch (error) {
      console.warn('[CodeMemory API] Backend unreachable, using fallback simulation:', error);
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
  async checkHealth() {
    try {
      const res = await fetch(`${BACKEND_URL}/submissions?limit=1`, { method: 'GET' });
      return res.ok;
    } catch {
      return false;
    }
  }
};
