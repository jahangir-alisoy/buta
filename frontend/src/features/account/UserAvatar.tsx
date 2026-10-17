import { Crown } from 'lucide-react';
import { apiUrl } from '../../shared/api/client';
import { useAuth } from '../auth/AuthProvider';

export function UserAvatar({ large = false }: { large?: boolean }) {
  const { user, isPlus } = useAuth();
  if (!user) {
    return null;
  }
  const initials = `${user.firstName?.[0] ?? user.email[0]}${user.lastName?.[0] ?? ''}`.toUpperCase();
  const size = large ? 'avatar avatar-lg' : 'avatar';
  return (
    <span className={`avatar-frame${large ? ' avatar-frame-lg' : ''}${isPlus ? ' is-plus' : ''}`}>
      {user.avatarUrl ? <img className={size} src={apiUrl(user.avatarUrl)} alt="" /> : <span className={size}>{initials}</span>}
      {isPlus && <Crown className="avatar-crown" aria-hidden="true" />}
    </span>
  );
}
