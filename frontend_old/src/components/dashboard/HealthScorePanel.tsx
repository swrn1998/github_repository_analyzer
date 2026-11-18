import React from 'react';
import { HealthScore } from '../../types/analysis';
import { gradeToColor, gradeToLabel, scoreToColor } from '../../utils/scoreHelpers';
import {
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  Radar,
  ResponsiveContainer,
  Tooltip,
} from 'recharts';

interface HealthScorePanelProps {
  healthScore: HealthScore;
}

/**
 * Displays the health score as a circular gauge with letter grade,
 * plus a radar chart showing the four score dimensions.
 */
const HealthScorePanel: React.FC<HealthScorePanelProps> = ({ healthScore }) => {
  const { score, grade, breakdown } = healthScore;
  const color = gradeToColor(grade);

  const radarData = [
    { dimension: 'Commit Activity', score: breakdown.commitActivityScore, fullMark: 25 },
    { dimension: 'Issue Ratio', score: breakdown.issueRatioScore, fullMark: 25 },
    { dimension: 'Community', score: breakdown.communityScore, fullMark: 25 },
    { dimension: 'Documentation', score: breakdown.documentationScore, fullMark: 25 },
  ];

  return (
    <div
      style={{
        backgroundColor: '#ffffff',
        border: '1px solid #e5e7eb',
        borderRadius: '12px',
        padding: '24px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
      }}
    >
      <h3 style={{ margin: '0 0 20px', fontSize: '16px', fontWeight: 600, color: '#374151' }}>
        Health Score
      </h3>

      <div style={{ display: 'flex', alignItems: 'center', gap: '32px', flexWrap: 'wrap' }}>

        {/* Circular Score Display */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
          <div
            aria-label={`Health score: ${score} out of 100`}
            style={{
              width: '120px',
              height: '120px',
              borderRadius: '50%',
              background: `conic-gradient(${color} ${score}%, #f3f4f6 0%)`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: `0 0 0 8px ${color}20`,
            }}
          >
            <div
              style={{
                width: '90px',
                height: '90px',
                borderRadius: '50%',
                backgroundColor: 'white',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <span style={{ fontSize: '26px', fontWeight: 700, color }}>{score}</span>
              <span style={{ fontSize: '11px', color: '#9ca3af' }}>/ 100</span>
            </div>
          </div>

          <div
            style={{
              padding: '4px 12px',
              borderRadius: '20px',
              backgroundColor: `${color}20`,
              color,
              fontWeight: 700,
              fontSize: '18px',
            }}
          >
            {grade} — {gradeToLabel(grade)}
          </div>
        </div>

        {/* Radar Chart */}
        <div style={{ flex: 1, minWidth: '240px', height: '200px' }}>
          <ResponsiveContainer width="100%" height="100%">
            <RadarChart data={radarData}>
              <PolarGrid stroke="#e5e7eb" />
              <PolarAngleAxis
                dataKey="dimension"
                tick={{ fontSize: 11, fill: '#6b7280' }}
              />
              <Radar
                name="Score"
                dataKey="score"
                stroke={color}
                fill={color}
                fillOpacity={0.25}
              />
              <Tooltip formatter={(value) => [`${value} / 25`, 'Score']} />
            </RadarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Score breakdown table */}
      <div style={{ marginTop: '20px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
        {radarData.map((d) => (
          <div
            key={d.dimension}
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              padding: '6px 10px',
              backgroundColor: '#f9fafb',
              borderRadius: '6px',
              fontSize: '13px',
            }}
          >
            <span style={{ color: '#4b5563' }}>{d.dimension}</span>
            <span style={{ fontWeight: 600, color }}>
              {d.score} <span style={{ color: '#9ca3af', fontWeight: 400 }}>/ 25</span>
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default HealthScorePanel;
