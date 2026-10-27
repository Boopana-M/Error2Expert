/**
 * CodeMemory API Service
 * Handles communication with the Spring Boot backend (http://localhost:8080/api)
 * Provides automatic fallback to initial seed dataset when backend is booting or hermetic.
 */

const API_BASE_URL = import.meta.env.VITE_API_URL || 
  (typeof window !== 'undefined' && window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1'
    ? `${window.location.origin}/api`
    : 'http://localhost:8080/api');

// Canonical fallback data adhering directly to database/seed.sql and CodeMemory_Project_Plan.md
const FALLBACK_DASHBOARD_DATA = {
  summary: {
    problemsAttempted: 18,
    problemsAccepted: 11,
    totalMistakes: 12,
    recurringMistakes: 5,
    strongestConcept: "Dynamic Programming",
    weakestConcept: "Boundary Management",
    currentMastery: 74,
  },
  mistakeProfile: [
    { category: "BOUNDARY_ERROR", label: "Boundary Management", count: 5, percentage: 38, color: "#f43f5e" },
    { category: "TIME_LIMIT_EXCEEDED", label: "Time Complexity (TLE)", count: 3, percentage: 23, color: "#f59e0b" },
    { category: "GRAPH_VISITED_ERROR", label: "Graph Visited State", count: 2, percentage: 15, color: "#a855f7" },
    { category: "EDGE_CASE_HANDLING", label: "Edge Case Omission", count: 2, percentage: 15, color: "#06b6d4" },
    { category: "OFF_BY_ONE", label: "Off By One Indexing", count: 1, percentage: 9, color: "#818cf8" },
  ],
  weaknesses: [
    {
      id: 1,
      concept: "Boundary Management",
      mistakeCategory: "BOUNDARY_ERROR",
      severity: "HIGH",
      frequency: 5,
      confidence: 0.94,
      masteryScore: 48,
      status: "RECURRING_ACROSS_PROBLEMS",
      firstObserved: "2026-09-15",
      lastObserved: "2026-10-14",
      description: "Repeated failure to handle upper/lower search bounds correctly in Binary Search & Two Pointers (high = mid vs mid - 1).",
      affectedProblems: ["Binary Search", "Find First and Last Position", "Search Insert Position"]
    },
    {
      id: 2,
      concept: "Time Complexity Misconception",
      mistakeCategory: "TIME_LIMIT_EXCEEDED",
      severity: "MEDIUM",
      frequency: 3,
      confidence: 0.88,
      masteryScore: 65,
      status: "RECURRING",
      firstObserved: "2026-09-22",
      lastObserved: "2026-10-10",
      description: "Defaulting to O(N^2) nested scan instead of O(N) hash-map or two-pointer two-pass optimal strategy.",
      affectedProblems: ["Two Sum", "3Sum", "Subarray Sum Equals K"]
    },
    {
      id: 3,
      concept: "Graph Visited State",
      mistakeCategory: "GRAPH_VISITED_ERROR",
      severity: "MEDIUM",
      frequency: 2,
      confidence: 0.82,
      masteryScore: 70,
      status: "EMERGING_PATTERN",
      firstObserved: "2026-10-01",
      lastObserved: "2026-10-08",
      description: "Pushing adjacent nodes to BFS queue before marking them visited, creating redundant cycle loops.",
      affectedProblems: ["Number of Islands", "Clone Graph"]
    },
    {
      id: 4,
      concept: "DP Base Cases",
      mistakeCategory: "DP_BASE_CASE_ERROR",
      severity: "IMPROVING",
      frequency: 1,
      confidence: 0.72,
      masteryScore: 88,
      status: "IMPROVING",
      firstObserved: "2026-09-02",
      lastObserved: "2026-09-28",
      description: "Sub-problem base initialization for length 0 strings. Successfully corrected in last 3 attempts.",
      affectedProblems: ["Longest Palindromic Substring", "Coin Change"]
    }
  ],
  timeline: [
    { period: "Aug 2026", boundary: 7, complexity: 4, edgeCase: 3, total: 14 },
    { period: "Sep 2026", boundary: 5, complexity: 3, edgeCase: 2, total: 10 },
    { period: "Oct 2026", boundary: 2, complexity: 1, edgeCase: 1, total: 4 },
  ],
  todaysFocus: {
    weakness: "Boundary Management",
    why: "You made 5 boundary-related mistakes across binary-search and two-pointer problems. Your weakness is boundary update logic (mid ± 1), not binary search itself.",
    recommendedProblems: [
      {
        id: "704",
        title: "Binary Search",
        difficulty: "EASY",
        topics: ["Binary Search", "Array"],
        url: "https://leetcode.com/problems/binary-search/",
        reason: "Practice exact loop invariant termination (left <= right vs left < right)."
      },
      {
        id: "35",
        title: "Search Insert Position",
        difficulty: "EASY",
        topics: ["Binary Search", "Array"],
        url: "https://leetcode.com/problems/search-insert-position/",
        reason: "Reinforce returning left pointer upon target absence without boundary overshoot."
      },
      {
        id: "153",
        title: "Find Minimum in Rotated Sorted Array",
        difficulty: "MEDIUM",
        topics: ["Binary Search", "Array"],
        url: "https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/",
        reason: "Sharpen unsymmetric boundary convergence (right = mid vs right = mid - 1)."
      },
      {
        id: "33",
        title: "Search in Rotated Sorted Array",
        difficulty: "MEDIUM",
        topics: ["Binary Search", "Array"],
        url: "https://leetcode.com/problems/search-in-rotated-sorted-array/",
        reason: "Multi-condition boundary partitioning under shifted pivots."
      }
    ]
  }
};

