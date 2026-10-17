import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import { Award, ChevronRight, Heart, LogOut, Receipt, Settings, Shield, Store } from 'lucide-react';
import { apiUrl } from '../../shared/api/client';
import { useAuth } from '../auth/AuthProvider';

export default function AccountPage() {
  const { t } = useTranslation();
  const { user, isAdmin, isPlus, logout } = useAuth();
  if (!user) {
    return null;
  }
  const initials = `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase();
  const links = [
    { to: '/orders', icon: <Receipt size={20} />, label: t('nav.orders') },
    { to: '/rewards', icon: <Award size={20} />, label: t('nav.loyalty') },
    { to: '/favorites', icon: <Heart size={20} />, label: t('nav.favorites') },
    { to: '/account/settings', icon: <Settings size={20} />, label: t('nav.settings') },
    { to: '/account/privacy', icon: <Shield size={20} />, label: t('nav.privacy') },
  ];
  return (
    <div className="container page" style={{ maxWidth: 720 }}>
      <header className="card row" style={{ gap: 16, marginBottom: 24 }}>
        {user.avatarUrl ? <img className="avatar avatar-lg" src={apiUrl(user.avatarUrl)} alt="" /> : <span className="avatar avatar-lg">{initials}</span>}
        <div className="stack" style={{ gap: 4, minWidth: 0 }}>
          <h1 className="section-title">
            {user.firstName} {user.lastName}
          </h1>
          <span className="small muted" style={{ overflowWrap: 'anywhere' }}>
            @{user.username} · {user.email}
          </span>
          <span className={`badge ${isPlus ? 'badge-plus' : 'badge-soft'}`} style={{ alignSelf: 'flex-start' }}>
            {isPlus ? t('common.plus') : t('common.free')}
          </span>
        </div>
      </header>
      <nav className="card menu-list" style={{ padding: 0 }}>
        {links.map((link) => (
          <Link key={link.to} to={link.to}>
            {link.icon}
            {link.label}
            <ChevronRight size={18} className="chevron" aria-hidden="true" />
          </Link>
        ))}
        {isAdmin && (
          <Link to="/admin">
            <Store size={20} />
            {t('account.adminConsole')}
            <ChevronRight size={18} className="chevron" aria-hidden="true" />
          </Link>
        )}
      </nav>
      <div className="card menu-list" style={{ padding: 0, marginTop: 16 }}>
        <button type="button" onClick={() => logout()}>
          <LogOut size={20} />
          {t('common.signOut')}
        </button>
      </div>
    </div>
  );
}
