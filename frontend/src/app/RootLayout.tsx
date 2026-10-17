import { useEffect, useRef } from 'react';
import { Outlet, ScrollRestoration, useSearchParams } from 'react-router';
import { PreferencesProvider, usePreferences } from './PreferencesProvider';
import { AuthProvider, useAuth } from '../features/auth/AuthProvider';
import { AppShell } from './AppShell';
import { rememberReferral } from '../shared/referral';

function AccountSync() {
  const { user } = useAuth();
  const { setLanguage, setTheme } = usePreferences();
  const synced = useRef<string | null>(null);
  const [params] = useSearchParams();
  const referral = params.get('ref');

  useEffect(() => {
    if (referral) {
      rememberReferral(referral);
    }
  }, [referral]);

  useEffect(() => {
    if (!user || synced.current === user.id) {
      return;
    }
    synced.current = user.id;
    if (user.locale === 'az' || user.locale === 'en') {
      setLanguage(user.locale);
    }
    if (user.theme !== 'SYSTEM') {
      setTheme(user.theme);
    }
  }, [user, setLanguage, setTheme]);

  return null;
}

export function RootLayout() {
  return (
    <PreferencesProvider>
      <AuthProvider>
        <AccountSync />
        <AppShell>
          <Outlet />
        </AppShell>
        <ScrollRestoration />
      </AuthProvider>
    </PreferencesProvider>
  );
}
