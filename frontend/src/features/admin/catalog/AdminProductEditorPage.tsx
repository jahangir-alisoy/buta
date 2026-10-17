import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { ArrowLeft } from 'lucide-react';
import { ErrorState, errorMessage } from '../../../design-system/components/States';
import { useToast } from '../../../design-system/components/Toast';
import { PageLoader } from '../../../app/PageLoader';
import { adminApi } from '../api';
import { AdminHeader, CheckField, FormField, LocalizedInput, emptyText, fieldErrors, fromLocalInput, optionalNumber, optionalText, toLocalInput, useLocalized } from '../forms';
import type { AdminProduct, LocalizedText, ProductRequest } from '../types';

const PRODUCT_TYPES = ['EVENT_TICKET', 'ADMISSION', 'COUPON', 'TRANSPORT_PASS', 'DIGITAL_SUBSCRIPTION', 'SERVICE'];
const REDEMPTION_METHODS = ['VOUCHER_CODE', 'SHOW_CODE_AT_VENUE', 'PARTNER_WEBSITE_CODE'];

interface ProductForm {
  partnerId: string;
  categoryId: string;
  productType: string;
  title: LocalizedText;
  description: LocalizedText;
  terms: LocalizedText;
  redemptionInstructions: LocalizedText;
  eligibilityNote: LocalizedText;
  retailPrice: string;
  costModel: 'WHOLESALE' | 'COMMISSION';
  unitNetCost: string;
  commissionPercent: string;
  validFrom: string;
  validUntil: string;
  eventStartsAt: string;
  eventEndsAt: string;
  venueName: string;
  venueAddress: string;
  city: string;
  redemptionMethod: string;
  refundable: boolean;
  refundDeadlineHours: string;
  restockOnRefund: boolean;
  status: 'ACTIVE' | 'INACTIVE';
  initialInventory: string;
}

function emptyForm(): ProductForm {
  const now = new Date();
  return {
    partnerId: '',
    categoryId: '',
    productType: 'ADMISSION',
    title: emptyText(),
    description: emptyText(),
    terms: emptyText(),
    redemptionInstructions: emptyText(),
    eligibilityNote: emptyText(),
    retailPrice: '',
    costModel: 'WHOLESALE',
    unitNetCost: '',
    commissionPercent: '',
    validFrom: toLocalInput(now.toISOString()),
    validUntil: toLocalInput(new Date(now.getTime() + 90 * 24 * 3600 * 1000).toISOString()),
    eventStartsAt: '',
    eventEndsAt: '',
    venueName: '',
    venueAddress: '',
    city: 'Bakı',
    redemptionMethod: 'VOUCHER_CODE',
    refundable: true,
    refundDeadlineHours: '24',
    restockOnRefund: true,
    status: 'ACTIVE',
    initialInventory: '100',
  };
}

function toForm(product: AdminProduct): ProductForm {
  const text = (value: number | undefined | null) => (value === undefined || value === null ? '' : String(value));
  return {
    partnerId: product.partnerId,
    categoryId: product.categoryId,
    productType: product.productType,
    title: product.title,
    description: product.description,
    terms: product.terms,
    redemptionInstructions: product.redemptionInstructions,
    eligibilityNote: product.eligibilityNote ?? emptyText(),
    retailPrice: String(product.retailPrice),
    costModel: product.costModel,
    unitNetCost: text(product.unitNetCost),
    commissionPercent: text(product.commissionPercent),
    validFrom: toLocalInput(product.validFrom),
    validUntil: toLocalInput(product.validUntil),
    eventStartsAt: toLocalInput(product.eventStartsAt),
    eventEndsAt: toLocalInput(product.eventEndsAt),
    venueName: product.venueName ?? '',
    venueAddress: product.venueAddress ?? '',
    city: product.city,
    redemptionMethod: product.redemptionMethod,
    refundable: product.refundable,
    refundDeadlineHours: String(product.refundDeadlineHours),
    restockOnRefund: product.restockOnRefund,
    status: product.status,
    initialInventory: '',
  };
}

