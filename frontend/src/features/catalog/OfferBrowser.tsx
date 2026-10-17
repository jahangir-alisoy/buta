import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { useSearchParams } from 'react-router';
import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { ArrowDownUp, Search, SlidersHorizontal } from 'lucide-react';
import { catalogApi, type OfferQuery } from '../../shared/api/endpoints';
import { CardGridSkeleton, EmptyState, ErrorState } from '../../design-system/components/States';
import { Sheet } from '../../design-system/components/Sheet';
import { Field, Segmented } from '../../design-system/components/Controls';
import { SelectMenu } from '../../design-system/components/SelectMenu';
import { OfferGrid } from './OfferCollection';
import { track } from './track';

const SORTS = ['RECOMMENDED', 'PRICE_ASC', 'PRICE_DESC', 'SAVINGS_DESC', 'ENDING_SOON', 'NEWEST', 'EVENT_DATE'];
const PRODUCT_TYPES = ['EVENT_TICKET', 'ADMISSION', 'COUPON', 'TRANSPORT_PASS', 'DIGITAL_SUBSCRIPTION', 'SERVICE'];
const FILTER_KEYS = ['offerType', 'productType', 'minPrice', 'maxPrice', 'plusOnly', 'includeSoldOut'] as const;
const PAGE_SIZE = 24;

type FilterKey = (typeof FILTER_KEYS)[number];
type FilterDraft = Record<FilterKey, string>;

interface OfferBrowserProps {
  title: string;
  subtitle?: string;
  preset?: OfferQuery;
  defaultSort?: string;
  showCategoryChips?: boolean;
  showTypeFilter?: boolean;
  header?: ReactNode;
}

function readDraft(params: URLSearchParams): FilterDraft {
  return Object.fromEntries(FILTER_KEYS.map((key) => [key, params.get(key) ?? ''])) as FilterDraft;
}

