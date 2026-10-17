import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Plus, Trash2 } from 'lucide-react';
import { ErrorState } from '../../../design-system/components/States';
import { useToast } from '../../../design-system/components/Toast';
import { PageLoader } from '../../../app/PageLoader';
import { adminApi } from '../api';
import { AdminHeader, CheckField, FormField, LocalizedInput, StatusPill, emptyText, fieldErrors, fromLocalInput, optionalNumber, toLocalInput, useLocalized } from '../forms';
import type { AdminOffer, LocalizedText, OfferRequest, PriceBreakdown } from '../types';
import { MarginPreview } from './MarginPreview';
import { publishFailureText } from './AdminOffersPage';

interface ComponentDraft {
  productId: string;
  quantity: string;
  negotiatedUnitCost: string;
}

interface OfferForm {
  slug: string;
  offerType: 'SINGLE' | 'BUNDLE';
  primaryCategoryId: string;
  title: LocalizedText;
  subtitle: LocalizedText;
  description: LocalizedText;
  imagePath: string;
  sellingPrice: string;
  visibility: 'PUBLIC' | 'PLUS_ONLY';
  saleStartsAt: string;
  saleEndsAt: string;
  maxPerOrder: string;
  businessPriority: string;
  featured: boolean;
  tags: string;
  components: ComponentDraft[];
}

function emptyForm(): OfferForm {
  const start = new Date();
  const end = new Date(start.getTime() + 30 * 24 * 3600 * 1000);
  return {
    slug: '',
    offerType: 'SINGLE',
    primaryCategoryId: '',
    title: emptyText(),
    subtitle: emptyText(),
    description: emptyText(),
    imagePath: '',
    sellingPrice: '',
    visibility: 'PUBLIC',
    saleStartsAt: toLocalInput(start.toISOString()),
    saleEndsAt: toLocalInput(end.toISOString()),
    maxPerOrder: '4',
    businessPriority: '50',
    featured: false,
    tags: '',
    components: [{ productId: '', quantity: '1', negotiatedUnitCost: '' }],
  };
}

function toForm(offer: AdminOffer): OfferForm {
  return {
    slug: offer.slug,
    offerType: offer.offerType,
    primaryCategoryId: offer.primaryCategoryId,
    title: offer.title,
    subtitle: offer.subtitle,
    description: offer.description,
    imagePath: offer.imagePath ?? '',
    sellingPrice: String(offer.sellingPrice),
    visibility: offer.visibility,
    saleStartsAt: toLocalInput(offer.saleStartsAt),
    saleEndsAt: toLocalInput(offer.saleEndsAt),
    maxPerOrder: String(offer.maxPerOrder),
    businessPriority: String(offer.businessPriority),
    featured: offer.featured,
    tags: offer.tags.join(', '),
    components: offer.components.map((component) => ({
      productId: component.productId,
      quantity: String(component.quantity),
      negotiatedUnitCost: component.negotiatedUnitCost === undefined || component.negotiatedUnitCost === null ? '' : String(component.negotiatedUnitCost),
    })),
  };
}

function components(form: OfferForm) {
  return form.components
    .filter((component) => component.productId)
    .map((component) => ({
      productId: component.productId,
      quantity: Math.max(1, Number(component.quantity) || 1),
      negotiatedUnitCost: optionalNumber(component.negotiatedUnitCost),
    }));
}

function toRequest(form: OfferForm): OfferRequest {
  return {
    slug: form.slug.trim() || undefined,
    offerType: form.offerType,
    primaryCategoryId: form.primaryCategoryId,
    title: form.title,
    subtitle: form.subtitle,
    description: form.description,
    imagePath: form.imagePath.trim() || undefined,
    sellingPrice: Number(form.sellingPrice),
    visibility: form.visibility,
    saleStartsAt: fromLocalInput(form.saleStartsAt) ?? '',
    saleEndsAt: fromLocalInput(form.saleEndsAt) ?? '',
    maxPerOrder: Number(form.maxPerOrder),
    businessPriority: Number(form.businessPriority),
    featured: form.featured,
    tags: form.tags
      .split(',')
      .map((tag) => tag.trim().toLowerCase())
      .filter(Boolean),
    components: components(form),
  };
}

