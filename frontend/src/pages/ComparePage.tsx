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

  const [ownerA, setOwnerA] = useState('');
  const [ownerB, setOwnerB] = useState('');

  const handleCompare = async (oA: string, rA: string, oB: string, rB: string) => {
    setIsLoading(true);
    setError(null);
    setRepoA(null);
    setRepoB(null);

    try {
      // Parallel API calls
      const [resultA, resultB] = await Promise.all([
        analyzeRepo(oA, rA),
        analyzeRepo(oB, rB),
      ]);
      setRepoA(resultA);
      setRepoB(resultB);
      setOwnerA(oA);
      setOwnerB(oB);
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

      {/* Two search bars */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
        <div>
          <label style={{ display: 'block', marginBottom: '8px', fontWeight: 600, color: '#374151' }}>
            Repository A
          </label>
          <SearchBar
            onAnalyze={(o, r) => handleCompare(o, r, ownerB || 'vuejs', 'vue')}
            isLoading={isLoading}
            placeholder="e.g. facebook/react"
          />
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '8px', fontWeight: 600, color: '#374151' }}>
            Repository B
          </label>
          <SearchBar
            onAnalyze={(o, r) => handleCompare(ownerA || 'facebook', 'react', o, r)}
            isLoading={isLoading}
            placeholder="e.g. vuejs/vue"
          />
        </div>
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
