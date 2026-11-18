import React, { useState } from 'react';
import SearchBar from '../components/search/SearchBar';
import AnalysisDashboard from '../components/dashboard/AnalysisDashboard';
import { analyzeRepo } from '../services/analyzerApi';
import { gradeToColor } from '../utils/scoreHelpers';
import styles from './ComparePage.module.css';

function ComparePage() {
  const [repoA, setRepoA] = useState(null);
  const [repoB, setRepoB] = useState(null);
  const [inputA, setInputA] = useState(null);
  const [inputB, setInputB] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  async function handleCompare() {
    if (!inputA || !inputB) {
      setError('Please enter both repositories to compare');
      return;
    }
    setIsLoading(true);
    setError(null);
    setRepoA(null);
    setRepoB(null);
    try {
      const [resultA, resultB] = await Promise.all([
        analyzeRepo(inputA.owner, inputA.repo),
        analyzeRepo(inputB.owner, inputB.repo),
      ]);
      setRepoA(resultA);
      setRepoB(resultB);
    } catch (err) {
      setError(err.message || 'Comparison failed');
    } finally {
      setIsLoading(false);
    }
  }

  const scoreA = repoA?.healthScore.score ?? 0;
  const scoreB = repoB?.healthScore.score ?? 0;
  const winner =
    scoreA > scoreB ? `${repoA.owner}/${repoA.repo}` :
    scoreB > scoreA ? `${repoB.owner}/${repoB.repo}` :
    'Tie';

  return (
    <div className={styles.page}>
      <h1 className={styles.pageTitle}>Compare Repositories</h1>
      <p className={styles.pageSubtitle}>Analyze two repositories side by side.</p>

      <div className={styles.searchGrid}>
        <div>
          <label className={styles.searchLabel}>Repository A</label>
          <SearchBar
            key="repo-a"
            onAnalyze={(owner, repo) => { setInputA({ owner, repo }); setError(null); }}
            isLoading={isLoading}
            placeholder="e.g. facebook/react"
            buttonText="Set"
          />
          {inputA && (
            <div className={styles.setConfirm}>✓ {inputA.owner}/{inputA.repo}</div>
          )}
        </div>
        <div>
          <label className={styles.searchLabel}>Repository B</label>
          <SearchBar
            key="repo-b"
            onAnalyze={(owner, repo) => { setInputB({ owner, repo }); setError(null); }}
            isLoading={isLoading}
            placeholder="e.g. vuejs/vue"
            buttonText="Set"
          />
          {inputB && (
            <div className={styles.setConfirm}>✓ {inputB.owner}/{inputB.repo}</div>
          )}
        </div>
      </div>

      <div className={styles.compareBtn}>
        <button
          onClick={handleCompare}
          disabled={isLoading || !inputA || !inputB}
          className={styles.btn}
          style={{ backgroundColor: isLoading || !inputA || !inputB ? '#9ca3af' : '#2563eb' }}
        >
          {isLoading ? 'Comparing...' : 'Compare Repositories'}
        </button>
      </div>

      {error && (
        <div role="alert" className={styles.errorBox}>{error}</div>
      )}

      {repoA && repoB && (
        <div className={styles.winnerBanner}>
          🏆 {winner === 'Tie' ? "It's a tie!" : `Winner: ${winner}`}
          {winner !== 'Tie' && (
            <span className={styles.winnerDelta}>by {Math.abs(scoreA - scoreB)} points</span>
          )}
        </div>
      )}

      {repoA && repoB && (
        <div className={styles.compareGrid}>
          <div
            className={styles.repoPane}
            style={{ border: scoreA > scoreB ? `2px solid ${gradeToColor(repoA.healthScore.grade)}` : '2px solid transparent' }}
          >
            <AnalysisDashboard data={repoA} />
          </div>
          <div
            className={styles.repoPane}
            style={{ border: scoreB > scoreA ? `2px solid ${gradeToColor(repoB.healthScore.grade)}` : '2px solid transparent' }}
          >
            <AnalysisDashboard data={repoB} />
          </div>
        </div>
      )}
    </div>
  );
}

export default ComparePage;
