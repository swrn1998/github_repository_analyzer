import React, { useState } from 'react';
import SearchBar from '../components/search/SearchBar';
import AnalysisDashboard from '../components/dashboard/AnalysisDashboard';
import { AnalysisResponse, ApiError } from '../types/analysis';
import { analyzeRepo } from '../services/analyzerApi';
import { gradeToColor } from '../utils/scoreHelpers';

/**
 * Compare two repositories side by side.
 * Makes parallel API calls and renders dual dashboards with a winner summary.
 */
const ComparePage: React.FC = () => {
  const [repoA, setRepoA] = useState<AnalysisResponse | null>(null);
  const [repoB, setRepoB] = useState<AnalysisResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Store input values separately - don't auto-trigger
  const [inputA, setInputA] = useState<{ owner: string; repo: string } | null>(null);
  const [inputB, setInputB] = useState<{ owner: string; repo: string } | null>(null);

  const handleCompareClick = async () => {
    if (!inputA || !inputB) {
      setError('Please enter both repositories to compare');
      return;
    }

    setIsLoading(true);
    setError(null);
    setRepoA(null);
    setRepoB(null);

    try {
      // Parallel API calls with the exact stored values
      const [resultA, resultB] = await Promise.all([
        analyzeRepo(inputA.owner, inputA.repo),
        analyzeRepo(inputB.owner, inputB.repo),
      ]);
      setRepoA(resultA);
      setRepoB(resultB);
    } catch (err) {
      const apiErr = err as ApiError;
      setError(apiErr.message || 'Comparison failed');
    } finally {
      setIsLoading(false);
    }
  };

  const scoreA = repoA?.healthScore.score ?? 0;
  const scoreB = repoB?.healthScore.score ?? 0;
  const winner = scoreA > scoreB ? repoA?.owner + '/' + repoA?.repo
    : scoreB > scoreA ? repoB?.owner + '/' + repoB?.repo
    : 'Tie';

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '32px 24px' }}>
      <h1 style={{ margin: '0 0 8px', fontSize: '28px', fontWeight: 700, color: '#111827' }}>
        Compare Repositories
      </h1>
      <p style={{ margin: '0 0 32px', color: '#6b7280' }}>
        Analyze two repositories side by side to compare health scores and metrics.
      </p>

      {/* Two search bars with separate keys to prevent state collision */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
        <div>
          <label style={{ display: 'block', marginBottom: '8px', fontWeight: 600, color: '#374151' }}>
            Repository A
          </label>
          <SearchBar
            key="search-bar-a"
            onAnalyze={(owner, repo) => {
              setInputA({ owner, repo });
              setError(null);
            }}
            isLoading={isLoading}
            placeholder="e.g. facebook/react"
            buttonText="Set"
          />
          {inputA && (
            <div style={{ fontSize: '13px', color: '#059669', marginTop: '4px' }}>
              ✓ {inputA.owner}/{inputA.repo}
            </div>
          )}
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '8px', fontWeight: 600, color: '#374151' }}>
            Repository B
          </label>
          <SearchBar
            key="search-bar-b"
            onAnalyze={(owner, repo) => {
              setInputB({ owner, repo });
              setError(null);
            }}
            isLoading={isLoading}
            placeholder="e.g. vuejs/vue"
            buttonText="Set"
          />
          {inputB && (
            <div style={{ fontSize: '13px', color: '#059669', marginTop: '4px' }}>
              ✓ {inputB.owner}/{inputB.repo}
            </div>
          )}
        </div>
      </div>

      {/* Compare button */}
      <div style={{ marginBottom: '16px', textAlign: 'center' }}>
        <button
          onClick={handleCompareClick}
          disabled={isLoading || !inputA || !inputB}
          style={{
            padding: '12px 32px',
            backgroundColor: isLoading || !inputA || !inputB ? '#9ca3af' : '#2563eb',
            color: 'white',
            border: 'none',
            borderRadius: '8px',
            cursor: isLoading || !inputA || !inputB ? 'not-allowed' : 'pointer',
            fontSize: '16px',
            fontWeight: 600,
          }}
        >
          {isLoading ? 'Comparing...' : 'Compare Repositories'}
        </button>
      </div>

      {error && (
        <div role="alert" style={{ color: '#dc2626', padding: '12px', backgroundColor: '#fef2f2', borderRadius: '8px', marginBottom: '20px' }}>
          {error}
        </div>
      )}

      {/* Winner summary */}
      {repoA && repoB && (
        <div style={{
          textAlign: 'center',
          padding: '16px',
          backgroundColor: '#f0fdf4',
          border: '1px solid #86efac',
          borderRadius: '12px',
          marginBottom: '24px',
          fontSize: '16px',
          fontWeight: 600,
          color: '#166534',
        }}>
          🏆 {winner === 'Tie' ? "It's a tie!" : `Winner: ${winner}`}
          {winner !== 'Tie' && (
            <span style={{ fontWeight: 400, color: '#374151', marginLeft: '8px' }}>
              by {Math.abs(scoreA - scoreB)} health score points
            </span>
          )}
        </div>
      )}

      {/* Side-by-side dashboards */}
      {repoA && repoB && (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>
          <div style={{
            border: scoreA > scoreB ? `2px solid ${gradeToColor(repoA.healthScore.grade)}` : '2px solid transparent',
            borderRadius: '16px',
            padding: '4px',
          }}>
            <AnalysisDashboard data={repoA} />
          </div>
          <div style={{
            border: scoreB > scoreA ? `2px solid ${gradeToColor(repoB.healthScore.grade)}` : '2px solid transparent',
            borderRadius: '16px',
            padding: '4px',
          }}>
            <AnalysisDashboard data={repoB} />
          </div>
        </div>
      )}
    </div>
  );
};

export default ComparePage;