export function OfferBrowser({ title, subtitle, preset = {}, defaultSort = 'RECOMMENDED', showCategoryChips = false, showTypeFilter = true, header }: OfferBrowserProps) {
  const { t } = useTranslation();
  const [params, setParams] = useSearchParams();
  const [search, setSearch] = useState(params.get('q') ?? '');
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [draft, setDraft] = useState<FilterDraft>(() => readDraft(params));
  const categories = useQuery({ queryKey: ['categories'], queryFn: catalogApi.categories, enabled: showCategoryChips });

  const committedQuery = params.get('q') ?? '';
  const sort = params.get('sort') ?? defaultSort;

  const query: OfferQuery = useMemo(
    () => ({
      ...preset,
      q: committedQuery || undefined,
      category: preset.category ?? (params.get('category') || undefined),
      offerType: preset.offerType ?? (params.get('offerType') || undefined),
      productType: params.get('productType') || undefined,
      minPrice: params.get('minPrice') || undefined,
      maxPrice: params.get('maxPrice') || undefined,
      plusOnly: params.get('plusOnly') === 'true',
      includeSoldOut: params.get('includeSoldOut') === 'true',
      sort,
    }),
    [preset, committedQuery, params, sort],
  );

  const offers = useInfiniteQuery({
    queryKey: ['offers', query],
    queryFn: ({ pageParam }) => catalogApi.offers({ ...query, page: pageParam, size: PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
  });

  const update = (changes: Record<string, string | undefined>) => {
    const next = new URLSearchParams(params);
    Object.entries(changes).forEach(([key, value]) => (value ? next.set(key, value) : next.delete(key)));
    setParams(next, { replace: true });
  };

  useEffect(() => {
    setSearch((current) => (current.trim() === committedQuery ? current : committedQuery));
  }, [committedQuery]);

  useEffect(() => {
    const value = search.trim();
    if (value === committedQuery) {
      return;
    }
    const timer = window.setTimeout(() => {
      update({ q: value || undefined });
      if (value.length >= 2) {
        track({ type: 'SEARCH', query: value });
      }
    }, 250);
    return () => window.clearTimeout(timer);
  });

  const activeFilters = FILTER_KEYS.filter((key) => params.get(key)).length;
  const items = offers.data?.pages.flatMap((page) => page.items) ?? [];
  const total = offers.data?.pages[0]?.totalItems ?? 0;

  const openFilters = () => {
    setDraft(readDraft(params));
    setFiltersOpen(true);
  };

  const applyFilters = () => {
    update(Object.fromEntries(FILTER_KEYS.map((key) => [key, draft[key] || undefined])));
    setFiltersOpen(false);
  };

  const clearAll = () => {
    setSearch('');
    const next = new URLSearchParams();
    if (params.get('sort')) {
      next.set('sort', params.get('sort')!);
    }
    setParams(next, { replace: true });
    setFiltersOpen(false);
  };

  return (
    <div className="container page">
      <header className="page-header">
        <h1 className="page-title">{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
        {header}
      </header>
      <div className="toolbar">
        <label className="search-bar">
          <Search size={18} />
          <span className="visually-hidden">{t('common.search')}</span>
          <input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder={t('home.searchPlaceholder')} />
        </label>
        <button type="button" className="btn btn-secondary" onClick={openFilters}>
          <SlidersHorizontal size={18} aria-hidden="true" />
          {t('explore.filters')}
          {activeFilters > 0 && <span className="badge badge-accent">{activeFilters}</span>}
        </button>
        <SelectMenu
          label={t('explore.sort')}
          value={sort}
          onChange={(value) => update({ sort: value })}
          icon={<ArrowDownUp size={16} aria-hidden="true" />}
          options={SORTS.map((option) => ({ value: option, label: t(`explore.sortOptions.${option}`) }))}
        />
      </div>
      {showCategoryChips && categories.data && (
        <div className="chip-scroller" style={{ marginBottom: 16 }}>
          <button type="button" className={`chip${!params.get('category') ? ' is-selected' : ''}`} onClick={() => update({ category: undefined })}>
            {t('explore.allCategories')}
          </button>
          {categories.data.map((category) => (
            <button
              key={category.id}
              type="button"
              className={`chip${params.get('category') === category.slug ? ' is-selected' : ''}`}
              onClick={() => update({ category: category.slug })}
            >
              {category.name}
            </button>
          ))}
        </div>
      )}
      {offers.data && (
        <p className="muted small" style={{ marginBottom: 16 }} aria-live="polite">
          {t('explore.results', { count: total })}
        </p>
      )}
      {offers.isLoading && <CardGridSkeleton />}
      {offers.isError && <ErrorState error={offers.error} onRetry={() => offers.refetch()} />}
      {offers.data && items.length === 0 && (
        <EmptyState
          title={t('explore.empty')}
          text={t('explore.emptyHint')}
          action={
            <button type="button" className="btn btn-secondary" onClick={clearAll}>
              {t('explore.clearFilters')}
            </button>
          }
        />
      )}
      {items.length > 0 && <OfferGrid offers={items} />}
      {offers.hasNextPage && (
        <div className="row" style={{ justifyContent: 'center', marginTop: 24 }}>
          <button type="button" className="btn btn-secondary" onClick={() => offers.fetchNextPage()} disabled={offers.isFetchingNextPage}>
            {offers.isFetchingNextPage ? t('common.loading') : t('common.loadMore')}
          </button>
        </div>
      )}
      <Sheet
        open={filtersOpen}
        title={t('explore.filters')}
        onClose={() => setFiltersOpen(false)}
        footer={
          <>
            <button type="button" className="btn btn-ghost" onClick={clearAll}>
              {t('common.clear')}
            </button>
            <button type="button" className="btn btn-primary btn-block" onClick={applyFilters}>
              {t('common.apply')}
            </button>
          </>
        }
      >
        <div className="stack">
          {showTypeFilter && (
            <div className="field">
              <span className="label">{t('explore.type')}</span>
              <Segmented
                label={t('explore.type')}
                value={draft.offerType}
                onChange={(value) => setDraft({ ...draft, offerType: value })}
                options={[
                  { value: '', label: t('explore.all') },
                  { value: 'BUNDLE', label: t('explore.bundles') },
                  { value: 'SINGLE', label: t('explore.singles') },
                ]}
              />
            </div>
          )}
          <Field id="filter-product-type" label={t('explore.productType')}>
            <select id="filter-product-type" className="select" value={draft.productType} onChange={(event) => setDraft({ ...draft, productType: event.target.value })}>
              <option value="">{t('explore.anyType')}</option>
              {PRODUCT_TYPES.map((type) => (
                <option key={type} value={type}>
                  {t(`productTypes.${type}`)}
                </option>
              ))}
            </select>
          </Field>
          <div className="field">
            <span className="label">{t('explore.priceRange')}</span>
            <div className="row">
              <input
                className="input"
                type="number"
                inputMode="decimal"
                min={0}
                placeholder={t('explore.min')}
                aria-label={t('explore.min')}
                value={draft.minPrice}
                onChange={(event) => setDraft({ ...draft, minPrice: event.target.value })}
              />
              <input
                className="input"
                type="number"
                inputMode="decimal"
                min={0}
                placeholder={t('explore.max')}
                aria-label={t('explore.max')}
                value={draft.maxPrice}
                onChange={(event) => setDraft({ ...draft, maxPrice: event.target.value })}
              />
            </div>
          </div>
          <label className="checkbox">
            <input type="checkbox" checked={draft.plusOnly === 'true'} onChange={(event) => setDraft({ ...draft, plusOnly: event.target.checked ? 'true' : '' })} />
            {t('explore.plusOnly')}
          </label>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={draft.includeSoldOut === 'true'}
              onChange={(event) => setDraft({ ...draft, includeSoldOut: event.target.checked ? 'true' : '' })}
            />
            {t('explore.includeSoldOut')}
          </label>
        </div>
      </Sheet>
    </div>
  );
}
