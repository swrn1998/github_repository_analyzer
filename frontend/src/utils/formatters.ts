/**
 * Formatting utilities for display values in the UI.
 */

/**
 * Formats a large number with K/M suffixes.
 * e.g., 220000 → "220K", 1500000 → "1.5M"
 */
export const formatNumber = (value: number): string => {
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1)}M`;
  if (value >= 1_000) return `${(value / 1_000).toFixed(value >= 10_000 ? 0 : 1)}K`;
  return String(value);
};

/**
 * Formats an ISO date string as a relative time.
 * e.g., "2 hours ago", "3 days ago", "5 months ago"
 */
export const formatRelativeTime = (isoDate: string | null): string => {
  if (!isoDate) return 'Never';

  const date = new Date(isoDate);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  const diffMonths = Math.floor(diffDays / 30);
  const diffYears = Math.floor(diffDays / 365);

  if (diffHours < 1) return 'Just now';
  if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
  if (diffDays < 30) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
  if (diffMonths < 12) return `${diffMonths} month${diffMonths > 1 ? 's' : ''} ago`;
  return `${diffYears} year${diffYears > 1 ? 's' : ''} ago`;
};

/**
 * Formats a date as a short readable string.
 * e.g., "Jun 15, 2026"
 */
export const formatDate = (isoDate: string | null): string => {
  if (!isoDate) return 'N/A';
  return new Date(isoDate).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
};

/**
 * Parses an "owner/repo" string and returns { owner, repo } or null if invalid.
 */
export const parseRepoInput = (input: string): { owner: string; repo: string } | null => {
  const trimmed = input.trim();
  const parts = trimmed.split('/');
  if (parts.length !== 2) return null;
  const [owner, repo] = parts.map(p => p.trim());
  if (!owner || !repo) return null;
  // Basic validation: only allow GitHub-valid characters
  const valid = /^[a-zA-Z0-9._-]{1,100}$/;
  if (!valid.test(owner) || !valid.test(repo)) return null;
  return { owner, repo };
};
