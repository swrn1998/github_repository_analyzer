import React from 'react';
import { LanguageDistribution } from '../../types/analysis';
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';

interface LanguageDistributionPanelProps {
  languages: LanguageDistribution;
}

const COLORS = ['#3b82f6', '#8b5cf6', '#ec4899', '#f59e0b', '#10b981', '#06b6d4', '#f97316'];

/**
 * Displays language distribution as a pie chart with a legend.
 */
const LanguageDistributionPanel: React.FC<LanguageDistributionPanelProps> = ({ languages }) => {
  const { percentages, primaryLanguage } = languages;

  const chartData = Object.entries(percentages)
    .sort(([, a], [, b]) => b - a)
    .map(([name, percent]) => ({ name, value: percent }));

  if (chartData.length === 0) {
    return (
      <div style={{ ...panelStyle }}>
        <h3 style={headingStyle}>Languages</h3>
        <p style={{ color: '#9ca3af', fontSize: '14px' }}>No language data available</p>
      </div>
    );
  }

  return (
    <div style={panelStyle}>
      <h3 style={headingStyle}>
        Languages
        <span style={{ fontSize: '13px', color: '#6b7280', fontWeight: 400, marginLeft: '8px' }}>
          Primary: {primaryLanguage}
        </span>
      </h3>

      <div style={{ height: '220px' }}>
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={chartData}
              cx="50%"
              cy="50%"
              innerRadius={55}
              outerRadius={90}
              paddingAngle={2}
              dataKey="value"
              aria-label="Language distribution pie chart"
            >
              {chartData.map((_, index) => (
                <Cell key={index} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip formatter={(value: number) => [`${value.toFixed(1)}%`]} />
            <Legend
              formatter={(value) => (
                <span style={{ fontSize: '12px', color: '#374151' }}>{value}</span>
              )}
            />
          </PieChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};

const panelStyle: React.CSSProperties = {
  backgroundColor: '#ffffff',
  border: '1px solid #e5e7eb',
  borderRadius: '12px',
  padding: '24px',
  boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
};

const headingStyle: React.CSSProperties = {
  margin: '0 0 16px',
  fontSize: '16px',
  fontWeight: 600,
  color: '#374151',
};

export default LanguageDistributionPanel;
