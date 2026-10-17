import { useEffect, useId, useRef, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Search, X } from 'lucide-react';
import { catalogApi } from '../../shared/api/endpoints';
import { formatMoney } from '../../shared/format';
import { track } from './track';

interface LiveSearchProps {
  className?: string;
  large?: boolean;
}

export function LiveSearch({ className, large }: LiveSearchProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const listId = useId();
  const wrapper = useRef<HTMLDivElement>(null);
  const [value, setValue] = useState('');
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const timer = window.setTimeout(() => setQuery(value.trim()), 250);
    return () => window.clearTimeout(timer);
  }, [value]);

  useEffect(() => {
    if (query.length >= 2) {
      track({ type: 'SEARCH', query });
    }
  }, [query]);

  useEffect(() => {
    const close = (event: MouseEvent) => {
      if (wrapper.current && !wrapper.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  const results = useQuery({
    queryKey: ['live-search', query],
    queryFn: () => catalogApi.offers({ q: query, size: 6 }),
    enabled: query.length >= 2,
    placeholderData: keepPreviousData,
    staleTime: 30_000,
  });

  const showAll = () => {
    const text = value.trim();
    setOpen(false);
    navigate(text ? `/explore?q=${encodeURIComponent(text)}` : '/explore');
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    showAll();
  };

  const reset = () => {
    setValue('');
    setQuery('');
    setOpen(false);
  };

  const items = results.data?.items ?? [];
  const visible = open && query.length >= 2;

  return (
    <div ref={wrapper} className={`live-search${large ? ' live-search-lg' : ''}${className ? ` ${className}` : ''}`}>
      <form role="search" onSubmit={submit}>
        <label className="search-bar">
          <Search size={18} />
          <span className="visually-hidden">{t('common.search')}</span>
          <input
            type="search"
            value={value}
            placeholder={t('home.searchPlaceholder')}
            autoComplete="off"
            aria-expanded={visible}
            aria-controls={listId}
            onFocus={() => setOpen(true)}
            onKeyDown={(event) => event.key === 'Escape' && setOpen(false)}
            onChange={(event) => {
              setValue(event.target.value);
              setOpen(true);
            }}
          />
          {value && (
            <button type="button" className="search-clear" onClick={reset} aria-label={t('common.clear')}>
              <X size={16} />
            </button>
          )}
        </label>
      </form>
      {visible && (
        <div className="live-results glass" id={listId}>
          {items.length === 0 && !results.isFetching && <p className="live-empty">{t('explore.empty')}</p>}
          {items.map((offer) => (
            <Link key={offer.id} to={`/offers/${offer.slug}`} className="live-item" onClick={() => setOpen(false)}>
              {offer.imagePath ? <img src={offer.imagePath} alt="" /> : <span className="live-thumb" />}
              <span className="live-text">
                <strong>{offer.title}</strong>
                <span>{offer.category.name}</span>
              </span>
              <span className="live-price">{formatMoney(offer.sellingPrice)}</span>
            </Link>
          ))}
          {items.length > 0 && (
            <button type="button" className="live-all" onClick={showAll}>
              {t('explore.seeAllResults', { count: results.data?.totalItems ?? items.length })}
            </button>
          )}
        </div>
      )}
    </div>
  );
}
