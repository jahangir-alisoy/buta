import { useEffect, useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { formatDateTime } from '../../../shared/format';
import { CardGridSkeleton, EmptyState, ErrorState } from '../../../design-system/components/States';
import { adminApi } from '../api';
import { AdminHeader } from '../forms';

export default function AdminAuditPage() {
  const { t } = useTranslation();
  const [action, setAction] = useState('');
  const [targetType, setTargetType] = useState('');
  const [filters, setFilters] = useState({ action: '', targetType: '' });
  const [page, setPage] = useState(0);
  const logs = useQuery({
    queryKey: ['admin', 'audit', filters, page],
    queryFn: () => adminApi.auditLogs(filters.action, filters.targetType, page),
    placeholderData: keepPreviousData,
    staleTime: 0,
  });

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setFilters({ action: action.trim().toUpperCase(), targetType: targetType.trim().toUpperCase() });
      setPage(0);
    }, 350);
    return () => window.clearTimeout(timer);
  }, [action, targetType]);

  return (
    <>
      <AdminHeader title={t('admin.nav.audit')} />
      <div className="toolbar">
        <input className="input" placeholder={t('admin.fields.action')} aria-label={t('admin.fields.action')} value={action} onChange={(event) => setAction(event.target.value)} style={{ maxWidth: 240 }} />
        <input className="input" placeholder={t('admin.fields.target2')} aria-label={t('admin.fields.target2')} value={targetType} onChange={(event) => setTargetType(event.target.value)} style={{ maxWidth: 200 }} />
      </div>
      {logs.isLoading && <CardGridSkeleton count={2} />}
      {logs.isError && <ErrorState error={logs.error} onRetry={() => logs.refetch()} />}
      {logs.data && logs.data.items.length === 0 && <EmptyState title={t('admin.audit.empty')} />}
      {logs.data && logs.data.items.length > 0 && (
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>{t('admin.fields.createdAt')}</th>
                  <th>{t('admin.fields.actor')}</th>
                  <th>{t('admin.fields.action')}</th>
                  <th>{t('admin.fields.target2')}</th>
                  <th>{t('admin.details')}</th>
                </tr>
              </thead>
              <tbody>
                {logs.data.items.map((entry) => (
                  <tr key={entry.id}>
                    <td style={{ whiteSpace: 'nowrap' }}>{formatDateTime(entry.createdAt)}</td>
                    <td>
                      {entry.actorType}
                      {entry.actorUserId && <div className="caption tertiary">{entry.actorUserId.slice(0, 8)}</div>}
                    </td>
                    <td>{entry.action}</td>
                    <td>
                      {entry.targetType}
                      {entry.targetId && <div className="caption tertiary">{entry.targetId.slice(0, 8)}</div>}
                    </td>
                    <td>
                      <code className="caption" style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>
                        {JSON.stringify(entry.details)}
                      </code>
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
              {page + 1} {t('common.of')} {Math.max(1, logs.data.totalPages)}
            </span>
            <button type="button" className="btn btn-ghost btn-sm" disabled={page + 1 >= logs.data.totalPages} onClick={() => setPage(page + 1)}>
              →
            </button>
          </div>
        </>
      )}
    </>
  );
}
