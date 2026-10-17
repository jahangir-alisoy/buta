import { useEffect, useState } from 'react';
import { authApi } from '../../shared/api/endpoints';

export const USERNAME_PATTERN = /^[a-z][a-z0-9._]{2,29}$/;

export type Availability = 'idle' | 'available' | 'taken';

export function useUsernameAvailability(username: string): Availability {
  const [availability, setAvailability] = useState<Availability>('idle');

  useEffect(() => {
    const value = username.trim().toLowerCase();
    if (!USERNAME_PATTERN.test(value)) {
      setAvailability('idle');
      return;
    }
    const timer = window.setTimeout(async () => {
      try {
        const result = await authApi.usernameAvailability(value);
        setAvailability(result.available ? 'available' : 'taken');
      } catch {
        setAvailability('idle');
      }
    }, 400);
    return () => window.clearTimeout(timer);
  }, [username]);

  return availability;
}
