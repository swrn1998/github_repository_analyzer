import React from 'react';
import { formatNumber } from '../../utils/formatters';
import styles from './StatCard.module.css';

function StatCard({ label, value, icon, color = '#3b82f6', subtitle, formatValue = true }) {
  const displayValue = typeof value === 'number' && formatValue ? formatNumber(value) : String(value);

  return (
    <div className={styles.card}>
      <div className={styles.labelRow}>
        <span className={styles.icon}>{icon}</span>
        <span className={styles.label}>{label}</span>
      </div>
      <div className={styles.value} style={{ color }}>{displayValue}</div>
      {subtitle && <div className={styles.subtitle}>{subtitle}</div>}
    </div>
  );
}

export default StatCard;
