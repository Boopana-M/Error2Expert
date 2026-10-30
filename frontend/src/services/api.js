/**
 * Error2Expert API Service
 * Handles authenticated communication with the Spring Boot backend.
 * Enforces multi-user isolation by resolving data strictly for the authenticated JWT user.
 * ZERO fake demo fallbacks; real database responses only.
 */

const API_BASE_URL = import.meta.env.VITE_API_URL || 
  (typeof window !== 'undefined' && window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1'
    ? `${window.location.origin}/api`
    : 'http://localhost:8080/api');

const ML_API_BASE_URL = import.meta.env.VITE_ML_API_URL || 'http://localhost:8000';

function getAuthHeaders() {
  const token = localStorage.getItem('codememory_token');
  const headers = { 
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

export const api = {
  // --- Auth APIs ---
  async login(email, password) {
    const res = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: email.trim(), password }),
    });
    const json = await res.json();
    if (!res.ok || !json.success) {
      throw new Error(json.message || 'Login failed. Please check your credentials.');
    }
    if (json.data?.token) {
      localStorage.setItem('codememory_token', json.data.token);
      localStorage.setItem('codememory_user', JSON.stringify(json.data));
    }
    return json.data;
  },

  async register(name, email, password) {
    const res = await fetch(`${API_BASE_URL}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: name.trim(), email: email.trim(), password }),
    });
    const json = await res.json();
    if (!res.ok || !json.success) {
      throw new Error(json.message || 'Registration failed. Email may already be in use.');
    }
    if (json.data?.token) {
      localStorage.setItem('codememory_token', json.data.token);
      localStorage.setItem('codememory_user', JSON.stringify(json.data));
    }
    return json.data;
  },

  logout() {
    localStorage.removeItem('codememory_token');
    localStorage.removeItem('codememory_user');
  },

  getCurrentUser() {
    try {
      const stored = localStorage.getItem('codememory_user');
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  },

  isAuthenticated() {
    return Boolean(localStorage.getItem('codememory_token'));
  },

  // --- Dashboard APIs (Scoped strictly to Authenticated JWT) ---
  async getDashboardSummary() {
    const res = await fetch(`${API_BASE_URL}/dashboard/summary`, {
      headers: getAuthHeaders(),
    });
    if (res.status === 401) {
      this.logout();
      throw new Error('UNAUTHORIZED');
    }
    if (!res.ok) {
      throw new Error(`Failed to load summary (HTTP ${res.status})`);
    }
    return await res.json();
  },

  async getMistakeProfile() {
    const res = await fetch(`${API_BASE_URL}/dashboard/profile`, {
      headers: getAuthHeaders(),
    });
    if (res.status === 401) {
      this.logout();
      throw new Error('UNAUTHORIZED');
    }
    if (!res.ok) {
      throw new Error(`Failed to load mistake profile (HTTP ${res.status})`);
    }
    return await res.json();
  },

  async getWeaknesses() {
    const res = await fetch(`${API_BASE_URL}/weaknesses`, {
      headers: getAuthHeaders(),
    });
    if (res.status === 401) {
      this.logout();
      throw new Error('UNAUTHORIZED');
    }
    if (!res.ok) {
      throw new Error(`Failed to load weaknesses (HTTP ${res.status})`);
    }
    return await res.json();
  },

  async getTimeline() {
    const res = await fetch(`${API_BASE_URL}/dashboard/trends`, {
      headers: getAuthHeaders(),
    });
    if (res.status === 401) {
      this.logout();
      throw new Error('UNAUTHORIZED');
    }
    if (!res.ok) {
      throw new Error(`Failed to load timeline (HTTP ${res.status})`);
    }
    return await res.json();
  },

  async getTodaysFocus() {
    const res = await fetch(`${API_BASE_URL}/recommendations/today`, {
      headers: getAuthHeaders(),
    });
    if (res.status === 401) {
      this.logout();
      throw new Error('UNAUTHORIZED');
    }
    if (!res.ok) {
      throw new Error(`Failed to load recommendations (HTTP ${res.status})`);
    }
    return await res.json();
  },

  async submitAttempt(payload) {
    const res = await fetch(`${API_BASE_URL}/submissions`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(payload),
    });
    const json = await res.json();
    if (!res.ok || !json.success) {
      throw new Error(json.message || `Submission failed with status: ${res.status}`);
    }
    return json.data;
  },

  // --- ML Advanced Analysis APIs ---
  async analyzeDiff(wrongCode, acceptedCode, problemTitle) {
    const res = await fetch(`${ML_API_BASE_URL}/analyze-diff`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ wrongCode, acceptedCode, problemTitle }),
    });
    return await res.json();
  },

  async analyzeAst(code, language = 'java', problemTitle) {
    const res = await fetch(`${ML_API_BASE_URL}/analyze-ast`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code, language, problemTitle }),
    });
    return await res.json();
  }
};
