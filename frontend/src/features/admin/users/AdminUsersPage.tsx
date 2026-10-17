import { useEffect, useState } from 'react';
import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Search } from 'lucide-react';
import { formatDate } from '../../../shared/format';
import { CardGridSkeleton, ErrorState, errorMessage } from '../../../design-system/components/States';
import { Sheet } from '../../../design-system/components/Sheet';
import { useToast } from '../../../design-system/components/Toast';
import { adminApi } from '../api';
import { AdminHeader, FormField, StatusPill, fromLocalInput } from '../forms';
import type { AdminUser } from '../types';

function UserActions({ user, onChange }: { user: AdminUser; onChange: (user: AdminUser) => void }) {
  const { t } = useTranslation();
  const toast = useToast();
  const [reason, setReason] = useState('');
  const [points, setPoints] = useState('');
  const [endsAt, setEndsAt] = useState('');
  const [busy, setBusy] = useState(false);

  const run = async (action: () => Promise<AdminUser | void>) => {
    setBusy(true);
    try {
      const updated = await action();
      if (updated) {
        onChange(updated);
      }
      toast.show(t('admin.users.done'));
      setPoints('');
    } catch (error) {
      toast.show(errorMessage(error, t('errors.generic')), 'error');
    } finally {
      setBusy(false);
    }
  };

  const needsReason = !reason.trim();
  return (
    <div className="stack">
      <dl className="stack" style={{ gap: 6, margin: 0 }}>
        <div className="kv">
          <dt>{t('admin.fields.email')}</dt>
          <dd>{user.email}</dd>
        </div>
        <div className="kv">
          <dt>{t('admin.fields.plan')}</dt>
          <dd>
            {user.plan}
            {user.planSource ? ` · ${user.planSource}` : ''}
            {user.planEndsAt ? ` · ${formatDate(user.planEndsAt)}` : ''}
          </dd>
        </div>
        <div className="kv">
          <dt>{t('admin.fields.points')}</dt>
          <dd>
            {user.availablePoints} {user.tier ? `· ${user.tier}` : ''}
          </dd>
        </div>
        <div className="kv">
          <dt>{t('admin.status')}</dt>
          <dd>
            <StatusPill status={user.status} />
          </dd>
        </div>
      </dl>
      {user.flaggedForReview && <p className="alert alert-warning small">{t('admin.users.flagged')}</p>}
      <FormField id="user-reason" label={t('admin.fields.reason')}>
        <input id="user-reason" className="input" maxLength={200} value={reason} onChange={(event) => setReason(event.target.value)} />
      </FormField>
      <div className="row wrap">
        {user.plan === 'PLUS' ? (
          <button type="button" className="btn btn-secondary btn-sm" disabled={busy || needsReason} onClick={() => run(() => adminApi.revokePlus(user.id, reason.trim()))}>
            {t('admin.users.revokePlus')}
          </button>
        ) : (
          <>
            <input
              className="input"
              type="datetime-local"
              aria-label={t('admin.fields.endsAtOptional')}
              title={t('admin.fields.endsAtOptional')}
              value={endsAt}
              onChange={(event) => setEndsAt(event.target.value)}
              style={{ maxWidth: 220 }}
            />
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              disabled={busy || needsReason}
              onClick={() => run(() => adminApi.assignPlus(user.id, reason.trim(), fromLocalInput(endsAt)))}
            >
              {t('admin.users.assignPlus')}
            </button>
          </>
        )}
      </div>
      <div className="row wrap">
        {user.status === 'SUSPENDED' ? (
          <button type="button" className="btn btn-secondary btn-sm" disabled={busy} onClick={() => run(() => adminApi.reactivate(user.id))}>
            {t('admin.users.reactivate')}
          </button>
        ) : (
          <button type="button" className="btn btn-danger btn-sm" disabled={busy || needsReason} onClick={() => run(() => adminApi.suspend(user.id, reason.trim()))}>
            {t('admin.users.suspend')}
          </button>
        )}
        <button type="button" className="btn btn-ghost btn-sm" disabled={busy} onClick={() => run(() => adminApi.revokeSessions(user.id))}>
          {t('admin.users.revokeSessions')}
        </button>
      </div>
      <div className="row wrap">
        <input
          className="input"
          type="number"
          min={-100000}
          max={100000}
          placeholder={t('admin.fields.delta')}
          aria-label={t('admin.users.adjustPoints')}
          value={points}
          onChange={(event) => setPoints(event.target.value)}
          style={{ maxWidth: 160 }}
        />
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          disabled={busy || needsReason || !points || Number(points) === 0}
          onClick={() => run(() => adminApi.adjustPoints(user.id, Number(points), reason.trim()))}
        >
          {t('admin.users.adjustPoints')}
        </button>
      </div>
    </div>
  );
}

export default function AdminUsersPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<AdminUser | null>(null);
  const users = useQuery({ queryKey: ['admin', 'users', query, page], queryFn: () => adminApi.users(query, page), placeholderData: keepPreviousData, staleTime: 0 });

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setQuery(search.trim());
      setPage(0);
    }, 350);
    return () => window.clearTimeout(timer);
  }, [search]);

  const changed = (user: AdminUser) => {
    setSelected(user);
    void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
  };

  return (
    <>
      <AdminHeader title={t('admin.nav.users')} />
      <label className="search-bar" style={{ marginBottom: 16 }}>
        <Search size={18} />
        <span className="visually-hidden">{t('admin.search')}</span>
        <input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder={t('admin.search')} />
      </label>
      {users.isLoading && <CardGridSkeleton count={2} />}
      {users.isError && <ErrorState error={users.error} onRetry={() => users.refetch()} />}
      {users.data && (
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>{t('admin.fields.email')}</th>
                  <th>{t('admin.fields.name')}</th>
                  <th>{t('admin.fields.plan')}</th>
                  <th>{t('admin.fields.points')}</th>
                  <th>{t('admin.status')}</th>
                  <th>{t('admin.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {users.data.items.map((user) => (
                  <tr key={user.id}>
                    <td>
                      {user.email}
                      {user.roles.includes('ADMIN') && <span className="badge badge-accent" style={{ marginLeft: 6 }}>ADMIN</span>}
                    </td>
                    <td>
                      {user.firstName} {user.lastName}
                      {user.username && <div className="caption tertiary">@{user.username}</div>}
                    </td>
                    <td>{user.plan === 'PLUS' ? <span className="badge badge-plus">PLUS</span> : user.plan}</td>
                    <td>{user.availablePoints}</td>
                    <td>
                      <StatusPill status={user.status} />
                    </td>
                    <td>
                      <button type="button" className="btn btn-secondary btn-sm" onClick={() => setSelected(user)}>
                        {t('admin.details')}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="row spread" style={{ marginTop: 12 }}>
            <button type="button" className="btn btn-ghost btn-sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
              ←
            </button>
            <span className="caption muted">
              {page + 1} {t('common.of')} {Math.max(1, users.data.totalPages)}
            </span>
            <button type="button" className="btn btn-ghost btn-sm" disabled={page + 1 >= users.data.totalPages} onClick={() => setPage(page + 1)}>
              →
            </button>
          </div>
        </>
      )}
      <Sheet open={selected !== null} title={selected ? `${selected.firstName ?? ''} ${selected.lastName ?? ''}`.trim() || selected.email : ''} onClose={() => setSelected(null)}>
        {selected && <UserActions key={selected.id} user={selected} onChange={changed} />}
      </Sheet>
    </>
  );
}
