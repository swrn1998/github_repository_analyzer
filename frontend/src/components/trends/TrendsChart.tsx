import React from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { TrendSnapshot } from '../../types/trends';

interface TrendsChartProps {
  snapshots: TrendSnapshot[];
  metric: 'healthScore' | 'stars' | 'forks' | 'openIssues' | 'contributors' | 'commits';
}

const TrendsChart: React.FC<TrendsChartProps> = ({ snapshots, metric }) => {
  const metricConfig = {
    healthScore: { label: 'Health Score', color: '#3b82f6', unit: '' },
    stars: { label: 'Stars', color: '#f59e0b', unit: '' },
    forks: { label: 'Forks', color: '#8b5cf6', unit: '' },
    openIssues: { label: 'Open Issues', color: '#ef4444', unit: '' },
    contributors: { label: 'Total Contributors', color: '#10b981', unit: '' },
    commits: { label: 'Commits (52 weeks)', color: '#06b6d4', unit: '' },
  };

  const config = metricConfig[metric];

  // Transform snapshots to chart data
  const chartData = snapshots.map((snapshot) => {
    const date = new Date(snapshot.timestamp);
    return {
      date: date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
      fullDate: date.toLocaleString(),
      healthScore: snapshot.healthScore,
      stars: snapshot.stars,
      forks: snapshot.forks,
      openIssues: snapshot.openIssues,
      contributors: snapshot.totalContributors,
      commits: snapshot.commits52Weeks,
    };
  });

  return (
    <div style={{ width: '100%', height: 400 }}>
      <ResponsiveContainer>
        <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
          <XAxis 
            dataKey="date" 
            stroke="#6b7280"
            style={{ fontSize: '12px' }}
          />
          <YAxis 
            stroke="#6b7280"
            style={{ fontSize: '12px' }}
          />
          <Tooltip 
            contentStyle={{
              backgroundColor: '#ffffff',
              border: '1px solid #e5e7eb',
              borderRadius: '8px',
              padding: '12px',
            }}
            labelFormatter={(label, payload) => {
              if (payload && payload.length > 0) {
                return payload[0].payload.fullDate;
              }
              return label;
            }}
          />
          <Legend 
            wrapperStyle={{ paddingTop: '20px' }}
          />
          <Line
            type="monotone"
            dataKey={metric}
            stroke={config.color}
            strokeWidth={2}
            dot={{ fill: config.color, r: 4 }}
            activeDot={{ r: 6 }}
            name={config.label}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

export default TrendsChart;
