package com.codememory.entity.enums;

/**
 * Lifecycle status of a coding mistake.
 * OPEN: Initial mistake detected on a failed attempt; not yet resolved.
 * RESOLVED: Subsequent successful attempt(s) proved mastery and eliminated the error.
 * RECURRING: Error reappeared in a future attempt after having been previously resolved.
 */
public enum MistakeStatus {
    OPEN,
    RESOLVED,
    RECURRING
}
