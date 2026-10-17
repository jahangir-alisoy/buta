import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuth } from '../features/auth/AuthProvider';
import { PageLoader } from './PageLoader';
import NotFoundPage from '../features/info/NotFoundPage';

export function RequireActive() {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) {
    return <PageLoader />;
  }
  const returnTo = encodeURIComponent(`${location.pathname}${location.search}`);
  if (!user) {
    return <Navigate to={`/auth?returnTo=${returnTo}`} replace />;
  }
  if (user.status === 'PROFILE_INCOMPLETE') {
    return <Navigate to={`/auth/complete-profile?returnTo=${returnTo}`} replace />;
  }
  return <Outlet />;
}

export function RequireAdmin() {
  const { user, loading, isAdmin } = useAuth();
  const location = useLocation();
  if (loading) {
    return <PageLoader />;
  }
  if (!user) {
    return <Navigate to={`/auth?returnTo=${encodeURIComponent(location.pathname)}`} replace />;
  }
  return isAdmin ? <Outlet /> : <NotFoundPage />;
}
