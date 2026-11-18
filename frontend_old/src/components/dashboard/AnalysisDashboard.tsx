import React from 'react';
import { AnalysisResponse } from '../../types/analysis';
import StatCard from '../common/StatCard';
import AlertCard from '../common/AlertCard';
import DataSourceBanner from '../common/DataSourceBanner';
import HealthScorePanel from './HealthScorePanel';
import LanguageDistributionPanel from './LanguageDistributionPanel';
import { formatNumber, formatRelativeTime } from '../../utils/formatters';

interface AnalysisDashboardProps {
  data: AnalysisResponse;
}

/**
 * Main analysis dashboard — assembles all panels for a single repository analysis.
 * Receives the full AnalysisResponse and delegates rendering to sub-panels.
 */
const AnalysisDashboard: React.FC<AnalysisDashboardProps> = ({ data }) => {
  const { stats, healthScore, alerts, languages, contributorActivity, source, cachedAt } = data;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>

      {/* Repo title */}
      <div>
        <h2 style={{ margin: '0 0 4px', fontSize: '22px', fontWeight: 700, color: '#111827' }}>
          {data.owner}/{data.repo}
        </h2>
        {stats.description && (
          <p style={{ margin: 0, color: '#6b7280', fontSize: '14px' }}>{stats.description}</p>
        )}
      </div>

      {/* Data freshness banner */}
      <DataSourceBanner source={source} cachedAt={cachedAt} notice={data.notice} />

      {/* Stats grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
        gap: '12px',
      }}>
        <StatCard label="Stars" value={stats.stars} icon="⭐" color="#f59e0b" />
        <StatCard label="Forks" value={stats.forks} icon="🍴" color="#8b5cf6" />
        <StatCard label="Watchers" value={stats.watchers} icon="👁" color="#06b6d4" />
        <StatCard
          label="Open Issues"
          value={stats.openIssues}
          icon="🐛"
          color={stats.openIssues > 50 ? '#ef4444' : '#10b981'}
        />
      </div>

      {/* Health score + Language distribution */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
        gap: '20px',
      }}>
        <HealthScorePanel healthScore={healthScore} />
        <LanguageDistributionPanel languages={languages} />
      </div>

      {/* Alerts panel */}
      {alerts.length > 0 && (
        <div style={{
          backgroundColor: '#ffffff',
          border: '1px solid #e5e7eb',
          borderRadius: '12px',
          padding: '24px',
          boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
        }}>
          <h3 style={{ margin: '0 0 16px', fontSize: '16px', fontWeight: 600, color: '#374151' }}>
            Alerts ({alerts.length})
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {alerts.map((alert, i) => (
              <AlertCard key={i} alert={alert} />
            ))}
          </div>
        </div>
      )}

      {/* Contributor activity */}
      <div style={{
        backgroundColor: '#ffffff',
        border: '1px solid #e5e7eb',
        borderRadius: '12px',
        padding: '24px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
      }}>
        <h3 style={{ margin: '0 0 16px', fontSize: '16px', fontWeight: 600, color: '#374151' }}>
          Contributor Activity
        </h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: '16px', marginBottom: '20px' }}>
          <div>
            <div style={{ fontSize: '24px', fontWeight: 700, color: '#111827' }}>
              {formatNumber(contributorActivity.totalContributors)}
            </div>
            <div style={{ fontSize: '12px', color: '#6b7280' }}>Total Contributors</div>
          </div>
          <div>
            <div style={{ fontSize: '24px', fontWeight: 700, color: '#111827' }}>
              {formatNumber(contributorActivity.totalCommitsLast52Weeks)}
            </div>
            <div style={{ fontSize: '12px', color: '#6b7280' }}>Commits (52 weeks)</div>
          </div>
          <div>
            <div style={{ fontSize: '16px', fontWeight: 600, color: '#111827' }}>
              {formatRelativeTime(contributorActivity.lastCommitDate)}
            </div>
            <div style={{ fontSize: '12px', color: '#6b7280' }}>Last Commit</div>
          </div>
        </div>

        {contributorActivity.topContributors.length > 0 && (
          <div>
            <h4 style={{ margin: '0 0 10px', fontSize: '13px', fontWeight: 600, color: '#6b7280', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Top Contributors
            </h4>
            {contributorActivity.topContributors.map((c, i) => (
              <div key={c.login} style={{
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                padding: '8px 0', borderTop: i > 0 ? '1px solid #f3f4f6' : 'none',
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{ color: '#9ca3af', fontSize: '13px', width: '20px' }}>#{i + 1}</span>
                  <a
                    href={`https://github.com/${c.login}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    style={{ color: '#2563eb', textDecoration: 'none', fontWeight: 500 }}
                  >
                    {c.login}
                  </a>
                </div>
                <div style={{ fontSize: '13px', color: '#6b7280' }}>
                  {formatNumber(c.totalCommits)} commits · {c.weeksActive} active weeks
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Repo metadata footer */}
      <div style={{ fontSize: '12px', color: '#9ca3af', display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
        <span>Branch: <strong>{stats.defaultBranch}</strong></span>
        {stats.license && <span>License: <strong>{stats.license}</strong></span>}
        <span>Created: <strong>{new Date(stats.createdAt).getFullYear()}</strong></span>
        <span>Last updated: <strong>{formatRelativeTime(stats.updatedAt)}</strong></span>
      </div>
    </div>
  );
};

export default AnalysisDashboard;
