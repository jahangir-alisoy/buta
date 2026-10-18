export interface Money {
  amount: string;
  currency: string;
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface CategoryRef {
  id: string;
  slug: string;
  name: string;
  iconKey: string;
}

export interface PartnerRef {
  id: string;
  slug: string;
  name: string;
  demo: boolean;
}

export interface CampaignBadge {
  id: string;
  slug: string;
  name: string;
  audience: string;
  bonusPoints?: number;
  endsAt: string;
}

export type Availability = 'AVAILABLE' | 'LOW_STOCK' | 'SOLD_OUT';

export interface OfferCard {
  id: string;
  slug: string;
  offerType: 'SINGLE' | 'BUNDLE';
  title: string;
  subtitle: string;
  imagePath?: string;
  category: CategoryRef;
  partners: PartnerRef[];
  sellingPrice: Money;
  referenceRetail: Money;
  savings: Money;
  savingsPercent: string;
  plusOnly: boolean;
  availability: Availability;
  remaining?: number;
  endsAt: string;
  eventStartsAt?: string;
  cities: string[];
  purchasable: boolean;
  purchaseBlockReason?: string;
  favorite: boolean;
  pointsPreview?: number;
  componentCount: number;
  campaign?: CampaignBadge;
}

export interface OfferComponent {
  productId: string;
  title: string;
  description: string;
  partner: PartnerRef;
  category: CategoryRef;
  productType: string;
  quantity: number;
  retailPrice: Money;
  validFrom: string;
  validUntil: string;
  eventStartsAt?: string;
  eventEndsAt?: string;
  venueName?: string;
  venueAddress?: string;
  city: string;
  redemptionMethod: string;
  redemptionInstructions: string;
  terms: string;
  eligibilityNote?: string;
  refundable: boolean;
  refundDeadlineHours: number;
}

export interface OfferDetail {
  offer: OfferCard;
  description: string;
  tags: string[];
  components: OfferComponent[];
  validFrom: string;
  validUntil: string;
  maxPerOrder: number;
}

export interface Category extends CategoryRef {
  offerCount: number;
}

export interface PartnerSummary {
  id: string;
  slug: string;
  name: string;
  description: string;
  city: string;
  demo: boolean;
  logoPath?: string;
  offerCount: number;
}

export interface PartnerDetail {
  id: string;
  slug: string;
  name: string;
  description: string;
  city: string;
  address?: string;
  websiteUrl?: string;
  demo: boolean;
  logoPath?: string;
  offers: OfferCard[];
}

export interface CampaignSection {
  id: string;
  slug: string;
  name: string;
  bannerText: string;
  audience: string;
  bonusPoints: number;
  endsAt: string;
  offers: OfferCard[];
}

export interface Home {
  featuredCampaign?: CampaignSection;
  popular: OfferCard[];
  bestValueBundles: OfferCard[];
  thisWeek: OfferCard[];
  endingSoon: OfferCard[];
  categories: Category[];
}

export interface ClientConfig {
  locales: string[];
  defaultLocale: string;
  demoMode: boolean;
  paymentMode: string;
  currency: string;
  timeZone: string;
}

export interface CurrentUser {
  id: string;
  email: string;
  username?: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  status: 'PROFILE_INCOMPLETE' | 'ACTIVE' | 'SUSPENDED';
  roles: string[];
  avatarUrl?: string;
  membership: 'FREE' | 'PLUS';
  locale: string;
  theme: 'LIGHT' | 'DARK' | 'SYSTEM';
  personalizationEnabled: boolean;
  personalizationDecided: boolean;
  referralCode: string;
  emailVerified: boolean;
}

export interface SessionInfo {
  authenticated: boolean;
  user?: CurrentUser;
}

export interface SignInResponse {
  nextStep: 'DONE' | 'COMPLETE_PROFILE';
  user: CurrentUser;
}

export interface Preferences {
  locale: string;
  theme: 'LIGHT' | 'DARK' | 'SYSTEM';
  personalizationEnabled: boolean;
  personalizationDecided: boolean;
  preferredCategoryIds: string[];
  budgetMin?: number;
  budgetMax?: number;
  city?: string;
}

export interface PreferencesUpdate {
  locale: string;
  theme: 'LIGHT' | 'DARK' | 'SYSTEM';
  personalizationEnabled?: boolean;
  preferredCategoryIds: string[];
  budgetMin?: number;
  budgetMax?: number;
  city?: string;
}

export interface CartLine {
  id: string;
  offer?: OfferCard;
  quantity: number;
  unitPrice: Money;
  unitPriceAtAdd: Money;
  priceChanged: boolean;
  lineTotal: Money;
  issue?: string;
  maxQuantity: number;
}

export interface Cart {
  items: CartLine[];
  subtotal: Money;
  itemCount: number;
  checkoutReady: boolean;
  pointsPreview?: number;
}

export interface Quote {
  lines: { offerId: string; title: string; quantity: number; unitPrice: Money; lineTotal: Money }[];
  subtotal: Money;
  pointsToRedeem: number;
  pointsDiscount: Money;
  total: Money;
  pointsToEarn?: number;
  points: { allowed: boolean; available: number; maxRedeemable: number; step: number; minimum: number; pointValue: string };
  paymentMode: string;
}

export interface Voucher {
  code: string;
  status: string;
  productTitle?: string;
  partnerName?: string;
  validFrom: string;
  validUntil: string;
  eventStartsAt?: string;
  venueName?: string;
  redemptionMethod?: string;
  redemptionInstructions?: string;
}

export interface OrderItem {
  id: string;
  offerId: string;
  title: string;
  offerType: string;
  quantity: number;
  unitPrice: Money;
  cashPaid: Money;
  pointsEarned: number;
  refunded: boolean;
  refundable: boolean;
  refundBlockReason?: string;
  vouchers: Voucher[];
}

export interface Order {
  id: string;
  orderNumber: string;
  status: string;
  subtotal: Money;
  pointsRedeemed: number;
  pointsDiscount: Money;
  total: Money;
  pointsEarned: number;
  createdAt: string;
  expiresAt: string;
  fulfilledAt?: string;
  completedAt?: string;
  cancelledAt?: string;
  cancelReason?: string;
  payment?: { id: string; provider: string; mode: string; status: string; amount: Money; refunded: Money };
  items: OrderItem[];
  refunds: { amount: Money; initiatedBy: string; reason: string; createdAt: string }[];
  cancellable: boolean;
  awaitingPayment: boolean;
}

export interface OrderSummary {
  id: string;
  orderNumber: string;
  status: string;
  total: Money;
  itemCount: number;
  firstItemTitle?: string;
  createdAt: string;
}

export interface Tier {
  code: string;
  name: string;
  threshold: number;
  multiplier: string;
  benefits: string[];
}

export interface LoyaltySummary {
  plan: 'FREE' | 'PLUS';
  earnsPoints: boolean;
  availablePoints: number;
  lifetimeEarnedPoints: number;
  redeemedPoints: number;
  tierQualifyingPoints: number;
  tier: Tier;
  nextTier?: { code: string; name: string; threshold: number; pointsNeeded: number };
  tierProgressPercent: number;
  qualifyingSpend: { spent: Money; threshold: Money; remaining: Money; windowDays: number };
  redemptionBlocked: boolean;
  membershipEndsAt?: string;
  membershipSource: string;
  tiers: Tier[];
}

export interface StripeCheckout {
  url: string;
}

export interface PlusPlan {
  price: Money;
  periodDays: number;
  forSale: boolean;
  canPurchase: boolean;
  plan: 'FREE' | 'PLUS';
  source: string;
  endsAt?: string;
}

export interface LedgerEntry {
  id: string;
  type: string;
  points: number;
  descriptionKey: string;
  params: Record<string, string>;
  createdAt: string;
}

export interface LoyaltyTask {
  id: string;
  code: string;
  type: string;
  title: string;
  description: string;
  terms: string;
  progress: number;
  target: number;
  rewardPoints: number;
  period: string;
  periodKey: string;
  status: string;
  awarded: boolean;
  completedAt?: string;
  endsAt?: string;
}

export interface ReferralInfo {
  code: string;
  referrals: { status: string; createdAt: string; qualifiedAt?: string }[];
}

export interface RecommendationItem {
  offer: OfferCard;
  reasonCode?: string;
  reasonParams: Record<string, string>;
  exploration: boolean;
}

export interface Recommendations {
  requestId: string;
  placement: string;
  personalized: boolean;
  algorithmVersion: string;
  variant: string;
  items: RecommendationItem[];
}

export interface FieldError {
  field: string;
  code: string;
  message: string;
}
