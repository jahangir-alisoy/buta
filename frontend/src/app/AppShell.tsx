import type { ReactNode } from 'react';
import { Link, NavLink, useLocation, useNavigation } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Compass, House, Moon, Search, ShoppingBag, Sparkles, Sun, User } from 'lucide-react';
import { BrandMark } from '../design-system/components/BrandMark';
import { commerceApi } from '../shared/api/endpoints';
import { useAuth } from '../features/auth/AuthProvider';
import { useAppearance } from '../features/account/useAppearance';
import { UserAvatar } from '../features/account/UserAvatar';
import { LiveSearch } from '../features/catalog/LiveSearch';
import { MainNav } from './MainNav';

const SEARCH_PAGES = ['/explore', '/bundles', '/events', '/categories'];

export function useCartCount(): number {
  const { isActive } = useAuth();
  const cart = useQuery({ queryKey: ['cart'], queryFn: commerceApi.cart, enabled: isActive, staleTime: 30_000 });
  return isActive ? (cart.data?.itemCount ?? 0) : 0;
}

function ThemeButton() {
  const { t } = useTranslation();
  const { theme, changeTheme } = useAppearance();
  const dark = theme === 'DARK' || (theme === 'SYSTEM' && window.matchMedia('(prefers-color-scheme: dark)').matches);
  const label = dark ? t('common.themeLight') : t('common.themeDark');
  return (
    <button type="button" className="icon-btn" onClick={() => changeTheme(dark ? 'LIGHT' : 'DARK')} aria-label={`${t('common.theme')}: ${label}`} title={label}>
      {dark ? <Sun size={20} /> : <Moon size={20} />}
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
  const { user, isActive } = useAuth();
  const location = useLocation();
  const cartCount = useCartCount();
  const navClass = ({ isActive: active }: { isActive: boolean }) => (active ? 'active' : undefined);

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main">
        {t('common.skipToContent')}
      </a>
      <NavigationProgress />
      <header className="app-header glass">
        <div className="container app-header-inner">
          <Link to="/" className="brand" aria-label={t('common.appName')}>
            <BrandMark />
            <span>{t('common.appName')}</span>
          </Link>
          <MainNav />
          {!SEARCH_PAGES.some((prefix) => location.pathname.startsWith(prefix)) && <LiveSearch className="header-search" />}
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
          <span className="tab-label">{t('nav.home')}</span>
        </NavLink>
        <NavLink to="/explore" className={navClass}>
          <Compass size={22} />
          <span className="tab-label">{t('nav.explore')}</span>
        </NavLink>
        <NavLink to="/for-you" className={navClass}>
          <Sparkles size={22} />
          <span className="tab-label">{t('nav.forYou')}</span>
        </NavLink>
        <NavLink to="/cart" className={navClass}>
          <ShoppingBag size={22} />
          {cartCount > 0 && <span className="cart-count">{cartCount}</span>}
          <span className="tab-label">{t('nav.cart')}</span>
        </NavLink>
        <NavLink to={user ? '/account' : '/auth'} className={navClass}>
          <User size={22} />
          <span className="tab-label">{t('nav.account')}</span>
        </NavLink>
      </nav>
    </div>
  );
}
