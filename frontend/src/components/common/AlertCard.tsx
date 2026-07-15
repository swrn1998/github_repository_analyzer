import React from 'react';
import { Alert, AlertType } from '../../types/analysis';
import { severityToColor } from '../../utils/scoreHelpers';

interface AlertCardProps {
  alert: Alert;
}

const ALERT_ICONS: Record<AlertType, string> = {
  INACTIVE_REPO: '💤',
  HIGH_ISSUE_BACKLOG: '📋',
  NO_TESTS: '🧪',
  SECURITY_VULNERABILITIES: '🔒',
};

/**
 * Displays a single repository health alert with appropriate color coding.
 *
 * Uses a colored left border to visually communicate severity:
 * - INFO:     blue
 * - WARNING:  yellow
 * - CRITICAL: red
 */
const AlertCard: React.FC<AlertCardProps> = ({ alert }) => {
  const color = severityToColor(alert.severity);
  const icon = ALERT_ICONS[alert.type] || '⚠️';

  return (
    <div
      role="alert"
      style={{
        display: 'flex',
        alignItems: 'flex-start',
        gap: '12px',
        padding: '12px 16px',
        borderRadius: '8px',
        borderLeft: `4px solid ${color}`,
        backgroundColor: `${color}10`,
        border: `1px solid ${color}30`,
        borderLeftWidth: '4px',
      }}
    >
      <span style={{ fontSize: '18px', flexShrink: 0 }}>{icon}</span>
      <div>
        <div style={{ fontWeight: 600, color: '#1f2937', fontSize: '14px' }}>
          {alert.message}
        </div>
        <div style={{ fontSize: '12px', color: '#6b7280', marginTop: '2px' }}>
          {alert.severity} · {alert.type.replace(/_/g, ' ')}
        </div>
      </div>
    </div>
  );
};

export default AlertCard;
