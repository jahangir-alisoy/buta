import { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Plus, Trash2 } from 'lucide-react';
import { formatDate } from '../../../shared/format';
import { CardGridSkeleton, EmptyState, ErrorState, errorMessage } from '../../../design-system/components/States';
import { Segmented } from '../../../design-system/components/Controls';
import { Sheet } from '../../../design-system/components/Sheet';
import { useToast } from '../../../design-system/components/Toast';
import { adminApi } from '../api';
import { AdminHeader, CheckField, FormField, LocalizedInput, StatusPill, emptyText, fieldErrors, fromLocalInput, toLocalInput, useLocalized } from '../forms';
import type { AdminTask, AdminTier, TaskRequest } from '../types';

const TASK_TYPES = ['DISTINCT_PARTNERS', 'BUNDLE_PURCHASE', 'EVENT_ATTENDANCE', 'NEW_CATEGORY', 'REFERRAL'];
const FEATURES = ['LOYALTY_POINTS', 'LOYALTY_TASKS', 'TIERS', 'POINTS_REDEMPTION', 'PERSONALIZED_RECOMMENDATIONS', 'EXCLUSIVE_OFFERS'];

type Tab = 'tasks' | 'tiers' | 'referrals' | 'plans';

interface TaskForm extends Omit<TaskRequest, 'startsAt' | 'endsAt'> {
  startsAt: string;
  endsAt: string;
}

function toTaskForm(task?: AdminTask): TaskForm {
  if (!task) {
    return {
      code: '',
      taskType: 'DISTINCT_PARTNERS',
      title: emptyText(),
      description: emptyText(),
      terms: emptyText(),
      targetValue: 3,
      rewardPoints: 50,
      periodType: 'CALENDAR_MONTH',
      startsAt: toLocalInput(new Date().toISOString()),
      endsAt: '',
      active: true,
      minOrderAmount: 0,
    };
  }
  return { ...task, startsAt: toLocalInput(task.startsAt), endsAt: toLocalInput(task.endsAt) };
}

