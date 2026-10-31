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
    // Helper to check if an element belongs to the sample testcase runner rather than the authentic submission
    const isInsideTestcaseConsole = (el) => {
      if (!el) return false;
      const testcaseParent = el.closest('[data-layout-path*="testcase"]') ||
                             el.closest('[data-e2e-locator="console-result"]') ||
                             el.closest('.testcase-container') ||
                             el.closest('#test-result');
      if (testcaseParent) return true;

      // Check if neighboring text contains testcase indicators (e.g. "Case 1", "Case 2", "Testcase")
      const parent = el.parentElement;
      if (parent) {
        const pText = (parent.textContent || '');
        if (pText.includes('Case 1') || pText.includes('Testcase') || pText.includes('Runtime:')) {
          return true;
        }
      }
      return false;
    };

    // 1. Check for authentic submission result container first
    const e2eEl = document.querySelector('[data-e2e-locator="submission-result"]');
    if (e2eEl && !isInsideTestcaseConsole(e2eEl)) {
      const normalized = this.normalizeResult(e2eEl.textContent.trim());
      if (normalized) return normalized;
    }

    // 2. Scan for FAILURE verdicts FIRST across the whole page (failure always supersedes sample testcase passes)
    const failureSelectors = [
      'div[class*="text-red-"]',
      'span[class*="text-red-"]',
      '[class*="result-status"]',
      '[class*="status-column"]'
    ];

    for (const sel of failureSelectors) {
      const elements = document.querySelectorAll(sel);
      for (const el of elements) {
        if (isInsideTestcaseConsole(el)) continue;
        const txt = (el.textContent || '').trim().toUpperCase();
        if (txt === 'WRONG ANSWER' || txt.startsWith('WRONG ANSWER')) return 'WRONG_ANSWER';
        if (txt.includes('TIME LIMIT') || txt.startsWith('TLE')) return 'TIME_LIMIT_EXCEEDED';
        if (txt.includes('RUNTIME ERROR')) return 'RUNTIME_ERROR';
        if (txt.includes('COMPILE ERROR')) return 'COMPILE_ERROR';
        if (txt.includes('MEMORY LIMIT')) return 'MEMORY_LIMIT_EXCEEDED';
      }
    }

    // 3. Check for submission tabs / header text explicitly
    const tabElements = document.querySelectorAll('div, span, button, a');
    for (const el of tabElements) {
      if (isInsideTestcaseConsole(el)) continue;
      const txt = (el.textContent || '').trim().toUpperCase();
      if (txt === 'WRONG ANSWER') return 'WRONG_ANSWER';
    }

    // 4. Finally check for genuine ACCEPTED submission (ignoring testcase console)
    const acceptedSelectors = [
      'div[data-e2e-locator="submission-result"]',
      'span[data-e2e-locator="submission-result"]',
      'div[class*="text-green-"]',
      'span[class*="text-green-"]'
    ];

    for (const sel of acceptedSelectors) {
      const elements = document.querySelectorAll(sel);
      for (const el of elements) {
        if (isInsideTestcaseConsole(el)) continue;
        const txt = (el.textContent || '').trim().toUpperCase();
        if (txt === 'ACCEPTED' || txt.startsWith('ACCEPTED ') || txt.includes('ACCEPTED (')) {
          // Double check it's not a tab label like "Accepted Solutions"
          if (!txt.includes('SOLUTION') && !txt.includes('EDITORIAL')) {
            return 'ACCEPTED';
          }
        }
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
