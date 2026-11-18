import React from 'react';
import { DataSource } from '../../types/analysis';
import { dataSourceMeta } from '../../utils/scoreHelpers';
import { formatRelativeTime } from '../../utils/formatters';

interface DataSourceBannerProps {
  source: DataSource;
  cachedAt?: string | null;
  notice?: string | null;
}

/**
 * Banner that communicates data freshness to the user.
 *
 * Shows different messaging and colors based on whether the data is
 * LIVE, CACHE, STALE, or MOCK — so users know how much to trust the numbers.
 */
const DataSourceBanner: React.FC<DataSourceBannerProps> = ({ source, cachedAt, notice }) => {
  const meta = dataSourceMeta(source);

  const shouldShowCachedTime = (source === 'CACHE' || source === 'STALE') && cachedAt;

  return (
    <div
      role="status"
      aria-live="polite"
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '4px',
        padding: '8px 14px',
        borderRadius: '6px',
        backgroundColor: `${meta.color}18`,
        border: `1px solid ${meta.color}40`,
        fontSize: '14px',
        color: '#374151',
        marginBottom: '16px',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <span style={{ color: meta.color, fontSize: '16px' }}>{meta.icon}</span>
        <span style={{ fontWeight: 600, color: meta.color }}>{meta.label}</span>
        <span>—</span>
        <span>{meta.description}</span>
        {shouldShowCachedTime && (
          <span style={{ color: '#6b7280', marginLeft: '4px' }}>
            (from {formatRelativeTime(cachedAt!)})
          </span>
        )}
      </div>
      {notice && (
        <div style={{ color: '#6b7280', fontSize: '13px' }}>
          {notice}
        </div>
      )}
    </div>
  );
};

export default DataSourceBanner;
