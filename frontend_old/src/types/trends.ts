export interface TrendSnapshot {
  timestamp: string;
  healthScore: number;
  grade: string;
  stars: number;
  forks: number;
  openIssues: number;
  totalContributors: number;
  commits52Weeks: number;
  primaryLanguage: string;
}

export interface TrendSummary {
  healthScoreChange: number;
  healthScoreTrend: 'IMPROVING' | 'STABLE' | 'DECLINING' | 'INSUFFICIENT_DATA';
  starsGrowth: number;
  starsGrowthRate: string;
  snapshotCount: number;
  daysTracked: number;
}

export interface TrendsResponse {
  owner: string;
  repo: string;
  snapshots: TrendSnapshot[];
  summary?: TrendSummary;
  message?: string;
}
