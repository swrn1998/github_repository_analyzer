import React from 'react';
import { severityToColor } from '../../utils/scoreHelpers';
import styles from './AlertCard.module.css';

const ALERT_ICONS = {
  INACTIVE_REPO: '💤',
  HIGH_ISSUE_BACKLOG: '📋',
  NO_TESTS: '🧪',
  SECURITY_VULNERABILITIES: '🔒',
};

function AlertCard({ alert }) {
  const color = severityToColor(alert.severity);
  const icon = ALERT_ICONS[alert.type] || '⚠️';

  return (
    <div
      role="alert"
      className={styles.card}
      style={{
        borderLeftColor: color,
        backgroundColor: `${color}12`,
        border: `1px solid ${color}30`,
        borderLeftWidth: '4px',
      }}
    >
      <span className={styles.icon}>{icon}</span>
      <div>
        <div className={styles.message}>{alert.message}</div>
        <div className={styles.meta}>
          {alert.severity} · {alert.type.replace(/_/g, ' ')}
        </div>
      </div>
    </div>
  );
}

export default AlertCard;
