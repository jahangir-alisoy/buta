import { useCallback } from 'react';
import { usePreferences, type ThemeChoice } from '../../app/PreferencesProvider';
import { accountApi } from '../../shared/api/endpoints';
import type { Language } from '../../shared/i18n';
import { useAuth } from '../auth/AuthProvider';
import { preferencesUpdate } from './preferencesUpdate';

export function useAppearance() {
  const { theme, language, setTheme, setLanguage } = usePreferences();
  const { isActive } = useAuth();

  const persist = useCallback(
    async (patch: { locale?: Language; theme?: ThemeChoice }) => {
      if (!isActive) {
        return;
      }
      try {
        const current = await accountApi.preferences();
        await accountApi.updatePreferences(preferencesUpdate(current, patch));
      } catch {
        return;
      }
    },
    [isActive],
  );

  const changeLanguage = useCallback(
    (next: Language) => {
      setLanguage(next);
      void persist({ locale: next });
    },
    [persist, setLanguage],
  );

  const changeTheme = useCallback(
    (next: ThemeChoice) => {
      setTheme(next);
      void persist({ theme: next });
    },
    [persist, setTheme],
  );

  return { theme, language, changeLanguage, changeTheme };
}
