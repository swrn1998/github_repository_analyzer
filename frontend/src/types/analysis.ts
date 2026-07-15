/**
 * TypeScript interfaces for the GitHub Repository Analyzer API.
 * These types mirror the Java domain model — keep them in sync with the backend.
 */

// ── Enums ────────────────────────────────────────────────────────────────────

export type DataSource = 'LIVE' | 'CACHE' | 'STALE' | 'MOCK';
export type AlertType = 'INACTIVE_REPO' | 'HIGH_ISSUE_BACKLOG' | 'NO_TESTS' | 'SECURITY_VULNERABILITIES';
export type Severity = 'INFO' | 'WARNING' | 'CRITICAL';
export type Grade = 'A' | 'B' | 'C' | 'D' | 'F';

// ── Domain types ─────────────────────────────────────────────────────────────

export interface RepoStats {
  stars: number;
  forks: number;
  watchers: number;
  openIssues: number;
  defaultBranch: string;
  description: string | null;
  license: string | null;
  hasWiki: boolean;
  hasTopics: boolean;
  homepage: string | null;
  createdAt: string;      // ISO 8601 date string
  updatedAt: string;
  pushedAt: string;
}

export interface ScoreBreakdown {
  commitActivityScore: number;   // 0-25
  issueRatioScore: number;       // 0-25
  communityScore: number;        // 0-25
  documentationScore: number;    // 0-25
}

export interface HealthScore {
  score: number;          // 0-100
  grade: Grade;
  breakdown: ScoreBreakdown;
}

export interface Alert {
  type: AlertType;
  message: string;
  severity: Severity;
}

export interface LanguageDistribution {
  primaryLanguage: string;
  rawBytes: Record<string, number>;
  percentages: Record<string, number>;
}

export interface ContributorSummary {
  login: string;
  totalCommits: number;
  weeksActive: number;
}

export interface ContributorActivity {
  totalContributors: number;
  totalCommitsLast52Weeks: number;
  lastCommitDate: string | null;
  topContributors: ContributorSummary[];
}

export interface HateoasLink {
  href: string;
}

// ── API Response ─────────────────────────────────────────────────────────────

export interface AnalysisResponse {
  owner: string;
  repo: string;
  stats: RepoStats;
  healthScore: HealthScore;
  alerts: Alert[];
  languages: LanguageDistribution;
  contributorActivity: ContributorActivity;
  source: DataSource;
  cachedAt: string | null;
  notice?: string | null;
  _links: Record<string, HateoasLink>;
}

export interface ComparisonResponse {
  repoA: AnalysisResponse;
  repoB: AnalysisResponse;
  winner: string;             // "owner/repo" or "tie"
  healthScoreDelta: number;   // repoA.score - repoB.score
  summary: string;
}

// ── Request / Error types ─────────────────────────────────────────────────────

export interface AnalysisRequest {
  owner: string;
  repo: string;
}

export interface ApiError {
  code: string;
  message: string;
  timestamp: string;
  status: number;
}

export type RequestStatus = 'idle' | 'loading' | 'success' | 'error';
