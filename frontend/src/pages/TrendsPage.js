import React, { useState } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { getTrends } from '../services/analyzerApi';
import styles from './TrendsPage.module.css';

const METRICS = [
  { key: 'healthScore',  label: 'Health Score',  color: '#3b82f6' },
  { key: 'stars',        label: 'Stars',          color: '#f59e0b' },
  { key: 'forks',        label: 'Forks',          color: '#8b5cf6' },
  { key: 'openIssues',   label: 'Open Issues',    color: '#ef4444' },
  { key: 'contributors', label: 'Contributors',   color: '#10b981' },
  { key: 'commits',      label: 'Commits (52w)',  color: '#06b6d4' },
];

function getTrendIcon(trend) {
  if (trend === 'IMPROVING') return '📈';
  if (trend === 'DECLINING') return '📉';
  if (trend === 'STABLE') return '➡️';
  return '📊';
}

function TrendsPage() {
  const [owner, setOwner] = useState('');
  const [repo, setRepo] = useState('');
  const [trendsData, setTrendsData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedMetric, setSelectedMetric] = useState('healthScore');

  async function handleSearch() {
    if (!owner.trim() || !repo.trim()) {
      setError('Please enter both owner and repository name');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await getTrends(owner.trim(), repo.trim());
      setTrendsData(data);
    } catch (err) {
      setError(err.message || 'Failed to fetch trends data');
      setTrendsData(null);
    } finally {
      setLoading(false);
    }
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter') handleSearch();
  }

  const config = METRICS.find((m) => m.key === selectedMetric);

  const chartData = (trendsData?.snapshots || []).map((s) => {
    const date = new Date(s.timestamp);
    return {
      date: date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
      fullDate: date.toLocaleString(),
      healthScore: s.healthScore,
      stars: s.stars,
      forks: s.forks,
      openIssues: s.openIssues,
      contributors: s.totalContributors,
      commits: s.commits52Weeks,
    };
  });

  return (
    <div className={styles.page}>
      <h1 className={styles.pageTitle}>📈 Repository Trends</h1>
      <p className={styles.pageSubtitle}>
        View historical metrics and track how a repository evolves over time.
      </p>

      <div className={styles.searchRow}>
        <div className={styles.inputGroup}>
          <label className={styles.inputLabel}>Owner</label>
          <input
            type="text"
            placeholder="e.g. facebook"
            value={owner}
            onChange={(e) => setOwner(e.target.value)}
            onKeyDown={handleKeyDown}
            className={styles.input}
          />
        </div>
        <div className={styles.inputGroup}>
          <label className={styles.inputLabel}>Repository</label>
          <input
            type="text"
            placeholder="e.g. react"
            value={repo}
            onChange={(e) => setRepo(e.target.value)}
            onKeyDown={handleKeyDown}
            className={styles.input}
          />
        </div>
        <button
          onClick={handleSearch}
          disabled={loading}
          className={styles.btn}
          style={{ backgroundColor: loading ? '#9ca3af' : '#2563eb' }}
        >
          {loading ? 'Loading...' : 'View Trends'}
        </button>
      </div>

      {error && (
        <div className={styles.errorBox}>{error}</div>
      )}

      {trendsData && (
        trendsData.message ? (
          <div className={styles.emptyState}>
            <div className={styles.emptyIcon}>📊</div>
            <h3 className={styles.emptyTitle}>No Historical Data Yet</h3>
            <p className={styles.emptyText}>{trendsData.message}</p>
          </div>
        ) : (
          <>
            {trendsData.summary && (
              <div className={styles.summaryGrid}>
                <div className={styles.summaryCard}>
                  <div className={styles.summaryLabel}>Health Score Trend</div>
                  <div className={styles.summaryValue}>
                    <span className={styles.summaryIcon}>{getTrendIcon(trendsData.summary.healthScoreTrend)}</span>
                    <span className={styles.summaryNum}>
                      {trendsData.summary.healthScoreChange > 0 ? '+' : ''}{trendsData.summary.healthScoreChange}
                    </span>
                  </div>
                  <div className={styles.summaryMeta}>{trendsData.summary.healthScoreTrend}</div>
                </div>

                <div className={styles.summaryCard}>
                  <div className={styles.summaryLabel}>Stars Growth</div>
                  <div className={styles.summaryNum}>
                    {trendsData.summary.starsGrowth > 0 ? '+' : ''}{trendsData.summary.starsGrowth.toLocaleString()}
                  </div>
                  <div className={styles.summaryMeta}>{trendsData.summary.starsGrowthRate}</div>
                </div>

                <div className={styles.summaryCard}>
                  <div className={styles.summaryLabel}>Snapshots</div>
                  <div className={styles.summaryNum}>{trendsData.summary.snapshotCount}</div>
                  <div className={styles.summaryMeta}>over {trendsData.summary.daysTracked} days</div>
                </div>
              </div>
            )}

            <div className={styles.metricTabs}>
              {METRICS.map((m) => (
                <button
                  key={m.key}
                  onClick={() => setSelectedMetric(m.key)}
                  className={styles.metricBtn}
                  style={{
                    backgroundColor: selectedMetric === m.key ? '#2563eb' : '#fff',
                    color: selectedMetric === m.key ? '#fff' : '#374151',
                    border: `1px solid ${selectedMetric === m.key ? '#2563eb' : '#d1d5db'}`,
                  }}
                >
                  {m.label}
                </button>
              ))}
            </div>

            <div className={styles.chartCard}>
              <div className={styles.chartInner}>
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={chartData} margin={{ top: 5, right: 30, left: 10, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                    <XAxis dataKey="date" stroke="#9ca3af" style={{ fontSize: '12px' }} />
                    <YAxis stroke="#9ca3af" style={{ fontSize: '12px' }} />
                    <Tooltip
                      contentStyle={{ backgroundColor: '#fff', border: '1px solid #e5e7eb', borderRadius: '8px', padding: '10px' }}
                      labelFormatter={(label, payload) =>
                        payload?.length > 0 ? payload[0].payload.fullDate : label
                      }
                    />
                    <Legend wrapperStyle={{ paddingTop: '16px' }} />
                    <Line
                      type="monotone"
                      dataKey={selectedMetric}
                      stroke={config?.color}
                      strokeWidth={2}
                      dot={{ fill: config?.color, r: 4 }}
                      activeDot={{ r: 6 }}
                      name={config?.label}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </div>
          </>
        )
      )}
    </div>
  );
}

export default TrendsPage;
