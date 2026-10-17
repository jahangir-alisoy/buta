import { NavLink, useLocation, useNavigate } from 'react-router';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../features/auth/AuthProvider';
import { useGlassLens } from '../design-system/components/useGlassLens';

const LINKS = [
  { to: '/explore', label: 'nav.explore' },
  { to: '/bundles', label: 'nav.bundles' },
  { to: '/events', label: 'nav.events' },
  { to: '/partners', label: 'nav.partners' },
  { to: '/for-you', label: 'nav.forYou' },
];

const ADMIN_LINK = { to: '/admin', label: 'nav.admin' };

export function MainNav() {
  const { t } = useTranslation();
  const { isAdmin } = useAuth();
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const links = isAdmin ? [...LINKS, ADMIN_LINK] : LINKS;
  const selected = links.findIndex((link) => pathname === link.to || pathname.startsWith(`${link.to}/`));
  const lens = useGlassLens<HTMLElement>(selected, (index) => navigate(links[index].to));

  return (
    <nav ref={lens.trackRef} className={`main-nav ${lens.trackClassName}`} aria-label={t('common.menu')} {...lens.handlers}>
      <span ref={lens.lensRef} className="glass-lens" aria-hidden="true" />
      {links.map((link, index) => (
        <NavLink
          key={link.to}
          to={link.to}
          draggable={false}
          onClick={lens.guard}
          className={({ isActive }) => [isActive && 'active', index === lens.hovered && 'is-under-lens'].filter(Boolean).join(' ')}
        >
          {t(link.label)}
        </NavLink>
      ))}
    </nav>
  );
}
