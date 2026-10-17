import { Link } from 'react-router';
import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Award, Check, Crown, Gift, Users } from 'lucide-react';
import { accountApi } from '../../shared/api/endpoints';
import type { LoyaltySummary, LoyaltyTask } from '../../shared/api/types';
import { formatDate, formatMoney, formatNumber } from '../../shared/format';
import { ErrorState, Skeleton } from '../../design-system/components/States';
import { CopyButton } from '../../design-system/components/CopyButton';
import { PageLoader } from '../../app/PageLoader';

function Progress({ value, label }: { value: number; label: string }) {
  const percent = Math.max(0, Math.min(100, value));
  return (
    <div className="progress" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(percent)} aria-label={label}>
      <span style={{ width: `${percent}%` }} />
    </div>
  );
}

function FreeMember({ summary }: { summary: LoyaltySummary }) {
  const { t } = useTranslation();
  const spend = summary.qualifyingSpend;
  const percent = (Number(spend.spent.amount) / Math.max(1, Number(spend.threshold.amount))) * 100;
  return (
    <section className="feature-band">
      <div className="stack">
        <span className="badge badge-plus" style={{ alignSelf: 'flex-start' }}>
          <Crown size={14} aria-hidden="true" /> {t('common.plus')}
        </span>
        <h2 className="section-title">{t('loyalty.freeTitle')}</h2>
        <p className="muted">{t('loyalty.freeText', { threshold: formatMoney(spend.threshold), days: spend.windowDays })}</p>
      </div>
      <div className="card stack">
        <div className="row spread">
          <span className="small muted">{t('loyalty.qualifyingSpend')}</span>
          <strong>
            {formatMoney(spend.spent)} / {formatMoney(spend.threshold)}
          </strong>
        </div>
        <Progress value={percent} label={t('loyalty.qualifyingSpend')} />
        <p className="small">{t('loyalty.spendProgress', { remaining: formatMoney(spend.remaining) })}</p>
        <p className="caption tertiary">{t('loyalty.spendNote')}</p>
        <Link to="/explore" className="btn btn-primary">
          {t('cart.browse')}
        </Link>
      </div>
    </section>
  );
}

function TaskCard({ task }: { task: LoyaltyTask }) {
  const { t } = useTranslation();
  const done = task.status === 'COMPLETED' || task.awarded;
  return (
    <li className="card stack" style={{ gap: 10 }}>
      <div className="row spread" style={{ alignItems: 'flex-start' }}>
        <div className="stack" style={{ gap: 4 }}>
          <strong>{task.title}</strong>
          <span className="small muted">{task.description}</span>
        </div>
        <span className={`badge ${done ? 'badge-success' : 'badge-plus'}`}>
          {done ? <Check size={14} aria-hidden="true" /> : null}
          {done ? t('loyalty.completed') : t('loyalty.reward', { points: task.rewardPoints })}
        </span>
      </div>
      <Progress value={(task.progress / Math.max(1, task.target)) * 100} label={task.title} />
      <div className="row spread caption muted">
        <span>{t('loyalty.inProgress', { progress: Math.min(task.progress, task.target), target: task.target })}</span>
        {task.endsAt && <span>{t('offer.endsOn', { date: formatDate(task.endsAt) })}</span>}
      </div>
      <details className="caption muted">
        <summary style={{ cursor: 'pointer' }}>{t('loyalty.termsLabel')}</summary>
        <p style={{ marginTop: 6 }}>{task.terms}</p>
      </details>
    </li>
  );
}

function Ledger() {
  const { t } = useTranslation();
  const ledger = useInfiniteQuery({
    queryKey: ['loyalty', 'ledger'],
    queryFn: ({ pageParam }) => accountApi.ledger(pageParam),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
  });
  const entries = ledger.data?.pages.flatMap((page) => page.items) ?? [];
  return (
    <section className="section">
      <h2 className="section-title" style={{ marginBottom: 12 }}>
        {t('loyalty.history')}
      </h2>
      {ledger.isLoading && <Skeleton height={120} radius={16} />}
      {ledger.data && entries.length === 0 && <p className="muted small">{t('loyalty.emptyHistory')}</p>}
      {entries.length > 0 && (
        <ul className="card" style={{ listStyle: 'none', margin: 0, padding: 0 }}>
          {entries.map((entry) => (
            <li key={entry.id} className="list-row">
              <div className="stack" style={{ gap: 2, flex: 1 }}>
                <span>{t(`loyalty.${entry.descriptionKey}`, { ...entry.params, defaultValue: entry.type })}</span>
                <span className="caption tertiary">{formatDate(entry.createdAt)}</span>
              </div>
              <strong style={{ color: entry.points >= 0 ? 'var(--color-success)' : 'var(--color-danger)' }}>
                {entry.points > 0 ? '+' : ''}
                {formatNumber(entry.points)}
              </strong>
            </li>
          ))}
        </ul>
      )}
      {ledger.hasNextPage && (
        <button type="button" className="btn btn-secondary" style={{ marginTop: 12 }} onClick={() => ledger.fetchNextPage()} disabled={ledger.isFetchingNextPage}>
          {t('common.loadMore')}
        </button>
      )}
    </section>
  );
}

