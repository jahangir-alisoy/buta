import { useState } from 'react';
import { Link } from 'react-router';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Plus } from 'lucide-react';
import { formatMoney } from '../../../shared/format';
import { CardGridSkeleton, ErrorState, errorMessage } from '../../../design-system/components/States';
import { Sheet } from '../../../design-system/components/Sheet';
import { useToast } from '../../../design-system/components/Toast';
import { adminApi } from '../api';
import { AdminHeader, FormField, StatusPill, useLocalized } from '../forms';
import type { AdminProduct } from '../types';

export default function AdminProductsPage() {
  const { t } = useTranslation();
  const toast = useToast();
  const localized = useLocalized();
  const queryClient = useQueryClient();
  const products = useQuery({ queryKey: ['admin', 'products'], queryFn: adminApi.products, staleTime: 0 });
  const [adjusting, setAdjusting] = useState<AdminProduct | null>(null);
  const [delta, setDelta] = useState('');
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);

  const open = (product: AdminProduct) => {
    setAdjusting(product);
    setDelta('');
    setReason('');
  };

  const adjust = async () => {
    if (!adjusting) {
      return;
    }
    setBusy(true);
    try {
      await adminApi.adjustInventory(adjusting.id, Number(delta), reason.trim());
      toast.show(t('admin.products.adjusted'));
      void queryClient.invalidateQueries({ queryKey: ['admin', 'products'] });
      setAdjusting(null);
    } catch (error) {
      toast.show(errorMessage(error, t('errors.generic')), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <AdminHeader
        title={t('admin.nav.products')}
        action={
          <Link to="/admin/products/new" className="btn btn-primary btn-sm">
            <Plus size={16} aria-hidden="true" />
            {t('admin.new')}
          </Link>
        }
      />
      {products.isLoading && <CardGridSkeleton count={2} />}
      {products.isError && <ErrorState error={products.error} onRetry={() => products.refetch()} />}
      {products.data && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>{t('admin.fields.title')}</th>
                <th>{t('admin.fields.partner')}</th>
                <th>{t('admin.fields.retailPrice')}</th>
                <th>{t('admin.status')}</th>
                <th>{t('admin.fields.total')}</th>
                <th>{t('admin.fields.reserved')}</th>
                <th>{t('admin.fields.sold')}</th>
                <th>{t('admin.fields.available')}</th>
                <th>{t('admin.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {products.data.map((product) => (
                <tr key={product.id}>
                  <td>
                    <Link to={`/admin/products/${product.id}`} className="section-link">
                      {localized(product.title)}
                    </Link>
                    <div className="caption tertiary">{t(`productTypes.${product.productType}`)}</div>
                  </td>
                  <td>{product.partnerName}</td>
                  <td>{formatMoney(product.retailPrice)}</td>
                  <td>
                    <StatusPill status={product.status} />
                  </td>
                  <td>{product.totalQuantity}</td>
                  <td>{product.reservedQuantity}</td>
                  <td>{product.soldQuantity}</td>
                  <td>{product.available}</td>
                  <td>
                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => open(product)}>
                      {t('admin.products.adjust')}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Sheet
        open={adjusting !== null}
        title={t('admin.products.adjust')}
        onClose={() => setAdjusting(null)}
        footer={
          <button type="button" className="btn btn-primary btn-block" onClick={adjust} disabled={busy || !delta || Number(delta) === 0 || !reason.trim()}>
            {busy ? t('common.saving') : t('common.save')}
          </button>
        }
      >
        {adjusting && (
          <div className="stack">
            <p className="small muted">
              {localized(adjusting.title)} · {t('admin.fields.available')}: {adjusting.available}
            </p>
            <FormField id="adjust-delta" label={t('admin.fields.delta')}>
              <input id="adjust-delta" className="input" type="number" min={-10000} max={10000} value={delta} onChange={(event) => setDelta(event.target.value)} />
            </FormField>
            <FormField id="adjust-reason" label={t('admin.fields.reason')}>
              <input id="adjust-reason" className="input" maxLength={200} value={reason} onChange={(event) => setReason(event.target.value)} />
            </FormField>
          </div>
        )}
      </Sheet>
    </>
  );
}
