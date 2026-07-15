import { AnalysisResponse, ComparisonResponse } from '../types/analysis';
import httpClient from './httpClient';

/**
 * API service functions for the GitHub Repository Analyzer.
 *
 * All functions use the configured httpClient which handles:
 * - Base URL injection
 * - Offline mode header injection
 * - Error transformation to ApiError objects
 */

/**
 * Analyzes a single GitHub repository.
 *
 * @param owner - GitHub username or organization
 * @param repo  - Repository name
 * @returns Enriched analysis response
 */
export const analyzeRepo = async (
  owner: string,
  repo: string
): Promise<AnalysisResponse> => {
  const response = await httpClient.get<AnalysisResponse>('/api/analyze', {
    params: { owner, repo },
  });
  return response.data;
};

/**
 * Compares two GitHub repositories.
 *
 * Makes two parallel API calls using Promise.all for performance.
 * The comparison winner is computed server-side.
 *
 * @returns Comparison response with both analyses and winner
 */
export const compareRepos = async (
  ownerA: string, repoA: string,
  ownerB: string, repoB: string
): Promise<ComparisonResponse> => {
  const response = await httpClient.get<ComparisonResponse>('/api/compare', {
    params: { ownerA, repoA, ownerB, repoB },
  });
  return response.data;
};

/**
 * Fetches current cache statistics.
 */
export const getCacheStats = async () => {
  const response = await httpClient.get('/api/cache/stats');
  return response.data;
};

/**
 * Clears the entire server-side cache.
 */
export const clearCache = async (): Promise<void> => {
  await httpClient.delete('/api/cache');
};

/**
 * Evicts a specific repository from the cache.
 */
export const evictFromCache = async (owner: string, repo: string): Promise<void> => {
  await httpClient.delete(`/api/cache/${owner}/${repo}`);
};