function Referral() {
  const { t } = useTranslation();
  const referral = useQuery({ queryKey: ['loyalty', 'referral'], queryFn: accountApi.referral });
  if (!referral.data) {
    return null;
  }
  const link = `${window.location.origin}/auth?ref=${referral.data.code}`;
  return (
    <section className="section card stack">
      <h2 className="section-title row" style={{ gap: 8 }}>
        <Users size={20} aria-hidden="true" />
        {t('loyalty.referral')}
      </h2>
      <p className="small muted">{t('loyalty.referralText')}</p>
      <div className="voucher">
        <div className="row spread wrap">
          <span className="voucher-code">{referral.data.code}</span>
          <CopyButton value={link} label={t('loyalty.referral')} />
        </div>
      </div>
      {referral.data.referrals.length > 0 && (
        <ul className="stack" style={{ listStyle: 'none', margin: 0, padding: 0, gap: 6 }}>
          {referral.data.referrals.map((item, index) => (
            <li key={index} className="kv">
              <span className="muted">{formatDate(item.createdAt)}</span>
              <span className="badge badge-soft">{t(`loyalty.referralStatuses.${item.status}`, { defaultValue: item.status })}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function PlusMember({ summary }: { summary: LoyaltySummary }) {
  const { t } = useTranslation();
  const tasks = useQuery({ queryKey: ['loyalty', 'tasks'], queryFn: accountApi.tasks });
  return (
    <>
      {summary.redemptionBlocked && <p className="alert alert-warning">{t('loyalty.redemptionBlocked')}</p>}
      <div className="stat-grid">
        <div className="stat">
          <div className="caption muted">{t('loyalty.available')}</div>
          <div className="stat-value">{formatNumber(summary.availablePoints)}</div>
        </div>
        <div className="stat">
          <div className="caption muted">{t('loyalty.earned')}</div>
          <div className="stat-value">{formatNumber(summary.lifetimeEarnedPoints)}</div>
        </div>
        <div className="stat">
          <div className="caption muted">{t('loyalty.redeemed')}</div>
          <div className="stat-value">{formatNumber(summary.redeemedPoints)}</div>
        </div>
      </div>
      <section className="section card stack">
        <div className="row spread wrap">
          <h2 className="section-title row" style={{ gap: 8 }}>
            <Award size={20} aria-hidden="true" />
            {t('loyalty.tier')}: {summary.tier.name}
          </h2>
          <span className="badge badge-plus">{t('loyalty.multiplier', { value: summary.tier.multiplier })}</span>
        </div>
        <Progress value={summary.tierProgressPercent} label={t('loyalty.tier')} />
        <p className="small muted">
          {summary.nextTier ? t('loyalty.nextTier', { points: summary.nextTier.pointsNeeded, tier: summary.nextTier.name }) : t('loyalty.maxTier')}
        </p>
        {summary.membershipEndsAt && <p className="caption tertiary">{t('loyalty.membershipEnds', { date: formatDate(summary.membershipEndsAt) })}</p>}
        <div className="steps">
          {summary.tiers.map((tier) => (
            <div key={tier.code} className="stat" style={tier.code === summary.tier.code ? { borderColor: 'var(--color-accent)' } : undefined}>
              <div className="row spread">
                <strong>{tier.name}</strong>
                <span className="caption muted">{formatNumber(tier.threshold)}+</span>
              </div>
              <div className="caption muted">{t('loyalty.multiplier', { value: tier.multiplier })}</div>
              <ul className="caption" style={{ margin: '8px 0 0', paddingLeft: 18 }}>
                {tier.benefits.map((benefit) => (
                  <li key={benefit}>{benefit}</li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>
      <section className="section">
        <h2 className="section-title row" style={{ gap: 8, marginBottom: 12 }}>
          <Gift size={20} aria-hidden="true" />
          {t('loyalty.tasks')}
        </h2>
        {tasks.isLoading && <Skeleton height={120} radius={16} />}
        {tasks.data && tasks.data.length === 0 && <p className="muted small">{t('loyalty.noTasks')}</p>}
        {tasks.data && tasks.data.length > 0 && (
          <ul className="steps" style={{ listStyle: 'none', margin: 0, padding: 0 }}>
            {tasks.data.map((task) => (
              <TaskCard key={task.id} task={task} />
            ))}
          </ul>
        )}
      </section>
      <Ledger />
    </>
  );
}

export default function LoyaltyPage() {
  const { t } = useTranslation();
  const summary = useQuery({ queryKey: ['loyalty', 'summary'], queryFn: accountApi.loyalty, staleTime: 0 });
  if (summary.isLoading) {
    return <PageLoader />;
  }
  if (summary.isError || !summary.data) {
    return (
      <div className="container page">
        <ErrorState error={summary.error} onRetry={() => summary.refetch()} />
      </div>
    );
  }
  return (
    <div className="container page">
      <header className="page-header">
        <h1 className="page-title">{t('loyalty.title')}</h1>
      </header>
      {summary.data.plan === 'PLUS' ? <PlusMember summary={summary.data} /> : <FreeMember summary={summary.data} />}
      <Referral />
    </div>
  );
}