const ML_API_BASE_URL = import.meta.env.VITE_ML_API_URL || 'http://localhost:8000';

function getAuthHeaders() {
  const token = localStorage.getItem('codememory_token');
  const headers = { 'Content-Type': 'application/json' };
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
      body: JSON.stringify({ email, password }),
    });
    const json = await res.json();
    if (json.success && json.data?.token) {
      localStorage.setItem('codememory_token', json.data.token);
      localStorage.setItem('codememory_user', JSON.stringify(json.data));
    }
    return json;
  },

  async register(name, email, password) {
    const res = await fetch(`${API_BASE_URL}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, email, password }),
    });
    const json = await res.json();
    if (json.success && json.data?.token) {
      localStorage.setItem('codememory_token', json.data.token);
      localStorage.setItem('codememory_user', JSON.stringify(json.data));
    }
    return json;
  },

  logout() {
    localStorage.removeItem('codememory_token');
    localStorage.removeItem('codememory_user');
  },

  getCurrentUser() {
    try {
      return JSON.parse(localStorage.getItem('codememory_user')) || { name: 'Boopana (Demo)', email: 'boopana@example.com' };
    } catch {
      return { name: 'Boopana (Demo)', email: 'boopana@example.com' };
    }
  },

  // --- Dashboard APIs ---
  async getDashboardSummary() {
    try {
      const res = await fetch(`${API_BASE_URL}/dashboard/summary`, {
        headers: getAuthHeaders(),
      });
      if (!res.ok) throw new Error('Network error');
      return await res.json();
    } catch {
      return FALLBACK_DASHBOARD_DATA.summary;
    }
  },

  async getMistakeProfile() {
    try {
      const res = await fetch(`${API_BASE_URL}/dashboard/profile`, {
        headers: getAuthHeaders(),
      });
      if (!res.ok) throw new Error('Network error');
      return await res.json();
    } catch {
      return FALLBACK_DASHBOARD_DATA.mistakeProfile;
    }
  },

  async getWeaknesses() {
    try {
      const res = await fetch(`${API_BASE_URL}/weaknesses`, {
        headers: getAuthHeaders(),
      });
      if (!res.ok) throw new Error('Network error');
      return await res.json();
    } catch {
      return FALLBACK_DASHBOARD_DATA.weaknesses;
    }
  },

  async getTimeline() {
    try {
      const res = await fetch(`${API_BASE_URL}/dashboard/trends`, {
        headers: getAuthHeaders(),
      });
      if (!res.ok) throw new Error('Network error');
      return await res.json();
    } catch {
      return FALLBACK_DASHBOARD_DATA.timeline;
    }
  },

  async getTodaysFocus() {
    try {
      const res = await fetch(`${API_BASE_URL}/recommendations/today`, {
        headers: getAuthHeaders(),
      });
      if (!res.ok) throw new Error('Network error');
      return await res.json();
    } catch {
      return FALLBACK_DASHBOARD_DATA.todaysFocus;
    }
  },

  async submitAttempt(payload) {
    const res = await fetch(`${API_BASE_URL}/submissions`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      throw new Error(`Submission failed with status: ${res.status}`);
    }
    return await res.json();
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
