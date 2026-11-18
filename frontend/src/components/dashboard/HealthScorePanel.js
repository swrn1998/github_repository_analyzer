import React from 'react';
import { gradeToColor, gradeToLabel } from '../../utils/scoreHelpers';
import { RadarChart, PolarGrid, PolarAngleAxis, Radar, ResponsiveContainer, Tooltip } from 'recharts';
import styles from './HealthScorePanel.module.css';

function HealthScorePanel({ healthScore }) {
  const { score, grade, breakdown } = healthScore;
  const color = gradeToColor(grade);

  const radarData = [
    { dimension: 'Commits',   score: breakdown.commitActivityScore, fullMark: 25 },
    { dimension: 'Issues',    score: breakdown.issueRatioScore,     fullMark: 25 },
    { dimension: 'Community', score: breakdown.communityScore,      fullMark: 25 },
    { dimension: 'Docs',      score: breakdown.documentationScore,  fullMark: 25 },
  ];

  return (
    <div className={styles.panel}>
      <h3 className={styles.heading}>Health Score</h3>

      <div className={styles.body}>
        <div className={styles.gaugeWrapper}>
          <div
            aria-label={`Health score ${score} out of 100`}
            className={styles.gauge}
            style={{
              background: `conic-gradient(${color} ${score}%, #f3f4f6 0%)`,
              boxShadow: `0 0 0 8px ${color}20`,
            }}
          >
            <div className={styles.gaugeInner}>
              <span className={styles.gaugeScore} style={{ color }}>{score}</span>
              <span className={styles.gaugeMax}>/ 100</span>
            </div>
          </div>

          <div
            className={styles.gradeBadge}
            style={{ backgroundColor: `${color}20`, color }}
          >
            {grade} — {gradeToLabel(grade)}
          </div>
        </div>

        <div className={styles.radarWrapper}>
          <ResponsiveContainer width="100%" height="100%">
            <RadarChart data={radarData}>
              <PolarGrid stroke="#e5e7eb" />
              <PolarAngleAxis dataKey="dimension" tick={{ fontSize: 11, fill: '#6b7280' }} />
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

      <div className={styles.breakdown}>
        {radarData.map((d) => (
          <div key={d.dimension} className={styles.breakdownItem}>
            <span className={styles.breakdownLabel}>{d.dimension}</span>
            <span className={styles.breakdownScore} style={{ color }}>
              {d.score} <span className={styles.breakdownMax}>/ 25</span>
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

export default HealthScorePanel;
