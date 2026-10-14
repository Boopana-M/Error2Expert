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
      || document.querySelector('[class*="text-olive"]'); // Easy on some LeetCode variants

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
   * Normalizes raw LeetCode result string to enum
   */
  normalizeResult(rawText) {
    if (!rawText) return 'WRONG_ANSWER';
    const text = rawText.toUpperCase();
    if (text.includes('ACCEPTED')) return 'ACCEPTED';
    if (text.includes('TIME LIMIT') || text.includes('TLE')) return 'TIME_LIMIT_EXCEEDED';
    if (text.includes('RUNTIME ERROR')) return 'RUNTIME_ERROR';
    if (text.includes('COMPILE ERROR')) return 'COMPILE_ERROR';
    return 'WRONG_ANSWER';
  }
};
