import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { changeLanguage, type Language } from '../shared/i18n';

export type ThemeChoice = 'LIGHT' | 'DARK' | 'SYSTEM';

interface PreferencesContextValue {
  theme: ThemeChoice;
  language: Language;
  setTheme: (theme: ThemeChoice) => void;
  setLanguage: (language: Language) => void;
}

const PreferencesContext = createContext<PreferencesContextValue | null>(null);
const THEME_KEY = 'buta.theme';

function readTheme(): ThemeChoice {
  try {
    const value = localStorage.getItem(THEME_KEY);
    return value === 'LIGHT' || value === 'DARK' ? value : 'SYSTEM';
  } catch {
    return 'SYSTEM';
  }
}

function applyTheme(theme: ThemeChoice) {
  const dark = theme === 'DARK' || (theme === 'SYSTEM' && window.matchMedia('(prefers-color-scheme: dark)').matches);
  document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
}

export function PreferencesProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<ThemeChoice>(readTheme);
  const { i18n } = useTranslation();
  const queryClient = useQueryClient();
  const language = (i18n.language === 'en' ? 'en' : 'az') as Language;

  useEffect(() => {
    applyTheme(theme);
    if (theme !== 'SYSTEM') {
      return;
    }
    const media = window.matchMedia('(prefers-color-scheme: dark)');
    const listener = () => applyTheme('SYSTEM');
    media.addEventListener('change', listener);
    return () => media.removeEventListener('change', listener);
  }, [theme]);

  const setTheme = useCallback((next: ThemeChoice) => {
    try {
      localStorage.setItem(THEME_KEY, next);
    } catch {
      applyTheme(next);
    }
    setThemeState(next);
  }, []);

  const setLanguage = useCallback(
    (next: Language) => {
      if (next === language) {
        return;
      }
      changeLanguage(next);
      void queryClient.invalidateQueries();
    },
    [language, queryClient],
  );

  const value = useMemo(() => ({ theme, language, setTheme, setLanguage }), [theme, language, setTheme, setLanguage]);
  return <PreferencesContext.Provider value={value}>{children}</PreferencesContext.Provider>;
}

export function usePreferences() {
  const context = useContext(PreferencesContext);
  if (!context) {
    throw new Error('PreferencesProvider missing');
  }
  return context;
}