export default function AdminOfferEditorPage() {
  const { offerId = 'new' } = useParams();
  const isNew = offerId === 'new';
  const { t } = useTranslation();
  const toast = useToast();
  const navigate = useNavigate();
  const localized = useLocalized();
  const queryClient = useQueryClient();
  const offer = useQuery({ queryKey: ['admin', 'offer', offerId], queryFn: () => adminApi.offer(offerId), enabled: !isNew, staleTime: 0 });
  const categories = useQuery({ queryKey: ['admin', 'categories'], queryFn: adminApi.categories });
  const products = useQuery({ queryKey: ['admin', 'products'], queryFn: adminApi.products });
  const [form, setForm] = useState<OfferForm | null>(isNew ? emptyForm() : null);
  const [preview, setPreview] = useState<PriceBreakdown | undefined>();
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (offer.data) {
      setForm(toForm(offer.data));
      setPreview(offer.data.breakdown);
    }
  }, [offer.data]);

  const previewKey = form ? JSON.stringify([form.sellingPrice, components(form)]) : '';
  useEffect(() => {
    if (!form) {
      return;
    }
    const price = Number(form.sellingPrice);
    const items = components(form);
    if (!(price > 0) || items.length === 0) {
      setPreview(undefined);
      return;
    }
    const timer = window.setTimeout(async () => {
      try {
        setPreview(await adminApi.marginPreview({ offerId: isNew ? undefined : offerId, sellingPrice: price, components: items }));
      } catch {
        setPreview(undefined);
      }
    }, 400);
    return () => window.clearTimeout(timer);
  }, [previewKey]);

  if (!isNew && offer.isLoading) {
    return <PageLoader />;
  }
  if (offer.isError) {
    return <ErrorState error={offer.error} onRetry={() => offer.refetch()} />;
  }
  if (!form) {
    return <PageLoader />;
  }

  const set = <K extends keyof OfferForm>(key: K, value: OfferForm[K]) => setForm({ ...form, [key]: value });
  const setComponent = (index: number, patch: Partial<ComponentDraft>) =>
    set(
      'components',
      form.components.map((component, position) => (position === index ? { ...component, ...patch } : component)),
    );

  const save = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setErrors({});
    try {
      const saved = await adminApi.saveOffer(isNew ? undefined : offerId, toRequest(form));
      toast.show(t('admin.saved'));
      void queryClient.invalidateQueries({ queryKey: ['admin', 'offers'] });
      void queryClient.invalidateQueries({ queryKey: ['offers'] });
      void queryClient.invalidateQueries({ queryKey: ['home'] });
      queryClient.setQueryData(['admin', 'offer', saved.id], saved);
      if (isNew) {
        navigate(`/admin/offers/${saved.id}`, { replace: true });
      } else {
        setForm(toForm(saved));
      }
    } catch (error) {
      setErrors(fieldErrors(error));
      toast.show(publishFailureText(error, t, t('errors.generic')), 'error');
    } finally {
      setBusy(false);
    }
  };

  const changeStatus = async (action: 'publish' | 'pause' | 'archive') => {
    try {
      const updated = await adminApi.changeOfferStatus(offerId, action);
      queryClient.setQueryData(['admin', 'offer', offerId], updated);
      void queryClient.invalidateQueries({ queryKey: ['admin', 'offers'] });
      toast.show(t('admin.saved'));
    } catch (error) {
      toast.show(publishFailureText(error, t, t('errors.generic')), 'error');
    }
  };

  return (
    <form onSubmit={save} noValidate>
      <Link to="/admin/offers" className="btn btn-ghost btn-sm" style={{ marginBottom: 8 }}>
        <ArrowLeft size={16} aria-hidden="true" />
        {t('admin.nav.offers')}
      </Link>
      <AdminHeader
        title={isNew ? t('admin.offers.newTitle') : localized(form.title) || t('admin.offers.editTitle')}
        action={
          offer.data && (
            <div className="row wrap" style={{ gap: 6 }}>
              <StatusPill status={offer.data.status} />
              {offer.data.status !== 'PUBLISHED' && offer.data.status !== 'ARCHIVED' && (
                <button type="button" className="btn btn-secondary btn-sm" onClick={() => changeStatus('publish')}>
                  {t('admin.publish')}
                </button>
              )}
              {offer.data.status === 'PUBLISHED' && (
                <button type="button" className="btn btn-secondary btn-sm" onClick={() => changeStatus('pause')}>
                  {t('admin.pause')}
                </button>
              )}
            </div>
          )
        }
      />
      <div className="detail-layout">
        <div className="card form-grid">
          <FormField id="offer-slug" label={t('admin.fields.slug')} hint={t('admin.fields.slugHint')} error={errors.slug}>
            <input id="offer-slug" className="input" value={form.slug} maxLength={100} onChange={(event) => set('slug', event.target.value)} />
          </FormField>
          <FormField id="offer-type" label={t('admin.fields.offerType')}>
            <select id="offer-type" className="select" value={form.offerType} onChange={(event) => set('offerType', event.target.value as OfferForm['offerType'])}>
              <option value="SINGLE">{t('offer.single')}</option>
              <option value="BUNDLE">{t('offer.bundle')}</option>
            </select>
          </FormField>
          <FormField id="offer-category" label={t('admin.fields.primaryCategory')} error={errors.primaryCategoryId}>
            <select id="offer-category" className="select" value={form.primaryCategoryId} onChange={(event) => set('primaryCategoryId', event.target.value)}>
              <option value="">—</option>
              {categories.data?.map((category) => (
                <option key={category.id} value={category.id}>
                  {localized(category.name)}
                </option>
              ))}
            </select>
          </FormField>
          <FormField id="offer-visibility" label={t('admin.fields.visibility')}>
            <select id="offer-visibility" className="select" value={form.visibility} onChange={(event) => set('visibility', event.target.value as OfferForm['visibility'])}>
              <option value="PUBLIC">PUBLIC</option>
              <option value="PLUS_ONLY">PLUS_ONLY</option>
            </select>
          </FormField>
          <LocalizedInput id="offer-title" label={t('admin.fields.title')} value={form.title} onChange={(value) => set('title', value)} maxLength={120} error={errors.title} />
          <LocalizedInput id="offer-subtitle" label={t('admin.fields.subtitle')} value={form.subtitle} onChange={(value) => set('subtitle', value)} error={errors.subtitle} />
          <LocalizedInput id="offer-description" label={t('admin.fields.description')} value={form.description} onChange={(value) => set('description', value)} multiline maxLength={4000} error={errors.description} />
          <FormField id="offer-image" label={t('admin.fields.imagePath')} error={errors.imagePath}>
            <input id="offer-image" className="input" value={form.imagePath} maxLength={300} placeholder="/images/demo/bundle-1.svg" onChange={(event) => set('imagePath', event.target.value)} />
          </FormField>
          <FormField id="offer-price" label={t('admin.fields.sellingPrice')} error={errors.sellingPrice}>
            <input id="offer-price" className="input" type="number" inputMode="decimal" min={0.01} step={0.01} value={form.sellingPrice} onChange={(event) => set('sellingPrice', event.target.value)} />
          </FormField>
          <FormField id="offer-start" label={`${t('admin.fields.saleStartsAt')} (Asia/Baku)`} error={errors.saleStartsAt}>
            <input id="offer-start" className="input" type="datetime-local" value={form.saleStartsAt} onChange={(event) => set('saleStartsAt', event.target.value)} />
          </FormField>
          <FormField id="offer-end" label={`${t('admin.fields.saleEndsAt')} (Asia/Baku)`} error={errors.saleEndsAt}>
            <input id="offer-end" className="input" type="datetime-local" value={form.saleEndsAt} onChange={(event) => set('saleEndsAt', event.target.value)} />
          </FormField>
          <FormField id="offer-max" label={t('admin.fields.maxPerOrder')} error={errors.maxPerOrder}>
            <input id="offer-max" className="input" type="number" min={1} max={10} value={form.maxPerOrder} onChange={(event) => set('maxPerOrder', event.target.value)} />
          </FormField>
          <FormField id="offer-priority" label={t('admin.fields.businessPriority')} error={errors.businessPriority}>
            <input id="offer-priority" className="input" type="number" min={0} max={100} value={form.businessPriority} onChange={(event) => set('businessPriority', event.target.value)} />
          </FormField>
          <FormField id="offer-tags" label={t('admin.fields.tags')} hint={t('admin.fields.tagsHint')} error={errors.tags}>
            <input id="offer-tags" className="input" value={form.tags} onChange={(event) => set('tags', event.target.value)} />
          </FormField>
          <div className="field" style={{ justifyContent: 'flex-end' }}>
            <CheckField label={t('admin.fields.featured')} checked={form.featured} onChange={(value) => set('featured', value)} />
          </div>
          <fieldset className="span-all stack" style={{ border: 'none', padding: 0, margin: 0, gap: 10 }}>
            <legend className="label" style={{ fontWeight: 600, marginBottom: 8 }}>
              {t('admin.fields.components')}
            </legend>
            {form.components.map((component, index) => (
              <div key={index} className="row wrap" style={{ alignItems: 'flex-end' }}>
                <label className="field" style={{ flex: '2 1 240px' }}>
                  <span className="caption tertiary">{t('admin.fields.product')}</span>
                  <select className="select" value={component.productId} onChange={(event) => setComponent(index, { productId: event.target.value })}>
                    <option value="">—</option>
                    {products.data?.map((product) => (
                      <option key={product.id} value={product.id}>
                        {localized(product.title)} · {product.partnerName} ({product.retailPrice} ₼)
                      </option>
                    ))}
                  </select>
                </label>
                <label className="field" style={{ flex: '0 1 90px' }}>
                  <span className="caption tertiary">{t('admin.fields.quantity')}</span>
                  <input className="input" type="number" min={1} max={10} value={component.quantity} onChange={(event) => setComponent(index, { quantity: event.target.value })} />
                </label>
                <label className="field" style={{ flex: '1 1 160px' }}>
                  <span className="caption tertiary">{t('admin.fields.negotiatedUnitCost')}</span>
                  <input className="input" type="number" min={0} step={0.01} value={component.negotiatedUnitCost} onChange={(event) => setComponent(index, { negotiatedUnitCost: event.target.value })} />
                </label>
                <button
                  type="button"
                  className="icon-btn"
                  aria-label={t('common.remove')}
                  disabled={form.components.length === 1}
                  onClick={() => set('components', form.components.filter((_, position) => position !== index))}
                >
                  <Trash2 size={18} />
                </button>
              </div>
            ))}
            {errors.components && <span className="field-error">{errors.components}</span>}
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              style={{ alignSelf: 'flex-start' }}
              disabled={form.components.length >= 6}
              onClick={() => set('components', [...form.components, { productId: '', quantity: '1', negotiatedUnitCost: '' }])}
            >
              <Plus size={16} aria-hidden="true" />
              {t('admin.offers.addComponent')}
            </button>
          </fieldset>
          <div className="span-all">
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? t('common.saving') : isNew ? t('admin.create') : t('common.save')}
            </button>
          </div>
        </div>
        <aside className="card stack detail-aside">
          <h2 className="section-title">{t('admin.offers.preview')}</h2>
          <MarginPreview breakdown={preview} />
        </aside>
      </div>
    </form>
  );
}
