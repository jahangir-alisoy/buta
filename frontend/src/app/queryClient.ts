import { QueryClient } from '@tanstack/react-query';
import { ApiError } from '../shared/api/client';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60_000,
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => !(error instanceof ApiError && error.status < 500) && failureCount < 2,
    },
    mutations: {
      retry: false,
    },
  },
});