function toRequest(form: ProductForm, isNew: boolean): ProductRequest {
  return {
    partnerId: form.partnerId,
    categoryId: form.categoryId,
    productType: form.productType,
    title: form.title,
    description: form.description,
    terms: form.terms,
    redemptionInstructions: form.redemptionInstructions,
    eligibilityNote: optionalText(form.eligibilityNote),
    retailPrice: Number(form.retailPrice),
    costModel: form.costModel,
    unitNetCost: form.costModel === 'WHOLESALE' ? optionalNumber(form.unitNetCost) : undefined,
    commissionPercent: form.costModel === 'COMMISSION' ? optionalNumber(form.commissionPercent) : undefined,
    validFrom: fromLocalInput(form.validFrom) ?? '',
    validUntil: fromLocalInput(form.validUntil) ?? '',
    eventStartsAt: fromLocalInput(form.eventStartsAt),
    eventEndsAt: fromLocalInput(form.eventEndsAt),
    venueName: form.venueName.trim() || undefined,
    venueAddress: form.venueAddress.trim() || undefined,
    city: form.city.trim(),
    redemptionMethod: form.redemptionMethod,
    refundable: form.refundable,
    refundDeadlineHours: Number(form.refundDeadlineHours) || 0,
    restockOnRefund: form.restockOnRefund,
    status: form.status,
    initialInventory: isNew ? optionalNumber(form.initialInventory) : undefined,
  };
}

