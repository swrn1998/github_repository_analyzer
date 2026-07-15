import React from 'react';
import SearchBar from '../components/search/SearchBar';
import AnalysisDashboard from '../components/dashboard/AnalysisDashboard';
import { useAnalysis } from '../contexts/AnalysisContext';

/**
 * Main analysis page — the default route ("/").
 * Composes SearchBar + AnalysisDashboard with shared state from AnalysisContext.
 */
const AnalyzePage: React.FC = () => {
  const { result, status, error, analyze } = useAnalysis();
  const isLoading = status === 'loading';

  return (
    <div style={{ maxWidth: '960px', margin: '0 auto', padding: '32px 24px' }}>
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ margin: '0 0 8px', fontSize: '28px', fontWeight: 700, color: '#111827' }}>
          Repository Analyzer
        </h1>
        <p style={{ margin: 0, color: '#6b7280', fontSize: '16px' }}>
          Enter a GitHub repository to analyze its health, activity, and code quality metrics.
        </p>
      </div>

      <div style={{ marginBottom: '32px' }}>
        <SearchBar
          onAnalyze={analyze}
          isLoading={isLoading}
        />
      </div>

      {isLoading && (
        <div
          role="status"
          aria-live="polite"
          style={{
            textAlign: 'center',
            padding: '60px 0',
            color: '#6b7280',
          }}
        >
          <div style={{ fontSize: '32px', marginBottom: '12px' }}>⏳</div>
          <div style={{ fontSize: '16px' }}>Analyzing repository...</div>
          <div style={{ fontSize: '13px', color: '#9ca3af', marginTop: '4px' }}>
            Fetching data from GitHub API
          </div>
        </div>
      )}

      {status === 'error' && error && (
        <div
          role="alert"
          style={{
            padding: '20px',
            backgroundColor: '#fef2f2',
            border: '1px solid #fca5a5',
            borderRadius: '12px',
            color: '#dc2626',
          }}
        >
          <strong>Error: </strong>{error.message}
          {error.code === 'REPO_NOT_FOUND' && (
            <div style={{ marginTop: '8px', fontSize: '14px', color: '#6b7280' }}>
              Make sure the repository exists and is public, or check the owner/repo spelling.
            </div>
          )}
        </div>
      )}

      {status === 'success' && result && (
        <AnalysisDashboard data={result} />
      )}

      {status === 'idle' && (
        <div style={{
          textAlign: 'center',
          padding: '60px 0',
          color: '#9ca3af',
        }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>🔍</div>
          <div style={{ fontSize: '16px' }}>Enter a repository above to get started</div>
          <div style={{ fontSize: '13px', marginTop: '8px' }}>
            Try: <code style={{ backgroundColor: '#f3f4f6', padding: '2px 6px', borderRadius: '4px' }}>facebook/react</code>
            {' '}or{' '}
            <code style={{ backgroundColor: '#f3f4f6', padding: '2px 6px', borderRadius: '4px' }}>microsoft/vscode</code>
          </div>
        </div>
      )}
    </div>
  );
};

export default AnalyzePage;
