import { api } from './client';
import type {
  Cart,
  Category,
  ClientConfig,
  CurrentUser,
  Home,
  LedgerEntry,
  LoyaltySummary,
  LoyaltyTask,
  OfferCard,
  OfferDetail,
  Order,
  OrderSummary,
  Page,
  PartnerDetail,
  PartnerSummary,
  PlusPlan,
  Preferences,
  PreferencesUpdate,
  Quote,
  Recommendations,
  ReferralInfo,
  SessionInfo,
  SignInResponse,
} from './types';

export interface OfferQuery {
  q?: string;
  category?: string;
  offerType?: string;
  productType?: string;
  partner?: string;
  city?: string;
  minPrice?: string;
  maxPrice?: string;
  eventFrom?: string;
  eventTo?: string;
  plusOnly?: boolean;
  includeSoldOut?: boolean;
  sort?: string;
  page?: number;
  size?: number;
}

function query(params: object): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '' && value !== false) {
      search.set(key, String(value));
    }
  });
  const text = search.toString();
  return text ? `?${text}` : '';
}

export const catalogApi = {
  config: () => api<ClientConfig>('/config'),
  home: () => api<Home>('/home'),
  categories: () => api<Category[]>('/categories'),
  partners: () => api<PartnerSummary[]>('/partners'),
  partner: (slug: string) => api<PartnerDetail>(`/partners/${encodeURIComponent(slug)}`),
  offers: (params: OfferQuery) => api<Page<OfferCard>>(`/offers${query(params)}`),
  offer: (slug: string) => api<OfferDetail>(`/offers/${encodeURIComponent(slug)}`),
  recommendations: (placement: string, limit = 12, offerId?: string) =>
    api<Recommendations>(`/recommendations${query({ placement, limit, offerId })}`),
  track: (events: { type: string; offerId?: string; categoryId?: string; query?: string; requestId?: string; placement?: string }[]) =>
    api<void>('/interactions', { method: 'POST', body: { events } }),
};

export const authApi = {
  session: () => api<SessionInfo>('/auth/session'),
  requestCode: (email: string) => api<{ resendAfterSeconds: number }>('/auth/otp/request', { method: 'POST', body: { email } }),
  verify: (email: string, code: string) => api<SignInResponse>('/auth/otp/verify', { method: 'POST', body: { email, code } }),
  register: (body: { email: string; firstName: string; lastName: string; username: string; referralCode?: string }) =>
    api<SignInResponse>('/auth/register', { method: 'POST', body }),
  guest: (kind: 'CUSTOMER' | 'ADMIN') => api<SignInResponse>('/auth/guest', { method: 'POST', body: { kind } }),
  completeProfile: (body: { firstName: string; lastName: string; username: string; referralCode?: string }) =>
    api<CurrentUser>('/auth/complete-profile', { method: 'POST', body }),
  usernameAvailability: (username: string) =>
    api<{ username: string; available: boolean }>(`/auth/username-availability${query({ username })}`),
  logout: () => api<void>('/auth/logout', { method: 'POST' }),
};

export const accountApi = {
  me: () => api<CurrentUser>('/me'),
  update: (body: { firstName: string; lastName: string; username: string; phone?: string }) =>
    api<CurrentUser>('/me', { method: 'PATCH', body }),
  preferences: () => api<Preferences>('/me/preferences'),
  updatePreferences: (body: PreferencesUpdate) => api<Preferences>('/me/preferences', { method: 'PUT', body }),
  uploadAvatar: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api<CurrentUser>('/me/avatar', { method: 'PUT', body: form });
  },
  removeAvatar: () => api<void>('/me/avatar', { method: 'DELETE' }),
  requestEmailChange: (newEmail: string) => api<{ resendAfterSeconds: number }>('/me/email-change', { method: 'POST', body: { newEmail } }),
  confirmEmailChange: (code: string) => api<CurrentUser>('/me/email-change/confirm', { method: 'POST', body: { code } }),
  resetPersonalization: () => api<void>('/me/personalization-data', { method: 'DELETE' }),
  favorites: () => api<OfferCard[]>('/me/favorites'),
  addFavorite: (offerId: string) => api<void>(`/me/favorites/${offerId}`, { method: 'PUT' }),
  removeFavorite: (offerId: string) => api<void>(`/me/favorites/${offerId}`, { method: 'DELETE' }),
  loyalty: () => api<LoyaltySummary>('/me/loyalty'),
  ledger: (page = 0) => api<Page<LedgerEntry>>(`/me/loyalty/ledger${query({ page, size: 20 })}`),
  tasks: () => api<LoyaltyTask[]>('/me/loyalty/tasks'),
  referral: () => api<ReferralInfo>('/me/referral'),
  plusPlan: () => api<PlusPlan>('/me/plus'),
  buyPlus: () => api<PlusPlan>('/me/plus/purchase', { method: 'POST' }),
};

export const commerceApi = {
  cart: () => api<Cart>('/cart'),
  addItem: (offerId: string, quantity: number, recommendationRequestId?: string) =>
    api<Cart>('/cart/items', { method: 'POST', body: { offerId, quantity, recommendationRequestId } }),
  updateItem: (itemId: string, quantity: number) => api<Cart>(`/cart/items/${itemId}`, { method: 'PATCH', body: { quantity } }),
  removeItem: (itemId: string) => api<Cart>(`/cart/items/${itemId}`, { method: 'DELETE' }),
  quote: (pointsToRedeem: number) => api<Quote>('/checkout/quote', { method: 'POST', body: { pointsToRedeem } }),
  checkout: (pointsToRedeem: number, idempotencyKey: string) =>
    api<Order>('/checkout', { method: 'POST', body: { pointsToRedeem }, headers: { 'Idempotency-Key': idempotencyKey } }),
  confirmSandbox: (paymentId: string, outcome: 'SUCCEEDED' | 'FAILED') =>
    api<Order>(`/payments/${paymentId}/sandbox-confirmation`, { method: 'POST', body: { outcome } }),
  orders: (page = 0) => api<Page<OrderSummary>>(`/orders${query({ page, size: 20 })}`),
  order: (id: string) => api<Order>(`/orders/${id}`),
  cancel: (id: string) => api<Order>(`/orders/${id}/cancel`, { method: 'POST' }),
  refund: (id: string, orderItemIds?: string[]) => api<Order>(`/orders/${id}/refund`, { method: 'POST', body: { orderItemIds } }),
};

export { query as toQuery };