function Tasks() {
  const { t } = useTranslation();
  const toast = useToast();
  const localized = useLocalized();
  const queryClient = useQueryClient();
  const tasks = useQuery({ queryKey: ['admin', 'tasks'], queryFn: adminApi.tasks, staleTime: 0 });
  const [editing, setEditing] = useState<{ id?: string; form: TaskForm } | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  const set = <K extends keyof TaskForm>(key: K, value: TaskForm[K]) => editing && setEditing({ ...editing, form: { ...editing.form, [key]: value } });

  const save = async () => {
    if (!editing) {
      return;
    }
    setBusy(true);
    setErrors({});
    try {
      await adminApi.saveTask(editing.id, {
        ...editing.form,
        startsAt: fromLocalInput(editing.form.startsAt) ?? '',
        endsAt: fromLocalInput(editing.form.endsAt),
      });
      toast.show(t('admin.saved'));
      void queryClient.invalidateQueries({ queryKey: ['admin', 'tasks'] });
      setEditing(null);
    } catch (error) {
      setErrors(fieldErrors(error));
      toast.show(errorMessage(error, t('errors.generic')), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <div className="row" style={{ justifyContent: 'flex-end', marginBottom: 12 }}>
        <button type="button" className="btn btn-primary btn-sm" onClick={() => setEditing({ form: toTaskForm() })}>
          <Plus size={16} aria-hidden="true" />
          {t('admin.new')}
        </button>
      </div>
      {tasks.isLoading && <CardGridSkeleton count={2} />}
      {tasks.isError && <ErrorState error={tasks.error} onRetry={() => tasks.refetch()} />}
      {tasks.data && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>{t('admin.fields.title')}</th>
                <th>{t('admin.fields.taskType')}</th>
                <th>{t('admin.fields.target')}</th>
                <th>{t('admin.fields.rewardPoints')}</th>
                <th>{t('admin.fields.period')}</th>
                <th>{t('admin.status')}</th>
                <th>{t('admin.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {tasks.data.map((task) => (
                <tr key={task.id}>
                  <td>
                    {localized(task.title)}
                    <div className="caption tertiary">{task.code}</div>
                  </td>
                  <td>{task.taskType}</td>
                  <td>{task.targetValue}</td>
                  <td>{task.rewardPoints}</td>
                  <td>{task.periodType}</td>
                  <td>{task.active ? t('admin.statuses.ACTIVE') : t('admin.statuses.INACTIVE')}</td>
                  <td>
                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => setEditing({ id: task.id, form: toTaskForm(task) })}>
                      {t('admin.edit')}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Sheet
        open={editing !== null}
        title={editing?.id ? t('admin.edit') : t('admin.new')}
        onClose={() => setEditing(null)}
        footer={
          <button type="button" className="btn btn-primary btn-block" onClick={save} disabled={busy}>
            {busy ? t('common.saving') : t('common.save')}
          </button>
        }
      >
        {editing && (
          <div className="stack">
            <FormField id="task-code" label={t('admin.fields.code')} hint={t('admin.fields.slugHint')} error={errors.code}>
              <input id="task-code" className="input" maxLength={60} value={editing.form.code} onChange={(event) => set('code', event.target.value)} disabled={Boolean(editing.id)} />
            </FormField>
            <FormField id="task-type" label={t('admin.fields.taskType')}>
              <select id="task-type" className="select" value={editing.form.taskType} onChange={(event) => set('taskType', event.target.value)}>
                {TASK_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </select>
            </FormField>
            <LocalizedInput id="task-title" label={t('admin.fields.title')} value={editing.form.title} onChange={(value) => set('title', value)} maxLength={120} error={errors.title} />
            <LocalizedInput id="task-description" label={t('admin.fields.description')} value={editing.form.description} onChange={(value) => set('description', value)} multiline maxLength={500} error={errors.description} />
            <LocalizedInput id="task-terms" label={t('admin.fields.terms')} value={editing.form.terms} onChange={(value) => set('terms', value)} multiline maxLength={1000} error={errors.terms} />
            <FormField id="task-target" label={t('admin.fields.target')} error={errors.targetValue}>
              <input id="task-target" className="input" type="number" min={1} max={100} value={editing.form.targetValue} onChange={(event) => set('targetValue', Number(event.target.value))} />
            </FormField>
            <FormField id="task-reward" label={t('admin.fields.rewardPoints')} error={errors.rewardPoints}>
              <input id="task-reward" className="input" type="number" min={1} max={100000} value={editing.form.rewardPoints} onChange={(event) => set('rewardPoints', Number(event.target.value))} />
            </FormField>
            <FormField id="task-period" label={t('admin.fields.period')}>
              <select id="task-period" className="select" value={editing.form.periodType} onChange={(event) => set('periodType', event.target.value)}>
                <option value="ONE_TIME">ONE_TIME</option>
                <option value="CALENDAR_MONTH">CALENDAR_MONTH</option>
              </select>
            </FormField>
            <FormField id="task-min" label={t('admin.fields.minOrderAmount')} error={errors.minOrderAmount}>
              <input id="task-min" className="input" type="number" min={0} step={0.01} value={editing.form.minOrderAmount} onChange={(event) => set('minOrderAmount', Number(event.target.value))} />
            </FormField>
            <FormField id="task-start" label={`${t('admin.fields.startsAt')} (Asia/Baku)`} error={errors.startsAt}>
              <input id="task-start" className="input" type="datetime-local" value={editing.form.startsAt} onChange={(event) => set('startsAt', event.target.value)} />
            </FormField>
            <FormField id="task-end" label={`${t('admin.fields.endsAtOptional')} (Asia/Baku)`} error={errors.endsAt}>
              <input id="task-end" className="input" type="datetime-local" value={editing.form.endsAt} onChange={(event) => set('endsAt', event.target.value)} />
            </FormField>
            <CheckField label={t('admin.fields.active')} checked={editing.form.active} onChange={(value) => set('active', value)} />
          </div>
        )}
      </Sheet>
    </>
  );
}

interface TierDraft {
  code: string;
  nameAz: string;
  nameEn: string;
  thresholdPoints: string;
  earnMultiplier: string;
  benefitsAz: string;
  benefitsEn: string;
}

function toDraft(tier: AdminTier): TierDraft {
  return {
    code: tier.code,
    nameAz: tier.name.az,
    nameEn: tier.name.en,
    thresholdPoints: String(tier.thresholdPoints),
    earnMultiplier: String(tier.earnMultiplier),
    benefitsAz: (tier.benefits.az ?? []).join('\n'),
    benefitsEn: (tier.benefits.en ?? []).join('\n'),
  };
}

function toTier(draft: TierDraft): AdminTier {
  const lines = (text: string) =>
    text
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean);
  return {
    code: draft.code.trim().toUpperCase(),
    name: { az: draft.nameAz, en: draft.nameEn },
    thresholdPoints: Number(draft.thresholdPoints),
    earnMultiplier: Number(draft.earnMultiplier),
    benefits: { az: lines(draft.benefitsAz), en: lines(draft.benefitsEn) },
  };
}

function Tiers() {
  const { t } = useTranslation();
  const toast = useToast();
  const queryClient = useQueryClient();
  const tiers = useQuery({ queryKey: ['admin', 'tiers'], queryFn: adminApi.tiers, staleTime: 0 });
  const [drafts, setDrafts] = useState<TierDraft[]>([]);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (tiers.data) {
      setDrafts(tiers.data.map(toDraft));
    }
  }, [tiers.data]);

  const update = (index: number, patch: Partial<TierDraft>) => setDrafts(drafts.map((draft, position) => (position === index ? { ...draft, ...patch } : draft)));

  const save = async () => {
    setBusy(true);
    try {
      const saved = await adminApi.saveTiers(drafts.map(toTier));
      queryClient.setQueryData(['admin', 'tiers'], saved);
      toast.show(t('admin.saved'));
    } catch (error) {
      toast.show(errorMessage(error, t('errors.generic')), 'error');
    } finally {
      setBusy(false);
    }
  };

  if (tiers.isLoading) {
    return <CardGridSkeleton count={2} />;
  }
  if (tiers.isError) {
    return <ErrorState error={tiers.error} onRetry={() => tiers.refetch()} />;
  }
  return (
    <div className="stack">
      {drafts.map((draft, index) => (
        <div key={index} className="card form-grid">
          <FormField id={`tier-code-${index}`} label={t('admin.fields.code')}>
            <input id={`tier-code-${index}`} className="input" value={draft.code} onChange={(event) => update(index, { code: event.target.value })} />
          </FormField>
          <FormField id={`tier-az-${index}`} label={`${t('admin.fields.name')} (AZ)`}>
            <input id={`tier-az-${index}`} className="input" value={draft.nameAz} onChange={(event) => update(index, { nameAz: event.target.value })} />
          </FormField>
          <FormField id={`tier-en-${index}`} label={`${t('admin.fields.name')} (EN)`}>
            <input id={`tier-en-${index}`} className="input" value={draft.nameEn} onChange={(event) => update(index, { nameEn: event.target.value })} />
          </FormField>
          <FormField id={`tier-threshold-${index}`} label={t('admin.fields.threshold')}>
            <input id={`tier-threshold-${index}`} className="input" type="number" min={0} value={draft.thresholdPoints} onChange={(event) => update(index, { thresholdPoints: event.target.value })} />
          </FormField>
          <FormField id={`tier-multiplier-${index}`} label={t('admin.fields.multiplier')}>
            <input
              id={`tier-multiplier-${index}`}
              className="input"
              type="number"
              min={1}
              max={5}
              step={0.05}
              value={draft.earnMultiplier}
              onChange={(event) => update(index, { earnMultiplier: event.target.value })}
            />
          </FormField>
          <FormField id={`tier-benefits-az-${index}`} label={`${t('admin.fields.benefits')} (AZ)`} hint={t('admin.fields.benefitsHint')}>
            <textarea id={`tier-benefits-az-${index}`} className="textarea" rows={3} value={draft.benefitsAz} onChange={(event) => update(index, { benefitsAz: event.target.value })} />
          </FormField>
          <FormField id={`tier-benefits-en-${index}`} label={`${t('admin.fields.benefits')} (EN)`} hint={t('admin.fields.benefitsHint')}>
            <textarea id={`tier-benefits-en-${index}`} className="textarea" rows={3} value={draft.benefitsEn} onChange={(event) => update(index, { benefitsEn: event.target.value })} />
          </FormField>
          <div className="field" style={{ justifyContent: 'flex-end' }}>
            <button type="button" className="btn btn-ghost btn-sm" disabled={drafts.length === 1} onClick={() => setDrafts(drafts.filter((_, position) => position !== index))}>
              <Trash2 size={16} aria-hidden="true" />
              {t('common.remove')}
            </button>
          </div>
        </div>
      ))}
      <div className="row wrap">
        <button
          type="button"
          className="btn btn-secondary"
          disabled={drafts.length >= 10}
          onClick={() => setDrafts([...drafts, { code: '', nameAz: '', nameEn: '', thresholdPoints: '0', earnMultiplier: '1.00', benefitsAz: '', benefitsEn: '' }])}
        >
          <Plus size={16} aria-hidden="true" />
          {t('admin.loyalty.addTier')}
        </button>
        <button type="button" className="btn btn-primary" onClick={save} disabled={busy}>
          {busy ? t('common.saving') : t('admin.loyalty.saveTiers')}
        </button>
      </div>
    </div>
  );
}

function Referrals() {
  const { t } = useTranslation();
  const toast = useToast();
  const queryClient = useQueryClient();
  const referrals = useQuery({ queryKey: ['admin', 'referrals'], queryFn: adminApi.referrals, staleTime: 0 });
  const [rejecting, setRejecting] = useState<string | null>(null);
  const [reason, setReason] = useState('');

  const act = async (action: () => Promise<unknown>) => {
    try {
      await action();
      toast.show(t('admin.saved'));
      setRejecting(null);
      setReason('');
      void queryClient.invalidateQueries({ queryKey: ['admin', 'referrals'] });
    } catch (error) {
      toast.show(errorMessage(error, t('errors.generic')), 'error');
    }
  };

  if (referrals.isLoading) {
    return <CardGridSkeleton count={2} />;
  }
  if (referrals.isError) {
    return <ErrorState error={referrals.error} onRetry={() => referrals.refetch()} />;
  }
  if (!referrals.data || referrals.data.length === 0) {
    return <EmptyState title={t('admin.loyalty.noReferrals')} />;
  }
  return (
    <>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>{t('admin.fields.createdAt')}</th>
              <th>{t('admin.fields.referrer')}</th>
              <th>{t('admin.fields.referee')}</th>
              <th>{t('admin.status')}</th>
              <th>{t('admin.actions')}</th>
            </tr>
          </thead>
          <tbody>
            {referrals.data.map((referral) => (
              <tr key={referral.id}>
                <td>{formatDate(referral.createdAt)}</td>
                <td>{referral.referrerEmail}</td>
                <td>{referral.refereeEmail}</td>
                <td>
                  <StatusPill status={referral.status} />
                  {referral.reviewNote && <div className="caption tertiary">{referral.reviewNote}</div>}
                </td>
                <td>
                  <div className="row" style={{ gap: 6 }}>
                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => act(() => adminApi.approveReferral(referral.id))}>
                      {t('admin.approve')}
                    </button>
                    <button type="button" className="btn btn-ghost btn-sm" onClick={() => setRejecting(referral.id)}>
                      {t('admin.reject')}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Sheet
        open={rejecting !== null}
        title={t('admin.reject')}
        onClose={() => setRejecting(null)}
        footer={
          <button type="button" className="btn btn-danger btn-block" disabled={!reason.trim()} onClick={() => rejecting && act(() => adminApi.rejectReferral(rejecting, reason.trim()))}>
            {t('admin.reject')}
          </button>
        }
      >
        <FormField id="reject-reason" label={t('admin.loyalty.rejectReason')}>
          <input id="reject-reason" className="input" maxLength={200} value={reason} onChange={(event) => setReason(event.target.value)} />
        </FormField>
      </Sheet>
    </>
  );
}

function Plans() {
  const { t } = useTranslation();
  const toast = useToast();
  const localized = useLocalized();
  const queryClient = useQueryClient();
  const plans = useQuery({ queryKey: ['admin', 'plans'], queryFn: adminApi.plans, staleTime: 0 });

  const toggle = async (code: string, features: string[], feature: string) => {
    const next = features.includes(feature) ? features.filter((item) => item !== feature) : [...features, feature];
    try {
      await adminApi.savePlan(code, next);
      void queryClient.invalidateQueries({ queryKey: ['admin', 'plans'] });
      toast.show(t('admin.saved'));
    } catch (error) {
      toast.show(errorMessage(error, t('errors.generic')), 'error');
    }
  };

  if (plans.isLoading) {
    return <CardGridSkeleton count={2} />;
  }
  if (plans.isError) {
    return <ErrorState error={plans.error} onRetry={() => plans.refetch()} />;
  }
  return (
    <div className="steps">
      {plans.data?.map((plan) => (
        <div key={plan.code} className="card stack">
          <h2 className="section-title">
            {localized(plan.name)} <span className="caption tertiary">{plan.code}</span>
          </h2>
          <span className="small muted">{t('admin.fields.features')}</span>
          {FEATURES.map((feature) => (
            <CheckField key={feature} label={feature} checked={plan.features.includes(feature)} onChange={() => toggle(plan.code, plan.features, feature)} />
          ))}
        </div>
      ))}
    </div>
  );
}

export default function AdminLoyaltyPage() {
  const { t } = useTranslation();
  const [tab, setTab] = useState<Tab>('tasks');
  return (
    <>
      <AdminHeader title={t('admin.nav.loyalty')} />
      <div style={{ marginBottom: 16, overflowX: 'auto' }}>
        <Segmented<Tab>
          label={t('admin.nav.loyalty')}
          value={tab}
          onChange={setTab}
          options={[
            { value: 'tasks', label: t('admin.loyalty.tasks') },
            { value: 'tiers', label: t('admin.loyalty.tiers') },
            { value: 'referrals', label: t('admin.loyalty.referrals') },
            { value: 'plans', label: t('admin.loyalty.plans') },
          ]}
        />
      </div>
      {tab === 'tasks' && <Tasks />}
      {tab === 'tiers' && <Tiers />}
      {tab === 'referrals' && <Referrals />}
      {tab === 'plans' && <Plans />}
    </>
  );
}
