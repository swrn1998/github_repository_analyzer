import React from 'react';
import SearchBar from '../components/search/SearchBar';
import AnalysisDashboard from '../components/dashboard/AnalysisDashboard';
import { useAnalysis } from '../contexts/AnalysisContext';
import styles from './AnalyzePage.module.css';

function AnalyzePage() {
  const { result, status, error, analyze } = useAnalysis();
  const isLoading = status === 'loading';

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <h1 className={styles.pageTitle}>Repository Analyzer</h1>
        <p className={styles.pageSubtitle}>
          Enter a GitHub repository to analyze its health, activity, and code quality.
        </p>
      </div>

      <div className={styles.searchWrapper}>
        <SearchBar onAnalyze={analyze} isLoading={isLoading} />
      </div>

      {isLoading && (
        <div role="status" aria-live="polite" className={styles.loadingState}>
          <div className={styles.loadingIcon}>⏳</div>
          <div className={styles.loadingText}>Analyzing repository...</div>
          <div className={styles.loadingSubtext}>Fetching data from GitHub API</div>
        </div>
      )}

      {status === 'error' && error && (
        <div role="alert" className={styles.errorBox}>
          <strong>Error: </strong>{error.message}
          {error.code === 'REPO_NOT_FOUND' && (
            <div className={styles.errorHint}>
              Make sure the repository exists and is public.
            </div>
          )}
        </div>
      )}

      {status === 'success' && result && (
        <AnalysisDashboard data={result} />
      )}

      {status === 'idle' && (
        <div className={styles.emptyState}>
          <div className={styles.emptyIcon}>🔍</div>
          <div className={styles.emptyText}>Enter a repository above to get started</div>
          <div className={styles.emptyHint}>
            Try: <code>facebook/react</code> or <code>microsoft/vscode</code>
          </div>
        </div>
      )}
    </div>
  );
}

export default AnalyzePage;
