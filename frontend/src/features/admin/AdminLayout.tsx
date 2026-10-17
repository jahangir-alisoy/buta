import { NavLink, Outlet } from 'react-router';
import { useTranslation } from 'react-i18next';

const SECTIONS = ['offers', 'products', 'partners', 'categories', 'campaigns', 'loyalty', 'users', 'orders', 'redeem', 'settings', 'audit', 'metrics'];

export default function AdminLayout() {
  const { t } = useTranslation();
  return (
    <div className="container page">
      <div className="admin-layout">
        <nav className="admin-nav" aria-label={t('admin.title')}>
          {SECTIONS.map((section) => (
            <NavLink key={section} to={`/admin/${section}`} className={({ isActive }) => (isActive ? 'active' : undefined)}>
              {t(`admin.nav.${section}`)}
            </NavLink>
          ))}
        </nav>
        <div style={{ minWidth: 0 }}>
          <Outlet />
        </div>
      </div>
    </div>
  );
}
