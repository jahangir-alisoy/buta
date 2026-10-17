import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import az from './az.json';
import en from './en.json';
import { setApiLanguage } from '../api/client';

export type Language = 'az' | 'en';

const STORAGE_KEY = 'buta.locale';

function storedLanguage(): Language | null {
  try {
    const value = localStorage.getItem(STORAGE_KEY);
    return value === 'en' || value === 'az' ? value : null;
  } catch {
    return null;
  }
}

function detectLanguage(): Language {
  const stored = storedLanguage();
  if (stored) {
    return stored;
  }
  return navigator.language?.toLowerCase().startsWith('en') ? 'en' : 'az';
}

export function changeLanguage(language: Language) {
  try {
    localStorage.setItem(STORAGE_KEY, language);
  } catch {
    return applyLanguage(language);
  }
  applyLanguage(language);
}

function applyLanguage(language: Language) {
  document.documentElement.setAttribute('lang', language);
  setApiLanguage(language);
  void i18n.changeLanguage(language);
}

const initial = detectLanguage();
setApiLanguage(initial);
document.documentElement.setAttribute('lang', initial);

void i18n.use(initReactI18next).init({
  resources: { az: { translation: az }, en: { translation: en } },
  lng: initial,
  fallbackLng: false,
  interpolation: { escapeValue: false },
  returnNull: false,
});

export default i18n;
