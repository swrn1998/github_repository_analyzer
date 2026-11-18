import React from 'react';
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import styles from './LanguageDistributionPanel.module.css';

const COLORS = ['#3b82f6', '#8b5cf6', '#ec4899', '#f59e0b', '#10b981', '#06b6d4', '#f97316'];

function LanguageDistributionPanel({ languages }) {
  const { percentages, primaryLanguage } = languages;

  const chartData = Object.entries(percentages)
    .sort(([, a], [, b]) => b - a)
    .map(([name, value]) => ({ name, value }));

  if (chartData.length === 0) {
    return (
      <div className={styles.panel}>
        <h3 className={styles.heading}>Languages</h3>
        <p className={styles.empty}>No language data available</p>
      </div>
    );
  }

  return (
    <div className={styles.panel}>
      <h3 className={styles.heading}>
        Languages{' '}
        <span className={styles.primaryLang}>— primary: {primaryLanguage}</span>
      </h3>
      <div className={styles.chartWrapper}>
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
            >
              {chartData.map((_, i) => (
                <Cell key={i} fill={COLORS[i % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip formatter={(value) => [`${value.toFixed(1)}%`]} />
            <Legend
              formatter={(value) => (
                <span className={styles.legendLabel}>{value}</span>
              )}
            />
          </PieChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

export default LanguageDistributionPanel;
