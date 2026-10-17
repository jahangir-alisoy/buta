import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Plus } from 'lucide-react';
import { CardGridSkeleton, ErrorState, errorMessage } from '../../../design-system/components/States';
import { Sheet } from '../../../design-system/components/Sheet';
import { useToast } from '../../../design-system/components/Toast';
import { CategoryIcon } from '../../../design-system/components/CategoryIcon';
import { adminApi } from '../api';
import { AdminHeader, CheckField, FormField, LocalizedInput, emptyText, fieldErrors, useLocalized } from '../forms';
import type { AdminCategory, CategoryRequest } from '../types';

export default function AdminCategoriesPage() {
  const { t } = useTranslation();
  const toast = useToast();
  const localized = useLocalized();
  const queryClient = useQueryClient();
  const categories = useQuery({ queryKey: ['admin', 'categories'], queryFn: adminApi.categories, staleTime: 0 });
  const [editing, setEditing] = useState<{ id?: string; form: CategoryRequest } | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  const edit = (category?: AdminCategory) => {
    setErrors({});
    setEditing(
      category
        ? { id: category.id, form: { slug: category.slug, name: category.name, iconKey: category.iconKey, sortOrder: category.sortOrder, active: category.active } }
        : { form: { slug: '', name: emptyText(), iconKey: 'sparkles', sortOrder: 100, active: true } },
    );
  };

  const set = <K extends keyof CategoryRequest>(key: K, value: CategoryRequest[K]) => editing && setEditing({ ...editing, form: { ...editing.form, [key]: value } });

  const save = async () => {
    if (!editing) {
      return;
    }
    setBusy(true);
    setErrors({});
    try {
      await adminApi.saveCategory(editing.id, { ...editing.form, slug: editing.form.slug?.trim() || undefined });
      toast.show(t('admin.saved'));
      void queryClient.invalidateQueries({ queryKey: ['admin', 'categories'] });
      void queryClient.invalidateQueries({ queryKey: ['categories'] });
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
        title={t('admin.nav.categories')}
        action={
          <button type="button" className="btn btn-primary btn-sm" onClick={() => edit()}>
            <Plus size={16} aria-hidden="true" />
            {t('admin.new')}
          </button>
        }
      />
      {categories.isLoading && <CardGridSkeleton count={2} />}
      {categories.isError && <ErrorState error={categories.error} onRetry={() => categories.refetch()} />}
      {categories.data && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>{t('admin.fields.name')}</th>
                <th>{t('admin.fields.iconKey')}</th>
                <th>{t('admin.fields.sortOrder')}</th>
                <th>{t('admin.status')}</th>
                <th>{t('admin.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {categories.data.map((category) => (
                <tr key={category.id}>
                  <td>
                    {localized(category.name)}
                    <div className="caption tertiary">{category.slug}</div>
                  </td>
                  <td>
                    <span className="row" style={{ gap: 6 }}>
                      <CategoryIcon iconKey={category.iconKey} size={16} />
                      {category.iconKey}
                    </span>
                  </td>
                  <td>{category.sortOrder}</td>
                  <td>{category.active ? t('admin.statuses.ACTIVE') : t('admin.statuses.INACTIVE')}</td>
                  <td>
                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => edit(category)}>
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
            <LocalizedInput id="category-name" label={t('admin.fields.name')} value={editing.form.name} onChange={(value) => set('name', value)} maxLength={60} error={errors.name} />
            <FormField id="category-slug" label={t('admin.fields.slug')} hint={t('admin.fields.slugHint')} error={errors.slug}>
              <input id="category-slug" className="input" maxLength={60} value={editing.form.slug ?? ''} onChange={(event) => set('slug', event.target.value)} />
            </FormField>
            <FormField id="category-icon" label={t('admin.fields.iconKey')} hint="film, drama, music, palette, landmark, frame, book, bus, sparkles" error={errors.iconKey}>
              <input id="category-icon" className="input" maxLength={40} value={editing.form.iconKey} onChange={(event) => set('iconKey', event.target.value)} />
            </FormField>
            <FormField id="category-sort" label={t('admin.fields.sortOrder')} error={errors.sortOrder}>
              <input id="category-sort" className="input" type="number" min={0} max={1000} value={editing.form.sortOrder} onChange={(event) => set('sortOrder', Number(event.target.value))} />
            </FormField>
            <CheckField label={t('admin.fields.active')} checked={editing.form.active} onChange={(value) => set('active', value)} />
          </div>
        )}
      </Sheet>
    </>
  );
}
