import { useTranslation } from 'react-i18next';
import { Check } from 'lucide-react';
import type { Tier } from '../../shared/api/types';
import { formatNumber } from '../../shared/format';

const RADIUS = 30;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

function stageProgress(points: number, tiers: Tier[], index: number): number {
  const threshold = tiers[index].threshold;
  if (points >= threshold) {
    return 1;
  }
  const start = index > 0 ? tiers[index - 1].threshold : 0;
  return Math.max(0, Math.min(1, (points - start) / Math.max(1, threshold - start)));
}

function TierRing({ code, progress, label }: { code: string; progress: number; label: string }) {
  const percent = Math.floor(progress * 100);
  const gradient = `tier-ring-${code.toLowerCase()}`;
  return (
    <div className="tier-ring" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={percent} aria-label={label}>
      <svg viewBox="0 0 72 72" aria-hidden="true">
        <defs>
          <linearGradient id={gradient} x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" className="tier-ring-start" />
            <stop offset="100%" className="tier-ring-end" />
          </linearGradient>
        </defs>
        <circle className="tier-ring-track" cx="36" cy="36" r={RADIUS} />
        <circle
          className="tier-ring-value"
          cx="36"
          cy="36"
          r={RADIUS}
          stroke={`url(#${gradient})`}
          strokeDasharray={CIRCUMFERENCE}
          strokeDashoffset={CIRCUMFERENCE * (1 - progress)}
        />
      </svg>
      <span className="tier-ring-label">{progress >= 1 ? <Check size={22} strokeWidth={2.5} aria-hidden="true" /> : `${percent}%`}</span>
    </div>
  );
}

export function TierTrack({ tiers, currentCode, points }: { tiers: Tier[]; currentCode: string; points: number }) {
  const { t } = useTranslation();
  const ordered = [...tiers].sort((first, second) => first.threshold - second.threshold);
  return (
    <ol className="tier-track">
      {ordered.map((tier, index) => {
        const progress = stageProgress(points, ordered, index);
        const current = tier.code === currentCode;
        return (
          <li key={tier.code} className={`tier-card tier-${tier.code.toLowerCase()}${current ? ' is-current' : ''}`}>
            <TierRing code={tier.code} progress={progress} label={tier.name} />
            <div className="tier-card-body">
              <div className="tier-card-head">
                <strong className="tier-name">{tier.name}</strong>
                {current && <span className="badge tier-badge">{t('loyalty.currentTier')}</span>}
              </div>
              <span className="caption muted">
                {t('loyalty.tierThreshold', { points: formatNumber(tier.threshold) })} · {t('loyalty.multiplier', { value: tier.multiplier })}
              </span>
              {progress < 1 && <span className="caption tier-remaining">{t('loyalty.pointsToGo', { points: formatNumber(tier.threshold - points) })}</span>}
              {tier.benefits.length > 0 && (
                <ul className="tier-benefits">
                  {tier.benefits.map((benefit) => (
                    <li key={benefit}>{benefit}</li>
                  ))}
                </ul>
              )}
            </div>
          </li>
        );
      })}
    </ol>
  );
}
