import { DataSource, Grade, Severity } from '../types/analysis';

/**
 * Maps a grade letter to a Tailwind-like color class.
 * Used for styling grade badges and score panels.
 */
export const gradeToColor = (grade: Grade): string => {
  const map: Record<Grade, string> = {
    A: '#22c55e',  // green-500
    B: '#84cc16',  // lime-500
    C: '#eab308',  // yellow-500
    D: '#f97316',  // orange-500
    F: '#ef4444',  // red-500
  };
  return map[grade] ?? '#94a3b8';
};

/**
 * Maps a score (0-100) to a color for the circular gauge.
 */
export const scoreToColor = (score: number): string => {
  if (score >= 90) return '#22c55e';  // green
  if (score >= 75) return '#84cc16';  // lime
  if (score >= 60) return '#eab308';  // yellow
  if (score >= 40) return '#f97316';  // orange
  return '#ef4444';                   // red
};

/**
 * Maps a grade to its human-readable label.
 */
export const gradeToLabel = (grade: Grade): string => {
  const map: Record<Grade, string> = {
    A: 'Excellent',
    B: 'Good',
    C: 'Fair',
    D: 'Poor',
    F: 'Critical',
  };
  return map[grade] ?? 'Unknown';
};

/**
 * Maps a DataSource to display metadata for the DataSourceBanner.
 */
export const dataSourceMeta = (source: DataSource): {
  label: string;
  color: string;
  icon: string;
  description: string;
} => {
  const map: Record<DataSource, { label: string; color: string; icon: string; description: string }> = {
    LIVE: {
      label: 'Live',
      color: '#22c55e',
      icon: '●',
      description: 'Data fetched live from GitHub API',
    },
    CACHE: {
      label: 'Cached',
      color: '#3b82f6',
      icon: '🗄',
      description: 'Showing cached data',
    },
    STALE: {
      label: 'Stale Data',
      color: '#eab308',
      icon: '⚠',
      description: 'GitHub API unavailable — showing outdated cached data',
    },
    MOCK: {
      label: 'Demo Mode',
      color: '#94a3b8',
      icon: '📴',
      description: 'Offline mode — showing demo data',
    },
  };
  return map[source];
};

/**
 * Maps a Severity to display color for AlertCards.
 */
export const severityToColor = (severity: Severity): string => {
  const map: Record<Severity, string> = {
    INFO: '#3b82f6',
    WARNING: '#eab308',
    CRITICAL: '#ef4444',
  };
  return map[severity];
};
