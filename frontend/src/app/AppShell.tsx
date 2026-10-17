import { useState, type FormEvent, type ReactNode } from 'react';
import { Link, NavLink, useNavigate, useNavigation } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Compass, House, Monitor, Moon, Search, ShoppingBag, Sparkles, Sun, User } from 'lucide-react';
import { BrandMark } from '../design-system/components/BrandMark';
import { catalogApi, commerceApi } from '../shared/api/endpoints';
import { useAuth } from '../features/auth/AuthProvider';
import { useAppearance } from '../features/account/useAppearance';
import type { ThemeChoice } from './PreferencesProvider';

const THEME_ORDER: ThemeChoice[] = ['SYSTEM', 'LIGHT', 'DARK'];

export function useCartCount(): number {
  const { isActive } = useAuth();
  const cart = useQuery({ queryKey: ['cart'], queryFn: commerceApi.cart, enabled: isActive, staleTime: 30_000 });
  return isActive ? (cart.data?.itemCount ?? 0) : 0;
}

function UserAvatar() {
  const { user } = useAuth();
  if (!user) {
    return null;
  }
  const initials = `${user.firstName?.[0] ?? user.email[0]}${user.lastName?.[0] ?? ''}`.toUpperCase();
  return user.avatarUrl ? <img className="avatar" src={user.avatarUrl} alt="" /> : <span className="avatar">{initials}</span>;
}

function HeaderSearch() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [value, setValue] = useState('');
  const submit = (event: FormEvent) => {
    event.preventDefault();
    const query = value.trim();
    navigate(query ? `/explore?q=${encodeURIComponent(query)}` : '/explore');
  };
  return (
    <form className="header-search" role="search" onSubmit={submit}>
      <label className="search-bar">
        <Search size={18} />
        <span className="visually-hidden">{t('common.search')}</span>
        <input type="search" value={value} onChange={(event) => setValue(event.target.value)} placeholder={t('home.searchPlaceholder')} />
      </label>
    </form>
  );
}

function ThemeButton() {
  const { t } = useTranslation();
  const { theme, changeTheme } = useAppearance();
  const next = THEME_ORDER[(THEME_ORDER.indexOf(theme) + 1) % THEME_ORDER.length];
  const label = { SYSTEM: t('common.themeSystem'), LIGHT: t('common.themeLight'), DARK: t('common.themeDark') }[theme];
  const Icon = theme === 'DARK' ? Moon : theme === 'LIGHT' ? Sun : Monitor;
  return (
    <button type="button" className="icon-btn" onClick={() => changeTheme(next)} aria-label={`${t('common.theme')}: ${label}`} title={label}>
      <Icon size={20} />
    </button>
  );
}

function LanguageButton() {
  const { t } = useTranslation();
  const { language, changeLanguage } = useAppearance();
  const next = language === 'az' ? 'en' : 'az';
  return (
    <button type="button" className="icon-btn language-btn" onClick={() => changeLanguage(next)} aria-label={`${t('common.language')}: ${next.toUpperCase()}`}>
      {next.toUpperCase()}
    </button>
  );
}

function NavigationProgress() {
  const navigation = useNavigation();
  return navigation.state === 'loading' ? <div className="nav-progress" role="progressbar" aria-label="…" /> : null;
}

export function AppShell({ children }: { children: ReactNode }) {
  const { t } = useTranslation();
  const { user, isActive, isAdmin } = useAuth();
  const config = useQuery({ queryKey: ['config'], queryFn: catalogApi.config, staleTime: Infinity });
  const cartCount = useCartCount();
  const navClass = ({ isActive: active }: { isActive: boolean }) => (active ? 'active' : undefined);

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main">
        {t('common.skipToContent')}
      </a>
      <NavigationProgress />
      {config.data?.demoMode && <div className="demo-banner">{t('common.demoBanner')}</div>}
      <header className="app-header glass">
        <div className="container app-header-inner">
          <Link to="/" className="brand" aria-label={t('common.appName')}>
            <BrandMark />
            <span>{t('common.appName')}</span>
          </Link>
          <nav className="main-nav" aria-label={t('common.menu')}>
            <NavLink to="/explore" className={navClass}>
              {t('nav.explore')}
            </NavLink>
            <NavLink to="/bundles" className={navClass}>
              {t('nav.bundles')}
            </NavLink>
            <NavLink to="/events" className={navClass}>
              {t('nav.events')}
            </NavLink>
            <NavLink to="/partners" className={navClass}>
              {t('nav.partners')}
            </NavLink>
            <NavLink to="/for-you" className={navClass}>
              {t('nav.forYou')}
            </NavLink>
            {isAdmin && (
              <NavLink to="/admin" className={navClass}>
                {t('nav.admin')}
              </NavLink>
            )}
          </nav>
          <HeaderSearch />
          <div className="header-actions">
            <Link to="/explore" className="icon-btn mobile-only" aria-label={t('common.search')}>
              <Search size={20} />
            </Link>
            <LanguageButton />
            <ThemeButton />
            <Link to="/cart" className="icon-btn desktop-only" aria-label={`${t('nav.cart')} (${cartCount})`} style={{ position: 'relative' }}>
              <ShoppingBag size={20} />
              {cartCount > 0 && <span className="cart-count">{cartCount}</span>}
            </Link>
            {user ? (
              <Link to={isActive ? '/account' : '/auth/complete-profile'} className="icon-btn desktop-only" aria-label={t('nav.account')}>
                <UserAvatar />
              </Link>
            ) : (
              <Link to="/auth" className="btn btn-primary btn-sm desktop-only">
                {t('common.signIn')}
              </Link>
            )}
          </div>
        </div>
      </header>
      <main id="main" className="app-main" tabIndex={-1}>
        {children}
      </main>
      <footer className="app-footer">
        <div className="container stack">
          <nav className="footer-links" aria-label={t('nav.about')}>
            <Link to="/about">{t('nav.about')}</Link>
            <Link to="/how-it-works">{t('nav.howItWorks')}</Link>
            <Link to="/partners">{t('nav.partners')}</Link>
            <Link to="/terms">{t('nav.terms')}</Link>
            <Link to="/privacy-policy">{t('nav.privacyPolicy')}</Link>
          </nav>
          <p className="caption tertiary">
            © {new Date().getFullYear()} {t('common.appName')} · {t('common.tagline')}
          </p>
        </div>
      </footer>
      <nav className="tab-bar glass" aria-label={t('common.menu')}>
        <NavLink to="/" end className={navClass}>
          <House size={22} />
          {t('nav.home')}
        </NavLink>
        <NavLink to="/explore" className={navClass}>
          <Compass size={22} />
          {t('nav.explore')}
        </NavLink>
        <NavLink to="/for-you" className={navClass}>
          <Sparkles size={22} />
          {t('nav.forYou')}
        </NavLink>
        <NavLink to="/cart" className={navClass}>
          <ShoppingBag size={22} />
          {cartCount > 0 && <span className="cart-count">{cartCount}</span>}
          {t('nav.cart')}
        </NavLink>
        <NavLink to={user ? '/account' : '/auth'} className={navClass}>
          <User size={22} />
          {t('nav.account')}
        </NavLink>
      </nav>
    </div>
  );
}