export default function AdminProductEditorPage() {
  const { productId = 'new' } = useParams();
  const isNew = productId === 'new';
  const { t } = useTranslation();
  const toast = useToast();
  const navigate = useNavigate();
  const localized = useLocalized();
  const queryClient = useQueryClient();
  const product = useQuery({ queryKey: ['admin', 'product', productId], queryFn: () => adminApi.product(productId), enabled: !isNew, staleTime: 0 });
  const partners = useQuery({ queryKey: ['admin', 'partners'], queryFn: adminApi.partners });
  const categories = useQuery({ queryKey: ['admin', 'categories'], queryFn: adminApi.categories });
  const [form, setForm] = useState<ProductForm | null>(isNew ? emptyForm() : null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (product.data) {
      setForm(toForm(product.data));
    }
  }, [product.data]);

  if (!isNew && product.isLoading) {
    return <PageLoader />;
  }
  if (product.isError) {
    return <ErrorState error={product.error} onRetry={() => product.refetch()} />;
  }
  if (!form) {
    return <PageLoader />;
  }

  const set = <K extends keyof ProductForm>(key: K, value: ProductForm[K]) => setForm({ ...form, [key]: value });
  const input = (key: keyof ProductForm, label: string, type = 'text', extra: Record<string, unknown> = {}) => (
    <FormField id={`product-${key}`} label={label} error={errors[key]}>
      <input
        id={`product-${key}`}
        className="input"
        type={type}
        value={form[key] as string}
        onChange={(event) => set(key, event.target.value as never)}
        {...extra}
      />
    </FormField>
  );

  const save = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setErrors({});
    try {
      const saved = await adminApi.saveProduct(isNew ? undefined : productId, toRequest(form, isNew));
      toast.show(t('admin.saved'));
      void queryClient.invalidateQueries({ queryKey: ['admin', 'products'] });
      queryClient.setQueryData(['admin', 'product', saved.id], saved);
      if (isNew) {
        navigate(`/admin/products/${saved.id}`, { replace: true });
      }
    } catch (error) {
      setErrors(fieldErrors(error));
      toast.show(errorMessage(error, t('errors.generic')), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <form onSubmit={save} noValidate>
      <Link to="/admin/products" className="btn btn-ghost btn-sm" style={{ marginBottom: 8 }}>
        <ArrowLeft size={16} aria-hidden="true" />
        {t('admin.nav.products')}
      </Link>
      <AdminHeader title={isNew ? t('admin.products.newTitle') : localized(form.title) || t('admin.products.editTitle')} />
      <div className="card form-grid">
        <FormField id="product-partner" label={t('admin.fields.partner')} error={errors.partnerId}>
          <select id="product-partner" className="select" value={form.partnerId} onChange={(event) => set('partnerId', event.target.value)}>
            <option value="">—</option>
            {partners.data?.map((partner) => (
              <option key={partner.id} value={partner.id}>
                {partner.name}
              </option>
            ))}
          </select>
        </FormField>
        <FormField id="product-category" label={t('admin.fields.category')} error={errors.categoryId}>
          <select id="product-category" className="select" value={form.categoryId} onChange={(event) => set('categoryId', event.target.value)}>
            <option value="">—</option>
            {categories.data?.map((category) => (
              <option key={category.id} value={category.id}>
                {localized(category.name)}
              </option>
            ))}
          </select>
        </FormField>
        <FormField id="product-type" label={t('admin.fields.productType')}>
          <select id="product-type" className="select" value={form.productType} onChange={(event) => set('productType', event.target.value)}>
            {PRODUCT_TYPES.map((type) => (
              <option key={type} value={type}>
                {t(`productTypes.${type}`)}
              </option>
            ))}
          </select>
        </FormField>
        <FormField id="product-status" label={t('admin.status')}>
          <select id="product-status" className="select" value={form.status} onChange={(event) => set('status', event.target.value as ProductForm['status'])}>
            <option value="ACTIVE">{t('admin.statuses.ACTIVE')}</option>
            <option value="INACTIVE">{t('admin.statuses.INACTIVE')}</option>
          </select>
        </FormField>
        <LocalizedInput id="product-title" label={t('admin.fields.title')} value={form.title} onChange={(value) => set('title', value)} maxLength={120} error={errors.title} />
        <LocalizedInput id="product-description" label={t('admin.fields.description')} value={form.description} onChange={(value) => set('description', value)} multiline maxLength={4000} error={errors.description} />
        <LocalizedInput id="product-terms" label={t('admin.fields.terms')} value={form.terms} onChange={(value) => set('terms', value)} multiline maxLength={4000} error={errors.terms} />
        <LocalizedInput
          id="product-redemption"
          label={t('admin.fields.redemptionInstructions')}
          value={form.redemptionInstructions}
          onChange={(value) => set('redemptionInstructions', value)}
          multiline
          maxLength={2000}
          error={errors.redemptionInstructions}
        />
        <LocalizedInput id="product-eligibility" label={`${t('admin.fields.eligibilityNote')} (${t('common.optional')})`} value={form.eligibilityNote} onChange={(value) => set('eligibilityNote', value)} />
        {input('retailPrice', t('admin.fields.retailPrice'), 'number', { min: 0.01, step: 0.01 })}
        <FormField id="product-cost-model" label={t('admin.fields.costModel')}>
          <select id="product-cost-model" className="select" value={form.costModel} onChange={(event) => set('costModel', event.target.value as ProductForm['costModel'])}>
            <option value="WHOLESALE">WHOLESALE</option>
            <option value="COMMISSION">COMMISSION</option>
          </select>
        </FormField>
        {form.costModel === 'WHOLESALE'
          ? input('unitNetCost', t('admin.fields.unitNetCost'), 'number', { min: 0, step: 0.01 })
          : input('commissionPercent', t('admin.fields.commissionPercent'), 'number', { min: 0, max: 100, step: 0.1 })}
        {input('validFrom', `${t('admin.fields.validFrom')} (Asia/Baku)`, 'datetime-local')}
        {input('validUntil', `${t('admin.fields.validUntil')} (Asia/Baku)`, 'datetime-local')}
        {input('eventStartsAt', `${t('admin.fields.eventStartsAt')} (${t('common.optional')})`, 'datetime-local')}
        {input('eventEndsAt', `${t('admin.fields.eventEndsAt')} (${t('common.optional')})`, 'datetime-local')}
        {input('venueName', t('admin.fields.venueName'), 'text', { maxLength: 120 })}
        {input('venueAddress', t('admin.fields.venueAddress'), 'text', { maxLength: 200 })}
        {input('city', t('admin.fields.city'), 'text', { maxLength: 60 })}
        <FormField id="product-method" label={t('admin.fields.redemptionMethod')}>
          <select id="product-method" className="select" value={form.redemptionMethod} onChange={(event) => set('redemptionMethod', event.target.value)}>
            {REDEMPTION_METHODS.map((method) => (
              <option key={method} value={method}>
                {t(`redemptionMethods.${method}`)}
              </option>
            ))}
          </select>
        </FormField>
        {input('refundDeadlineHours', t('admin.fields.refundDeadlineHours'), 'number', { min: 0, max: 720 })}
        {isNew && input('initialInventory', t('admin.fields.initialInventory'), 'number', { min: 0, max: 100000 })}
        <div className="field span-all row wrap">
          <CheckField label={t('admin.fields.refundable')} checked={form.refundable} onChange={(value) => set('refundable', value)} />
          <CheckField label={t('admin.fields.restockOnRefund')} checked={form.restockOnRefund} onChange={(value) => set('restockOnRefund', value)} />
        </div>
        <div className="span-all">
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy ? t('common.saving') : isNew ? t('admin.create') : t('common.save')}
          </button>
        </div>
      </div>
    </form>
  );
}
