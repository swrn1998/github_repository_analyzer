import React, { useState } from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import httpClient from '../services/httpClient';

interface TrendSnapshot {
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

interface TrendSummary {
  healthScoreChange: number;
  healthScoreTrend: 'IMPROVING' | 'STABLE' | 'DECLINING' | 'INSUFFICIENT_DATA';
  starsGrowth: number;
  starsGrowthRate: string;
  snapshotCount: number;
  daysTracked: number;
}

interface TrendsResponse {
  owner: string;
  repo: string;
  snapshots: TrendSnapshot[];
  summary?: TrendSummary;
  message?: string;
}

type MetricType = 'healthScore' | 'stars' | 'forks' | 'openIssues' | 'contributors' | 'commits';

/**
 * Trends page — displays historical repository metrics over time.
 */
const TrendsPage: React.FC = () => {
  const [owner, setOwner] = useState('');
  const [repo, setRepo] = useState('');
  const [trendsData, setTrendsData] = useState<TrendsResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedMetric, setSelectedMetric] = useState<MetricType>('healthScore');

  const handleSearch = async () => {
    if (!owner || !repo) {
      setError('Please enter both owner and repository name');
      return;
    }

    setLoading(true);
    setError(null);
    
    try {
      const response = await httpClient.get<TrendsResponse>('/api/trends', {
        params: { owner, repo }
      });
      setTrendsData(response.data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
      setTrendsData(null);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  const getTrendIcon = (trend?: string) => {
    if (!trend) return '📊';
    switch (trend) {
      case 'IMPROVING': return '📈';
      case 'DECLINING': return '📉';
      case 'STABLE': return '➡️';
      default: return '📊';
    }
  };

  const renderChart = (snapshots: TrendSnapshot[], metric: MetricType) => {
    const metricConfig = {
      healthScore: { label: 'Health Score', color: '#3b82f6' },
      stars: { label: 'Stars', color: '#f59e0b' },
      forks: { label: 'Forks', color: '#8b5cf6' },
      openIssues: { label: 'Open Issues', color: '#ef4444' },
      contributors: { label: 'Total Contributors', color: '#10b981' },
      commits: { label: 'Commits (52 weeks)', color: '#06b6d4' },
    };

    const config = metricConfig[metric];

    const chartData = snapshots.map((snapshot) => {
      const date = new Date(snapshot.timestamp);
      return {
        date: date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
        fullDate: date.toLocaleString(),
        healthScore: snapshot.healthScore,
        stars: snapshot.stars,
        forks: snapshot.forks,
        openIssues: snapshot.openIssues,
        contributors: snapshot.totalContributors,
        commits: snapshot.commits52Weeks,
      };
    });

    return (
      <div style={{ width: '100%', height: 400 }}>
        <ResponsiveContainer>
          <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
            <XAxis 
              dataKey="date" 
              stroke="#6b7280"
              style={{ fontSize: '12px' }}
            />
            <YAxis 
              stroke="#6b7280"
              style={{ fontSize: '12px' }}
            />
            <Tooltip 
              contentStyle={{
                backgroundColor: '#ffffff',
                border: '1px solid #e5e7eb',
                borderRadius: '8px',
                padding: '12px',
              }}
              labelFormatter={(label, payload) => {
                if (payload && payload.length > 0) {
                  return (payload[0] as any).payload.fullDate;
                }
                return label;
              }}
            />
            <Legend 
              wrapperStyle={{ paddingTop: '20px' }}
            />
            <Line
              type="monotone"
              dataKey={metric}
              stroke={config.color}
              strokeWidth={2}
              dot={{ fill: config.color, r: 4 }}
              activeDot={{ r: 6 }}
              name={config.label}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    );
  };

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '24px' }}>
      {/* Header */}
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ 
          margin: '0 0 8px', 
          fontSize: '32px', 
          fontWeight: 700, 
          color: '#111827' 
        }}>
          📈 Repository Trends
        </h1>
        <p style={{ 
          color: '#6b7280', 
          fontSize: '16px', 
          margin: 0 
        }}>
          View historical metrics and track repository health over time
        </p>
      </div>

      {/* Search Bar */}
      <div style={{
        display: 'flex',
        gap: '12px',
        marginBottom: '32px',
        alignItems: 'flex-end',
      }}>
        <div style={{ flex: 1 }}>
          <label style={{
            display: 'block',
            fontSize: '14px',
            fontWeight: 500,
            color: '#374151',
            marginBottom: '6px',
          }}>
            Owner
          </label>
          <input
            type="text"
            placeholder="e.g., facebook"
            value={owner}
            onChange={(e) => setOwner(e.target.value)}
            onKeyPress={handleKeyPress}
            style={{
              width: '100%',
              padding: '10px 14px',
              border: '1px solid #d1d5db',
              borderRadius: '8px',
              fontSize: '14px',
            }}
          />
        </div>
        <div style={{ flex: 1 }}>
          <label style={{
            display: 'block',
            fontSize: '14px',
            fontWeight: 500,
            color: '#374151',
            marginBottom: '6px',
          }}>
            Repository
          </label>
          <input
            type="text"
            placeholder="e.g., react"
            value={repo}
            onChange={(e) => setRepo(e.target.value)}
            onKeyPress={handleKeyPress}
            style={{
              width: '100%',
              padding: '10px 14px',
              border: '1px solid #d1d5db',
              borderRadius: '8px',
              fontSize: '14px',
            }}
          />
        </div>
        <button
          onClick={handleSearch}
          disabled={loading}
          style={{
            padding: '10px 24px',
            backgroundColor: loading ? '#9ca3af' : '#3b82f6',
            color: 'white',
            border: 'none',
            borderRadius: '8px',
            fontSize: '14px',
            fontWeight: 500,
            cursor: loading ? 'not-allowed' : 'pointer',
            transition: 'background-color 0.2s',
          }}
        >
          {loading ? 'Loading...' : 'View Trends'}
        </button>
      </div>

      {/* Error Message */}
      {error && (
        <div style={{
          padding: '16px',
          backgroundColor: '#fef2f2',
          border: '1px solid #fecaca',
          borderRadius: '8px',
          color: '#991b1b',
          marginBottom: '24px',
        }}>
          {error}
        </div>
      )}

      {/* Trends Data */}
      {trendsData && (
        <>
          {trendsData.message ? (
            <div style={{
              padding: '48px',
              textAlign: 'center',
              backgroundColor: '#f9fafb',
              borderRadius: '12px',
              border: '1px solid #e5e7eb',
            }}>
              <div style={{ fontSize: '48px', marginBottom: '16px' }}>📊</div>
              <h3 style={{ margin: '0 0 8px', fontSize: '20px', color: '#111827' }}>
                No Historical Data Yet
              </h3>
              <p style={{ color: '#6b7280', margin: 0 }}>
                {trendsData.message}
              </p>
            </div>
          ) : (
            <>
              {/* Summary Cards */}
              {trendsData.summary && (
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                  gap: '16px',
                  marginBottom: '32px',
                }}>
                  <div style={{
                    padding: '20px',
                    backgroundColor: 'white',
                    border: '1px solid #e5e7eb',
                    borderRadius: '12px',
                  }}>
                    <div style={{ fontSize: '14px', color: '#6b7280', marginBottom: '8px' }}>
                      Health Score Trend
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span style={{ fontSize: '28px' }}>
                        {getTrendIcon(trendsData.summary.healthScoreTrend)}
                      </span>
                      <span style={{ fontSize: '24px', fontWeight: 600, color: '#111827' }}>
                        {trendsData.summary.healthScoreChange > 0 ? '+' : ''}
                        {trendsData.summary.healthScoreChange}
                      </span>
                    </div>
                    <div style={{ fontSize: '12px', color: '#6b7280', marginTop: '4px' }}>
                      {trendsData.summary.healthScoreTrend}
                    </div>
                  </div>

                  <div style={{
                    padding: '20px',
                    backgroundColor: 'white',
                    border: '1px solid #e5e7eb',
                    borderRadius: '12px',
                  }}>
                    <div style={{ fontSize: '14px', color: '#6b7280', marginBottom: '8px' }}>
                      Stars Growth
                    </div>
                    <div style={{ fontSize: '24px', fontWeight: 600, color: '#111827' }}>
                      {trendsData.summary.starsGrowth > 0 ? '+' : ''}
                      {trendsData.summary.starsGrowth.toLocaleString()}
                    </div>
                    <div style={{ fontSize: '12px', color: '#6b7280', marginTop: '4px' }}>
                      {trendsData.summary.starsGrowthRate}
                    </div>
                  </div>

                  <div style={{
                    padding: '20px',
                    backgroundColor: 'white',
                    border: '1px solid #e5e7eb',
                    borderRadius: '12px',
                  }}>
                    <div style={{ fontSize: '14px', color: '#6b7280', marginBottom: '8px' }}>
                      Snapshots
                    </div>
                    <div style={{ fontSize: '24px', fontWeight: 600, color: '#111827' }}>
                      {trendsData.summary.snapshotCount}
                    </div>
                    <div style={{ fontSize: '12px', color: '#6b7280', marginTop: '4px' }}>
                      Over {trendsData.summary.daysTracked} days
                    </div>
                  </div>
                </div>
              )}

              {/* Metric Selector */}
              <div style={{ marginBottom: '24px' }}>
                <div style={{
                  display: 'flex',
                  gap: '8px',
                  flexWrap: 'wrap',
                }}>
                  {[
                    { key: 'healthScore' as const, label: 'Health Score' },
                    { key: 'stars' as const, label: 'Stars' },
                    { key: 'forks' as const, label: 'Forks' },
                    { key: 'openIssues' as const, label: 'Open Issues' },
                    { key: 'contributors' as const, label: 'Contributors' },
                    { key: 'commits' as const, label: 'Commits (52w)' },
                  ].map((metric) => (
                    <button
                      key={metric.key}
                      onClick={() => setSelectedMetric(metric.key)}
                      style={{
                        padding: '8px 16px',
                        backgroundColor: selectedMetric === metric.key ? '#3b82f6' : 'white',
                        color: selectedMetric === metric.key ? 'white' : '#374151',
                        border: '1px solid ' + (selectedMetric === metric.key ? '#3b82f6' : '#d1d5db'),
                        borderRadius: '8px',
                        fontSize: '14px',
                        fontWeight: 500,
                        cursor: 'pointer',
                        transition: 'all 0.2s',
                      }}
                    >
                      {metric.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Chart */}
              <div style={{
                padding: '24px',
                backgroundColor: 'white',
                border: '1px solid #e5e7eb',
                borderRadius: '12px',
              }}>
                {renderChart(trendsData.snapshots, selectedMetric)}
              </div>
            </>
          )}
        </>
      )}
    </div>
  );
};

export default TrendsPage;
