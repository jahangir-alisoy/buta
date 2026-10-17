import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useLocation, useNavigate } from 'react-router';
import { authApi } from '../../shared/api/endpoints';
import { onUnauthorized } from '../../shared/api/client';
import type { CurrentUser } from '../../shared/api/types';
import { savePendingAction, type PendingAction } from '../../shared/pendingAction';

interface AuthContextValue {
  user: CurrentUser | null;
  loading: boolean;
  isActive: boolean;
  isAdmin: boolean;
  isPlus: boolean;
  setUser: (user: CurrentUser | null) => void;
  refresh: () => Promise<CurrentUser | null>;
  logout: () => Promise<void>;
  requireAuth: (action: PendingAction) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);
export const SESSION_KEY = ['session'];

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const location = useLocation();
  const session = useQuery({ queryKey: SESSION_KEY, queryFn: authApi.session, staleTime: 5 * 60_000 });
  const [override, setOverride] = useState<CurrentUser | null | undefined>(undefined);

  const user = override !== undefined ? override : session.data?.authenticated ? (session.data.user ?? null) : null;

  useEffect(() => {
    onUnauthorized(() => {
      setOverride(null);
      queryClient.setQueryData(SESSION_KEY, { authenticated: false });
    });
  }, [queryClient]);

  const setUser = useCallback(
    (next: CurrentUser | null) => {
      setOverride(next);
      queryClient.setQueryData(SESSION_KEY, next ? { authenticated: true, user: next } : { authenticated: false });
    },
    [queryClient],
  );

  const refresh = useCallback(async () => {
    const result = await authApi.session();
    const next = result.authenticated ? (result.user ?? null) : null;
    setUser(next);
    return next;
  }, [setUser]);

  const logout = useCallback(
    async () => {
      try {
        await authApi.logout();
      } finally {
        setUser(null);
        queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== 'session' });
        navigate('/');
      }
    },
    [navigate, queryClient, setUser],
  );

  const requireAuth = useCallback(
    (action: PendingAction) => {
      if (user?.status === 'ACTIVE') {
        return true;
      }
      const returnTo = `${location.pathname}${location.search}`;
      savePendingAction(action, returnTo);
      navigate(user?.status === 'PROFILE_INCOMPLETE' ? `/auth/complete-profile?returnTo=${encodeURIComponent(returnTo)}` : `/auth?returnTo=${encodeURIComponent(returnTo)}`);
      return false;
    },
    [location.pathname, location.search, navigate, user?.status],
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loading: session.isLoading && override === undefined,
      isActive: user?.status === 'ACTIVE',
      isAdmin: user?.status === 'ACTIVE' && user.roles.includes('ADMIN'),
      isPlus: user?.membership === 'PLUS',
      setUser,
      refresh,
      logout,
      requireAuth,
    }),
    [user, session.isLoading, override, setUser, refresh, logout, requireAuth],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('AuthProvider missing');
  }
  return context;
}
