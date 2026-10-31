/**
 * CodeMemory LeetCode DOM Parser Utilities
 * Reliably extracts metadata, editor source code, and submission status from LeetCode sessions.
 */

window.CodeMemoryParser = {
  /**
   * Derives problem title, slug, and difficulty from the active LeetCode page.
   */
  getProblemMetadata() {
    const url = window.location.href;
    const match = url.match(/\/problems\/([^/]+)/);
    const slug = match ? match[1] : '';

    // Convert slug (e.g. 'binary-search') to Title Case as fallback
    const titleFromSlug = slug
      .split('-')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');

    // Attempt DOM title selectors
    let title = titleFromSlug;
    const titleElement = document.querySelector('[data-cy="question-title"]') 
      || document.querySelector('div[class*="text-title-large"]') 
      || document.querySelector('a[href*="/problems/"]');
    if (titleElement && titleElement.textContent.trim()) {
      title = titleElement.textContent.trim().replace(/^\d+\.\s*/, '');
    }

    // Attempt difficulty detection
    let difficulty = 'MEDIUM';
    const bodyText = document.body.innerText;
    const diffEl = document.querySelector('div[class*="text-difficulty-"]')
      || document.querySelector('span[class*="text-difficulty-"]')
      || document.querySelector('[class*="text-olive"]');

    if (diffEl) {
      const diffText = diffEl.textContent.trim().toUpperCase();
      if (['EASY', 'MEDIUM', 'HARD'].includes(diffText)) {
        difficulty = diffText;
      }
    } else {
      if (bodyText.includes('Easy')) difficulty = 'EASY';
      else if (bodyText.includes('Hard')) difficulty = 'HARD';
    }

    // Extract topics
    const topics = [];
    const topicElements = document.querySelectorAll('a[href*="/tag/"]');
    topicElements.forEach(el => {
      const name = el.textContent.trim();
      if (name && !topics.includes(name)) topics.push(name);
    });

    return {
      title: title || 'LeetCode Problem',
      slug,
      difficulty,
      topics: topics.length > 0 ? topics : ['Algorithms'],
      url
    };
  },

  /**
   * Extracts current code from Monaco editor or active textarea
   */
  getSubmittedCode() {
    // Strategy 1: Monaco editor lines
    const lines = document.querySelectorAll('.monaco-editor .view-lines .view-line');
    if (lines && lines.length > 0) {
      return Array.from(lines)
        .map(line => line.textContent || '')
        .join('\n');
    }

    // Strategy 2: textarea fallback
    const textarea = document.querySelector('textarea.inputarea') || document.querySelector('textarea');
    if (textarea && textarea.value) {
      return textarea.value;
    }

    return '// Code extracted from active session';
  },

  /**
   * Scans the active LeetCode page for a legitimate submission verdict.
   * Returns normalized enum ('ACCEPTED', 'WRONG_ANSWER', etc.) or null if no active submission verdict is visible.
   */
  detectLiveVerdict() {
    // 1. Primary LeetCode data-e2e locator
    const e2eEl = document.querySelector('[data-e2e-locator="submission-result"]');
    if (e2eEl && e2eEl.textContent.trim()) {
      const normalized = this.normalizeResult(e2eEl.textContent.trim());
      if (normalized) return normalized;
    }

    // 2. Targeted search inside verdict headers and tab labels
    const candidateSelectors = [
      'div[data-e2e-locator="submission-result"]',
      'span[data-e2e-locator="submission-result"]',
      '[class*="result-status"]',
      '[class*="status-column"]',
      'div[class*="text-red-"]',
      'div[class*="text-green-"]',
      'div[class*="text-yellow-"]',
      'span[class*="text-red-"]',
      'span[class*="text-green-"]',
      'span[class*="text-yellow-"]',
      'div[class*="text-sd-"]',
      'span[class*="text-sd-"]'
    ];

    for (const sel of candidateSelectors) {
      const elements = document.querySelectorAll(sel);
      for (const el of elements) {
        const txt = (el.textContent || '').trim();
        const upper = txt.toUpperCase();

        // Must strictly match verdict text, NOT testcase counts or tab titles
        if (upper === 'ACCEPTED' || upper.startsWith('ACCEPTED ')) return 'ACCEPTED';
        if (upper === 'WRONG ANSWER' || upper.startsWith('WRONG ANSWER ')) return 'WRONG_ANSWER';
        if (upper === 'TIME LIMIT EXCEEDED' || upper.startsWith('TIME LIMIT EXCEEDED ') || upper.startsWith('TLE')) return 'TIME_LIMIT_EXCEEDED';
        if (upper === 'RUNTIME ERROR' || upper.startsWith('RUNTIME ERROR ')) return 'RUNTIME_ERROR';
        if (upper === 'COMPILE ERROR' || upper.startsWith('COMPILE ERROR ')) return 'COMPILE_ERROR';
        if (upper === 'MEMORY LIMIT EXCEEDED' || upper.startsWith('MEMORY LIMIT EXCEEDED ')) return 'MEMORY_LIMIT_EXCEEDED';
      }
    }

    return null;
  },

  /**
   * Normalizes raw LeetCode result string to enum.
   * Returns null if text does not represent a genuine verdict.
   */
  normalizeResult(rawText) {
    if (!rawText) return null;
    const text = rawText.trim().toUpperCase();

    // Check failure modes first
    if (text.includes('WRONG ANSWER')) return 'WRONG_ANSWER';
    if (text.includes('TIME LIMIT') || text.includes('TLE')) return 'TIME_LIMIT_EXCEEDED';
    if (text.includes('RUNTIME ERROR')) return 'RUNTIME_ERROR';
    if (text.includes('COMPILE ERROR')) return 'COMPILE_ERROR';
    if (text.includes('MEMORY LIMIT')) return 'MEMORY_LIMIT_EXCEEDED';

    // Check Accepted (strict: must start with or equal ACCEPTED to prevent matching random text)
    if (text === 'ACCEPTED' || text.startsWith('ACCEPTED ') || text.includes('ACCEPTED (')) {
      return 'ACCEPTED';
    }

    return null;
  }
};
