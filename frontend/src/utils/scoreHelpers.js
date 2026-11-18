export function gradeToColor(grade) {
  const map = { A: '#22c55e', B: '#84cc16', C: '#eab308', D: '#f97316', F: '#ef4444' };
  return map[grade] ?? '#94a3b8';
}

export function scoreToColor(score) {
  if (score >= 90) return '#22c55e';
  if (score >= 75) return '#84cc16';
  if (score >= 60) return '#eab308';
  if (score >= 40) return '#f97316';
  return '#ef4444';
}

export function gradeToLabel(grade) {
  const map = { A: 'Excellent', B: 'Good', C: 'Fair', D: 'Poor', F: 'Critical' };
  return map[grade] ?? 'Unknown';
}

export function dataSourceMeta(source) {
  const map = {
    LIVE:  { label: 'Live',       color: '#22c55e', icon: '●', description: 'Live data from GitHub API' },
    CACHE: { label: 'Cached',     color: '#3b82f6', icon: '🗄', description: 'Showing cached data' },
    STALE: { label: 'Stale',      color: '#eab308', icon: '⚠', description: 'GitHub API down — showing stale cache' },
    MOCK:  { label: 'Demo Mode',  color: '#94a3b8', icon: '📴', description: 'Offline mode — showing demo data' },
  };
  return map[source];
}

export function severityToColor(severity) {
  const map = { INFO: '#3b82f6', WARNING: '#eab308', CRITICAL: '#ef4444' };
  return map[severity];
}
