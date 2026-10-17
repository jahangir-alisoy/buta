import { useTranslation } from 'react-i18next';
import { Check, X } from 'lucide-react';
import type { Availability } from './useUsernameAvailability';

export function AvailabilityMark({ availability }: { availability: Availability }) {
  const { t } = useTranslation();
  if (availability === 'idle') {
    return null;
  }
  const available = availability === 'available';
  return (
    <span className={`input-mark ${available ? 'is-ok' : 'is-bad'}`} role="img" aria-label={available ? t('auth.usernameAvailable') : t('auth.usernameTaken')}>
      {available ? <Check size={14} strokeWidth={3} aria-hidden="true" /> : <X size={14} strokeWidth={3} aria-hidden="true" />}
    </span>
  );
}
