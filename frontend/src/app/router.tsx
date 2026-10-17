import type { ComponentType } from 'react';
import { createBrowserRouter, Navigate } from 'react-router';
import { RootLayout } from './RootLayout';
import { RequireActive, RequireAdmin } from './guards';
import { PageLoader } from './PageLoader';
import NotFoundPage from '../features/info/NotFoundPage';

const RELOAD_KEY = 'buta.chunkReload';

async function loadPage(loader: () => Promise<{ default: ComponentType }>) {
  try {
    const module = await loader();
    sessionStorage.removeItem(RELOAD_KEY);
    return module;
  } catch (error) {
    if (!sessionStorage.getItem(RELOAD_KEY)) {
      sessionStorage.setItem(RELOAD_KEY, '1');
      window.location.reload();
      return new Promise<never>(() => undefined);
    }
    throw error;
  }
}

function page(loader: () => Promise<{ default: ComponentType }>) {
  return async () => ({ Component: (await loadPage(loader)).default });
}

export const router = createBrowserRouter([
  {
    element: <RootLayout />,
    HydrateFallback: PageLoader,
    children: [
      { index: true, lazy: page(() => import('../features/catalog/HomePage')) },
      { path: 'explore', lazy: page(() => import('../features/catalog/ExplorePage')) },
      { path: 'bundles', lazy: page(() => import('../features/catalog/BundlesPage')) },
      { path: 'events', lazy: page(() => import('../features/catalog/EventsPage')) },
      { path: 'categories/:slug', lazy: page(() => import('../features/catalog/CategoryPage')) },
      { path: 'offers/:slug', lazy: page(() => import('../features/catalog/OfferDetailPage')) },
      { path: 'partners', lazy: page(() => import('../features/catalog/PartnersPage')) },
      { path: 'partners/:slug', lazy: page(() => import('../features/catalog/PartnerPage')) },
      { path: 'for-you', lazy: page(() => import('../features/recommendations/ForYouPage')) },
      { path: 'auth', lazy: page(() => import('../features/auth/SignInPage')) },
      { path: 'auth/complete-profile', lazy: page(() => import('../features/auth/CompleteProfilePage')) },
      { path: 'about', lazy: page(() => import('../features/info/AboutPage')) },
      { path: 'how-it-works', lazy: page(() => import('../features/info/HowItWorksPage')) },
      { path: 'terms', lazy: page(() => import('../features/info/TermsPage')) },
      { path: 'privacy-policy', lazy: page(() => import('../features/info/PrivacyPolicyPage')) },
      {
        element: <RequireActive />,
        children: [
          { path: 'cart', lazy: page(() => import('../features/commerce/CartPage')) },
          { path: 'checkout', lazy: page(() => import('../features/commerce/CheckoutPage')) },
          { path: 'checkout/pay/:orderId', lazy: page(() => import('../features/commerce/PaymentPage')) },
          { path: 'orders', lazy: page(() => import('../features/commerce/OrdersPage')) },
          { path: 'orders/:orderId', lazy: page(() => import('../features/commerce/OrderDetailPage')) },
          { path: 'account', lazy: page(() => import('../features/account/AccountPage')) },
          { path: 'account/settings', lazy: page(() => import('../features/account/SettingsPage')) },
          { path: 'account/privacy', lazy: page(() => import('../features/account/PrivacyPage')) },
          { path: 'favorites', lazy: page(() => import('../features/account/FavoritesPage')) },
          { path: 'rewards', lazy: page(() => import('../features/loyalty/LoyaltyPage')) },
        ],
      },
      {
        path: 'admin',
        element: <RequireAdmin />,
        children: [
          {
            lazy: page(() => import('../features/admin/AdminLayout')),
            children: [
              { index: true, element: <Navigate to="offers" replace /> },
              { path: 'offers', lazy: page(() => import('../features/admin/offers/AdminOffersPage')) },
              { path: 'offers/:offerId', lazy: page(() => import('../features/admin/offers/AdminOfferEditorPage')) },
              { path: 'products', lazy: page(() => import('../features/admin/catalog/AdminProductsPage')) },
              { path: 'products/:productId', lazy: page(() => import('../features/admin/catalog/AdminProductEditorPage')) },
              { path: 'partners', lazy: page(() => import('../features/admin/catalog/AdminPartnersPage')) },
              { path: 'categories', lazy: page(() => import('../features/admin/catalog/AdminCategoriesPage')) },
              { path: 'campaigns', lazy: page(() => import('../features/admin/catalog/AdminCampaignsPage')) },
              { path: 'loyalty', lazy: page(() => import('../features/admin/loyalty/AdminLoyaltyPage')) },
              { path: 'users', lazy: page(() => import('../features/admin/users/AdminUsersPage')) },
              { path: 'orders', lazy: page(() => import('../features/admin/orders/AdminOrdersPage')) },
              { path: 'redeem', lazy: page(() => import('../features/admin/orders/AdminRedeemPage')) },
              { path: 'settings', lazy: page(() => import('../features/admin/settings/AdminSettingsPage')) },
              { path: 'audit', lazy: page(() => import('../features/admin/insights/AdminAuditPage')) },
              { path: 'metrics', lazy: page(() => import('../features/admin/insights/AdminMetricsPage')) },
            ],
          },
        ],
      },
      { path: '*', Component: NotFoundPage },
    ],
  },
]);
