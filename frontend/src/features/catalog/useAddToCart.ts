import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { commerceApi } from '../../shared/api/endpoints';
import { useAuth } from '../auth/AuthProvider';
import { useToast } from '../../design-system/components/Toast';
import { errorMessage } from '../../design-system/components/States';

interface AddInput {
  offerId: string;
  quantity: number;
  recommendationRequestId?: string;
}

export function useAddToCart() {
  const { requireAuth } = useAuth();
  const queryClient = useQueryClient();
  const toast = useToast();
  const { t } = useTranslation();

  const mutation = useMutation({
    mutationFn: (input: AddInput) => commerceApi.addItem(input.offerId, input.quantity, input.recommendationRequestId),
    onSuccess: (cart) => {
      queryClient.setQueryData(['cart'], cart);
      toast.show(t('offer.added'));
    },
    onError: (error) => toast.show(errorMessage(error, t('errors.generic')), 'error'),
  });

  const add = (input: AddInput) => {
    if (requireAuth({ type: 'ADD_TO_CART', ...input })) {
      mutation.mutate(input);
    }
  };

  return { add, pending: mutation.isPending };
}
