import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Plus } from 'lucide-react';
import { formatDate } from '../../../shared/format';
import { CardGridSkeleton, ErrorState, errorMessage } from '../../../design-system/components/States';
import { Sheet } from '../../../design-system/components/Sheet';
import { useToast } from '../../../design-system/components/Toast';
import { adminApi } from '../api';
import { AdminHeader, CheckField, FormField, LocalizedInput, StatusPill, emptyText, fieldErrors, fromLocalInput, toLocalInput, useLocalized } from '../forms';
import type { AdminCampaign, CampaignRequest } from '../types';

interface CampaignForm extends Omit<CampaignRequest, 'startsAt' | 'endsAt'> {
  startsAt: string;
  endsAt: string;
}

function toForm(campaign?: AdminCampaign): CampaignForm {
  if (!campaign) {
    const now = new Date();
    return {
      slug: '',
      name: emptyText(),
      bannerText: emptyText(),
      audience: 'ALL',
      startsAt: toLocalInput(now.toISOString()),
      endsAt: toLocalInput(new Date(now.getTime() + 14 * 24 * 3600 * 1000).toISOString()),
      bonusPointsPerUnit: 0,
      featured: false,
      status: 'ACTIVE',
      offerIds: [],
    };
  }
  return { ...campaign, startsAt: toLocalInput(campaign.startsAt), endsAt: toLocalInput(campaign.endsAt) };
}

export default function AdminCampaignsPage() {
  const { t } = useTranslation();
  const toast = useToast();
  const localized = useLocalized();
  const queryClient = useQueryClient();
  const campaigns = useQuery({ queryKey: ['admin', 'campaigns'], queryFn: adminApi.campaigns, staleTime: 0 });
  const offers = useQuery({ queryKey: ['admin', 'offers'], queryFn: adminApi.offers });
  const [editing, setEditing] = useState<{ id?: string; form: CampaignForm } | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  const set = <K extends keyof CampaignForm>(key: K, value: CampaignForm[K]) => editing && setEditing({ ...editing, form: { ...editing.form, [key]: value } });

  const toggleOffer = (offerId: string) => {
    if (!editing) {
      return;
    }
    const selected = editing.form.offerIds.includes(offerId);
    set('offerIds', selected ? editing.form.offerIds.filter((id) => id !== offerId) : [...editing.form.offerIds, offerId]);
  };

  const save = async () => {
    if (!editing) {
      return;
    }
    setBusy(true);
    setErrors({});
    const form = editing.form;
    try {
      await adminApi.saveCampaign(editing.id, {
        ...form,
        slug: form.slug?.trim() || undefined,
        startsAt: fromLocalInput(form.startsAt) ?? '',
        endsAt: fromLocalInput(form.endsAt) ?? '',
      });
      toast.show(t('admin.saved'));
      void queryClient.invalidateQueries({ queryKey: ['admin', 'campaigns'] });
      void queryClient.invalidateQueries({ queryKey: ['home'] });
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
      <AdminHeader
        title={t('admin.nav.campaigns')}
        action={
          <button type="button" className="btn btn-primary btn-sm" onClick={() => setEditing({ form: toForm() })}>
            <Plus size={16} aria-hidden="true" />
            {t('admin.new')}
          </button>
        }
      />
      {campaigns.isLoading && <CardGridSkeleton count={2} />}
      {campaigns.isError && <ErrorState error={campaigns.error} onRetry={() => campaigns.refetch()} />}
      {campaigns.data && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>{t('admin.fields.name')}</th>
                <th>{t('admin.fields.audience')}</th>
                <th>{t('admin.fields.endsAt')}</th>
                <th>{t('admin.fields.bonusPoints')}</th>
                <th>{t('admin.status')}</th>
                <th>{t('admin.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {campaigns.data.map((campaign) => (
                <tr key={campaign.id}>
                  <td>
                    {localized(campaign.name)}
                    {campaign.featured && <span className="badge badge-accent" style={{ marginLeft: 6 }}>{t('admin.fields.featured')}</span>}
                    <div className="caption tertiary">{t('partners.offers', { count: campaign.offerIds.length })}</div>
                  </td>
                  <td>{campaign.audience}</td>
                  <td>{formatDate(campaign.endsAt)}</td>
                  <td>{campaign.bonusPointsPerUnit}</td>
                  <td>
                    <StatusPill status={campaign.status} />
                  </td>
                  <td>
                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => setEditing({ id: campaign.id, form: toForm(campaign) })}>
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
            <LocalizedInput id="campaign-name" label={t('admin.fields.name')} value={editing.form.name} onChange={(value) => set('name', value)} maxLength={120} error={errors.name} />
            <LocalizedInput id="campaign-banner" label={t('admin.fields.bannerText')} value={editing.form.bannerText} onChange={(value) => set('bannerText', value)} multiline maxLength={300} error={errors.bannerText} />
            <FormField id="campaign-slug" label={t('admin.fields.slug')} hint={t('admin.fields.slugHint')} error={errors.slug}>
              <input id="campaign-slug" className="input" maxLength={80} value={editing.form.slug ?? ''} onChange={(event) => set('slug', event.target.value)} />
            </FormField>
            <FormField id="campaign-audience" label={t('admin.fields.audience')}>
              <select id="campaign-audience" className="select" value={editing.form.audience} onChange={(event) => set('audience', event.target.value as CampaignForm['audience'])}>
                <option value="ALL">ALL</option>
                <option value="PLUS">PLUS</option>
              </select>
            </FormField>
            <FormField id="campaign-start" label={`${t('admin.fields.startsAt')} (Asia/Baku)`} error={errors.startsAt}>
              <input id="campaign-start" className="input" type="datetime-local" value={editing.form.startsAt} onChange={(event) => set('startsAt', event.target.value)} />
            </FormField>
            <FormField id="campaign-end" label={`${t('admin.fields.endsAt')} (Asia/Baku)`} error={errors.endsAt}>
              <input id="campaign-end" className="input" type="datetime-local" value={editing.form.endsAt} onChange={(event) => set('endsAt', event.target.value)} />
            </FormField>
            <FormField id="campaign-bonus" label={t('admin.fields.bonusPoints')} error={errors.bonusPointsPerUnit}>
              <input id="campaign-bonus" className="input" type="number" min={0} max={1000} value={editing.form.bonusPointsPerUnit} onChange={(event) => set('bonusPointsPerUnit', Number(event.target.value))} />
            </FormField>
            <FormField id="campaign-status" label={t('admin.status')}>
              <select id="campaign-status" className="select" value={editing.form.status} onChange={(event) => set('status', event.target.value as CampaignForm['status'])}>
                <option value="ACTIVE">{t('admin.statuses.ACTIVE')}</option>
                <option value="INACTIVE">{t('admin.statuses.INACTIVE')}</option>
              </select>
            </FormField>
            <CheckField label={t('admin.fields.featured')} checked={editing.form.featured} onChange={(value) => set('featured', value)} />
            <fieldset className="stack" style={{ border: 'none', padding: 0, margin: 0, gap: 6 }}>
              <legend className="label" style={{ fontWeight: 600, marginBottom: 6 }}>
                {t('admin.fields.offers')}
              </legend>
              {offers.data?.map((offer) => (
                <CheckField key={offer.id} label={`${localized(offer.title)} (${offer.status})`} checked={editing.form.offerIds.includes(offer.id)} onChange={() => toggleOffer(offer.id)} />
              ))}
            </fieldset>
          </div>
        )}
      </Sheet>
    </>
  );
}
