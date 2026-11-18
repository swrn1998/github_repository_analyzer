import React from 'react';
import { dataSourceMeta } from '../../utils/scoreHelpers';
import { formatRelativeTime } from '../../utils/formatters';
import styles from './DataSourceBanner.module.css';

function DataSourceBanner({ source, cachedAt, notice }) {
  const meta = dataSourceMeta(source);
  const showCacheTime = (source === 'CACHE' || source === 'STALE') && cachedAt;

  return (
    <div
      role="status"
      aria-live="polite"
      className={styles.banner}
      style={{
        backgroundColor: `${meta.color}15`,
        border: `1px solid ${meta.color}40`,
      }}
    >
      <div className={styles.row}>
        <span className={styles.icon} style={{ color: meta.color }}>{meta.icon}</span>
        <span className={styles.label} style={{ color: meta.color }}>{meta.label}</span>
        <span className={styles.separator}>—</span>
        <span className={styles.description}>{meta.description}</span>
        {showCacheTime && (
          <span className={styles.cacheTime}>(cached {formatRelativeTime(cachedAt)})</span>
        )}
      </div>
      {notice && <div className={styles.notice}>{notice}</div>}
    </div>
  );
}

export default DataSourceBanner;
