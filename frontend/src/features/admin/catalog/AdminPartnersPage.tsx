import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Plus } from 'lucide-react';
import { formatMoney } from '../../../shared/format';
import { CardGridSkeleton, ErrorState, Skeleton, errorMessage } from '../../../design-system/components/States';
import { Sheet } from '../../../design-system/components/Sheet';
import { useToast } from '../../../design-system/components/Toast';
import { adminApi } from '../api';
import { AdminHeader, CheckField, FormField, LocalizedInput, StatusPill, emptyText, fieldErrors } from '../forms';
import type { AdminPartner, PartnerRequest } from '../types';

function emptyPartner(): PartnerRequest {
  return { slug: '', name: '', description: emptyText(), city: 'Bakı', address: '', websiteUrl: '', logoPath: '', demo: true, status: 'ACTIVE' };
}

function Performance({ partnerId }: { partnerId: string }) {
  const { t } = useTranslation();
  const performance = useQuery({ queryKey: ['admin', 'partner-performance', partnerId], queryFn: () => adminApi.partnerPerformance(partnerId) });
  if (performance.isLoading) {
    return <Skeleton height={160} radius={16} />;
  }
  if (!performance.data) {
    return <ErrorState error={performance.error} />;
  }
  const data = performance.data;
  return (
    <div className="stack">
      <div className="stat-grid">
        <div className="stat">
          <div className="caption muted">{t('admin.partners.unitsSold')}</div>
          <div className="stat-value">{data.unitsSold}</div>
        </div>
        <div className="stat">
          <div className="caption muted">{t('admin.partners.payout')}</div>
          <div className="stat-value">{formatMoney(data.partnerPayout)}</div>
        </div>
        <div className="stat">
          <div className="caption muted">{t('admin.partners.redemptionRate')}</div>
          <div className="stat-value">{data.redemptionRatePercent}%</div>
        </div>
      </div>
      <dl className="stack" style={{ gap: 6, margin: 0 }}>
        <div className="kv">
          <dt>{t('admin.partners.issued')}</dt>
          <dd>{data.vouchersIssued}</dd>
        </div>
        <div className="kv">
          <dt>{t('admin.partners.redeemed')}</dt>
          <dd>{data.vouchersRedeemed}</dd>
        </div>
        <div className="kv">
          <dt>{t('admin.partners.refunded')}</dt>
          <dd>{data.refundedUnits}</dd>
        </div>
      </dl>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>{t('admin.fields.product')}</th>
              <th>{t('admin.fields.sold')}</th>
              <th>{t('admin.partners.sellThrough')}</th>
            </tr>
          </thead>
          <tbody>
            {data.products.map((product) => (
              <tr key={product.productId}>
                <td>{product.title}</td>
                <td>
                  {product.soldQuantity} / {product.totalQuantity}
                </td>
                <td>{product.sellThroughPercent}%</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default function AdminPartnersPage() {
  const { t } = useTranslation();
  const toast = useToast();
  const queryClient = useQueryClient();
  const partners = useQuery({ queryKey: ['admin', 'partners'], queryFn: adminApi.partners, staleTime: 0 });
  const [editing, setEditing] = useState<{ id?: string; form: PartnerRequest } | null>(null);
  const [viewing, setViewing] = useState<AdminPartner | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  const edit = (partner?: AdminPartner) => {
    setErrors({});
    setEditing(
      partner
        ? {
            id: partner.id,
            form: {
              slug: partner.slug,
              name: partner.name,
              description: partner.description,
              city: partner.city,
              address: partner.address ?? '',
              websiteUrl: partner.websiteUrl ?? '',
              logoPath: partner.logoPath ?? '',
              demo: partner.demo,
              status: partner.status,
            },
          }
        : { form: emptyPartner() },
    );
  };

  const save = async () => {
    if (!editing) {
      return;
    }
    setBusy(true);
    setErrors({});
    const form = editing.form;
    try {
      await adminApi.savePartner(editing.id, {
        ...form,
        slug: form.slug?.trim() || undefined,
        address: form.address?.trim() || undefined,
        websiteUrl: form.websiteUrl?.trim() || undefined,
        logoPath: form.logoPath?.trim() || undefined,
      });
      toast.show(t('admin.saved'));
      void queryClient.invalidateQueries({ queryKey: ['admin', 'partners'] });
      void queryClient.invalidateQueries({ queryKey: ['partners'] });
      setEditing(null);
    } catch (error) {
      setErrors(fieldErrors(error));
      toast.show(errorMessage(error, t('errors.generic')), 'error');
    } finally {
      setBusy(false);
    }
  };

  const set = <K extends keyof PartnerRequest>(key: K, value: PartnerRequest[K]) => editing && setEditing({ ...editing, form: { ...editing.form, [key]: value } });

  return (
    <>
      <AdminHeader
        title={t('admin.nav.partners')}
        action={
          <button type="button" className="btn btn-primary btn-sm" onClick={() => edit()}>
            <Plus size={16} aria-hidden="true" />
            {t('admin.new')}
          </button>
        }
      />
      {partners.isLoading && <CardGridSkeleton count={2} />}
      {partners.isError && <ErrorState error={partners.error} onRetry={() => partners.refetch()} />}
      {partners.data && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>{t('admin.fields.name')}</th>
                <th>{t('admin.fields.city')}</th>
                <th>{t('admin.status')}</th>
                <th>{t('admin.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {partners.data.map((partner) => (
                <tr key={partner.id}>
                  <td>
                    {partner.name}
                    {partner.demo && <span className="badge badge-soft" style={{ marginLeft: 6 }}>demo</span>}
                    <div className="caption tertiary">{partner.slug}</div>
                  </td>
                  <td>{partner.city}</td>
                  <td>
                    <StatusPill status={partner.status} />
                  </td>
                  <td>
                    <div className="row" style={{ gap: 6 }}>
                      <button type="button" className="btn btn-secondary btn-sm" onClick={() => edit(partner)}>
                        {t('admin.edit')}
                      </button>
                      <button type="button" className="btn btn-ghost btn-sm" onClick={() => setViewing(partner)}>
                        {t('admin.partners.performance')}
                      </button>
                    </div>
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
            <FormField id="partner-name" label={t('admin.fields.name')} error={errors.name}>
              <input id="partner-name" className="input" maxLength={120} value={editing.form.name} onChange={(event) => set('name', event.target.value)} />
            </FormField>
            <FormField id="partner-slug" label={t('admin.fields.slug')} hint={t('admin.fields.slugHint')} error={errors.slug}>
              <input id="partner-slug" className="input" maxLength={80} value={editing.form.slug ?? ''} onChange={(event) => set('slug', event.target.value)} />
            </FormField>
            <LocalizedInput id="partner-description" label={t('admin.fields.description')} value={editing.form.description} onChange={(value) => set('description', value)} multiline maxLength={2000} error={errors.description} />
            <FormField id="partner-city" label={t('admin.fields.city')} error={errors.city}>
              <input id="partner-city" className="input" maxLength={60} value={editing.form.city} onChange={(event) => set('city', event.target.value)} />
            </FormField>
            <FormField id="partner-address" label={t('admin.fields.address')} error={errors.address}>
              <input id="partner-address" className="input" maxLength={200} value={editing.form.address ?? ''} onChange={(event) => set('address', event.target.value)} />
            </FormField>
            <FormField id="partner-website" label={t('admin.fields.website')} error={errors.websiteUrl}>
              <input id="partner-website" className="input" type="url" maxLength={300} value={editing.form.websiteUrl ?? ''} onChange={(event) => set('websiteUrl', event.target.value)} />
            </FormField>
            <FormField id="partner-logo" label={t('admin.fields.logoPath')} error={errors.logoPath}>
              <input id="partner-logo" className="input" maxLength={300} value={editing.form.logoPath ?? ''} onChange={(event) => set('logoPath', event.target.value)} />
            </FormField>
            <FormField id="partner-status" label={t('admin.status')}>
              <select id="partner-status" className="select" value={editing.form.status} onChange={(event) => set('status', event.target.value as PartnerRequest['status'])}>
                <option value="ACTIVE">{t('admin.statuses.ACTIVE')}</option>
                <option value="INACTIVE">{t('admin.statuses.INACTIVE')}</option>
              </select>
            </FormField>
            <CheckField label={t('admin.fields.demo')} checked={editing.form.demo} onChange={(value) => set('demo', value)} />
          </div>
        )}
      </Sheet>
      <Sheet open={viewing !== null} title={viewing ? `${viewing.name} · ${t('admin.partners.performance')}` : ''} onClose={() => setViewing(null)}>
        {viewing && <Performance partnerId={viewing.id} />}
      </Sheet>
    </>
  );
}
