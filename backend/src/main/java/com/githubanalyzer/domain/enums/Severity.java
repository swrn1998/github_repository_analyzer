package com.githubanalyzer.domain.enums;

/**
 * Severity level for repository health alerts.
 *
 * <p>Used by the UI to determine visual styling:
 * <ul>
 *   <li>INFO     → blue border, informational icon</li>
 *   <li>WARNING  → yellow border, warning icon</li>
 *   <li>CRITICAL → red border, danger icon</li>
 * </ul>
 */
public enum Severity {

    /** Informational — good to know, no immediate action required. */
    INFO,

    /** Warrants attention — may indicate a maintenance concern. */
    WARNING,

    /** Requires immediate action — security or severe quality issue. */
    CRITICAL
}
