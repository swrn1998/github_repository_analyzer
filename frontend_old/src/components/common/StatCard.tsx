import React from 'react';
import { formatNumber } from '../../utils/formatters';

interface StatCardProps {
  label: string;
  value: number | string;
  icon: string;
  color?: string;
  subtitle?: string;
  formatValue?: boolean;
}

/**
 * Displays a single repository statistic (stars, forks, watchers, issues).
 *
 * The value is automatically formatted with K/M suffixes for large numbers.
 */
const StatCard: React.FC<StatCardProps> = ({
  label,
  value,
  icon,
  color = '#3b82f6',
  subtitle,
  formatValue = true,
}) => {
  const displayValue =
    typeof value === 'number' && formatValue ? formatNumber(value) : String(value);

  return (
    <div
      style={{
        backgroundColor: '#ffffff',
        border: '1px solid #e5e7eb',
        borderRadius: '12px',
        padding: '20px',
        display: 'flex',
        flexDirection: 'column',
        gap: '8px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <span style={{ fontSize: '20px' }}>{icon}</span>
        <span style={{ fontSize: '13px', color: '#6b7280', fontWeight: 500 }}>{label}</span>
      </div>
      <div style={{ fontSize: '28px', fontWeight: 700, color }}>
        {displayValue}
      </div>
      {subtitle && (
        <div style={{ fontSize: '12px', color: '#9ca3af' }}>{subtitle}</div>
      )}
    </div>
  );
};

export default StatCard;
