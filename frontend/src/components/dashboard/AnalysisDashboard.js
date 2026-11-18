import React from 'react';
import StatCard from '../common/StatCard';
import AlertCard from '../common/AlertCard';
import DataSourceBanner from '../common/DataSourceBanner';
import HealthScorePanel from './HealthScorePanel';
import LanguageDistributionPanel from './LanguageDistributionPanel';
import { formatNumber, formatRelativeTime } from '../../utils/formatters';
import styles from './AnalysisDashboard.module.css';

function AnalysisDashboard({ data }) {
  const { stats, healthScore, alerts, languages, contributorActivity, source, cachedAt } = data;

  return (
    <div className={styles.dashboard}>
      <div>
        <h2 className={styles.repoTitle}>{data.owner}/{data.repo}</h2>
        {stats.description && (
          <p className={styles.repoDescription}>{stats.description}</p>
        )}
      </div>

      <DataSourceBanner source={source} cachedAt={cachedAt} notice={data.notice} />

      <div className={styles.statsGrid}>
        <StatCard label="Stars"       value={stats.stars}      icon="⭐" color="#f59e0b" />
        <StatCard label="Forks"       value={stats.forks}      icon="🍴" color="#8b5cf6" />
        <StatCard label="Watchers"    value={stats.watchers}   icon="👁" color="#06b6d4" />
        <StatCard
          label="Open Issues"
          value={stats.openIssues}
          icon="🐛"
          color={stats.openIssues > 50 ? '#ef4444' : '#10b981'}
        />
      </div>

      <div className={styles.panelsGrid}>
        <HealthScorePanel healthScore={healthScore} />
        <LanguageDistributionPanel languages={languages} />
      </div>

      {alerts.length > 0 && (
        <div className={styles.card}>
          <h3 className={styles.cardHeading}>Alerts ({alerts.length})</h3>
          <div className={styles.alertList}>
            {alerts.map((alert, i) => (
              <AlertCard key={i} alert={alert} />
            ))}
          </div>
        </div>
      )}

      <div className={styles.card}>
        <h3 className={styles.cardHeading}>Contributor Activity</h3>
        <div className={styles.contributorStats}>
          <div>
            <div className={styles.statNum}>{formatNumber(contributorActivity.totalContributors)}</div>
            <div className={styles.statLabel}>Total Contributors</div>
          </div>
          <div>
            <div className={styles.statNum}>{formatNumber(contributorActivity.totalCommitsLast52Weeks)}</div>
            <div className={styles.statLabel}>Commits (52 weeks)</div>
          </div>
          <div>
            <div className={styles.statVal}>{formatRelativeTime(contributorActivity.lastCommitDate)}</div>
            <div className={styles.statLabel}>Last Commit</div>
          </div>
        </div>

        {contributorActivity.topContributors.length > 0 && (
          <div>
            <h4 className={styles.contributorsHeading}>Top Contributors</h4>
            {contributorActivity.topContributors.map((c, i) => (
              <div key={c.login} className={styles.contributorRow}>
                <div className={styles.contributorLeft}>
                  <span className={styles.contributorRank}>#{i + 1}</span>
                  <a
                    href={`https://github.com/${c.login}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className={styles.contributorLink}
                  >
                    {c.login}
                  </a>
                </div>
                <span className={styles.contributorMeta}>
                  {formatNumber(c.totalCommits)} commits · {c.weeksActive}w active
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className={styles.footer}>
        <span>Branch: <strong>{stats.defaultBranch}</strong></span>
        {stats.license && <span>License: <strong>{stats.license}</strong></span>}
        <span>Created: <strong>{new Date(stats.createdAt).getFullYear()}</strong></span>
        <span>Updated: <strong>{formatRelativeTime(stats.updatedAt)}</strong></span>
      </div>
    </div>
  );
}

export default AnalysisDashboard;
