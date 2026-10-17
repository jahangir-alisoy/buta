import type { Preferences, PreferencesUpdate } from '../../shared/api/types';

export function preferencesUpdate(current: Preferences, patch: Partial<PreferencesUpdate>): PreferencesUpdate {
  return {
    locale: current.locale,
    theme: current.theme,
    personalizationEnabled: current.personalizationDecided ? current.personalizationEnabled : undefined,
    preferredCategoryIds: current.preferredCategoryIds,
    budgetMin: current.budgetMin,
    budgetMax: current.budgetMax,
    city: current.city,
    ...patch,
  };
}
