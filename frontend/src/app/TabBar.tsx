import type { ReactNode } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router';
import { useTranslation } from 'react-i18next';
import { Compass, House, ShoppingBag, Sparkles, User } from 'lucide-react';
import { useAuth } from '../features/auth/AuthProvider';
import { useGlassLens } from '../design-system/components/useGlassLens';

interface Tab {
  to: string;
  label: string;
  icon: ReactNode;
}

function isCurrent(pathname: string, to: string): boolean {
  return to === '/' ? pathname === '/' : pathname === to || pathname.startsWith(`${to}/`);
}

export function TabBar({ cartCount }: { cartCount: number }) {
  const { t } = useTranslation();
  const { user } = useAuth();
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const tabs: Tab[] = [
    { to: '/', label: t('nav.home'), icon: <House size={22} /> },
    { to: '/explore', label: t('nav.explore'), icon: <Compass size={22} /> },
    { to: '/for-you', label: t('nav.forYou'), icon: <Sparkles size={22} /> },
    { to: '/cart', label: t('nav.cart'), icon: <ShoppingBag size={22} /> },
    { to: user ? '/account' : '/auth', label: t('nav.account'), icon: <User size={22} /> },
  ];
  const selected = tabs.findIndex((tab) => isCurrent(pathname, tab.to));
  const lens = useGlassLens<HTMLElement>(selected, (index) => navigate(tabs[index].to));

  return (
    <nav ref={lens.trackRef} className={`tab-bar glass ${lens.trackClassName}`} aria-label={t('common.menu')} {...lens.handlers}>
      <span ref={lens.lensRef} className="glass-lens" aria-hidden="true" />
      {tabs.map((tab, index) => (
        <NavLink
          key={tab.to}
          to={tab.to}
          end={tab.to === '/'}
          draggable={false}
          onClick={lens.guard}
          className={({ isActive }) => [isActive && 'active', index === lens.hovered && 'is-under-lens'].filter(Boolean).join(' ')}
        >
          {tab.icon}
          {tab.to === '/cart' && cartCount > 0 && <span className="cart-count">{cartCount}</span>}
          <span className="tab-label">{tab.label}</span>
        </NavLink>
      ))}
    </nav>
  );
}
