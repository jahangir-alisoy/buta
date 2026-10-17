export interface LocalizedText {
  az: string;
  en: string;
}

export interface AdminCategory {
  id: string;
  slug: string;
  name: LocalizedText;
  iconKey: string;
  sortOrder: number;
  active: boolean;
}

export interface CategoryRequest {
  slug?: string;
  name: LocalizedText;
  iconKey: string;
  sortOrder: number;
  active: boolean;
}

export interface AdminPartner {
  id: string;
  slug: string;
  name: string;
  description: LocalizedText;
  city: string;
  address?: string;
  websiteUrl?: string;
  logoPath?: string;
  demo: boolean;
  status: 'ACTIVE' | 'INACTIVE';
}

export type PartnerRequest = Omit<AdminPartner, 'id' | 'slug'> & { slug?: string };

export interface ProductPerformance {
  productId: string;
  title: string;
  totalQuantity: number;
  soldQuantity: number;
  sellThroughPercent: string;
}

export interface PartnerPerformance {
  partnerId: string;
  partnerName: string;
  from: string;
  to: string;
  unitsSold: number;
  partnerPayout: number;
  refundedUnits: number;
  vouchersIssued: number;
  vouchersRedeemed: number;
  redemptionRatePercent: string;
  products: ProductPerformance[];
}

export interface AdminProduct {
  id: string;
  partnerId: string;
  partnerName: string;
  categoryId: string;
  productType: string;
  title: LocalizedText;
  description: LocalizedText;
  terms: LocalizedText;
  redemptionInstructions: LocalizedText;
  eligibilityNote?: LocalizedText;
  retailPrice: number;
  costModel: 'WHOLESALE' | 'COMMISSION';
  unitNetCost?: number;
  commissionPercent?: number;
  validFrom: string;
  validUntil: string;
  eventStartsAt?: string;
  eventEndsAt?: string;
  venueName?: string;
  venueAddress?: string;
  city: string;
  redemptionMethod: string;
  refundable: boolean;
  refundDeadlineHours: number;
  restockOnRefund: boolean;
  status: 'ACTIVE' | 'INACTIVE';
  totalQuantity: number;
  reservedQuantity: number;
  soldQuantity: number;
  available: number;
}

export interface ProductRequest {
  partnerId: string;
  categoryId: string;
  productType: string;
  title: LocalizedText;
  description: LocalizedText;
  terms: LocalizedText;
  redemptionInstructions: LocalizedText;
  eligibilityNote?: LocalizedText;
  retailPrice: number;
  costModel: 'WHOLESALE' | 'COMMISSION';
  unitNetCost?: number;
  commissionPercent?: number;
  validFrom: string;
  validUntil: string;
  eventStartsAt?: string;
  eventEndsAt?: string;
  venueName?: string;
  venueAddress?: string;
  city: string;
  redemptionMethod: string;
  refundable: boolean;
  refundDeadlineHours: number;
  restockOnRefund: boolean;
  status: 'ACTIVE' | 'INACTIVE';
  initialInventory?: number;
}

export interface MarginRule {
  code: string;
  threshold: number;
  passed: boolean;
}

export interface PriceBreakdown {
  sellingPrice: number;
  referenceRetail: number;
  savings: number;
  savingsPercent: number;
  partnerCostTotal: number;
  paymentFee: number;
  maxPointsEarned: number;
  expectedLoyaltyCost: number;
  otherVariableCost: number;
  contributionMargin: number;
  marginPercent: number;
  rules: MarginRule[];
  sellable: boolean;
}

export interface ComponentRequest {
  productId: string;
  quantity: number;
  negotiatedUnitCost?: number;
}

export interface AdminOffer {
  id: string;
  slug: string;
  offerType: 'SINGLE' | 'BUNDLE';
  status: 'DRAFT' | 'PUBLISHED' | 'PAUSED' | 'ARCHIVED';
  visibility: 'PUBLIC' | 'PLUS_ONLY';
  primaryCategoryId: string;
  title: LocalizedText;
  subtitle: LocalizedText;
  description: LocalizedText;
  imagePath?: string;
  sellingPrice: number;
  saleStartsAt: string;
  saleEndsAt: string;
  maxPerOrder: number;
  businessPriority: number;
  featured: boolean;
  tags: string[];
  components: { productId: string; productTitle: string; partnerName: string; quantity: number; negotiatedUnitCost?: number }[];
  breakdown: PriceBreakdown;
  available: number;
  publishedAt?: string;
}

export interface OfferRequest {
  slug?: string;
  offerType: 'SINGLE' | 'BUNDLE';
  primaryCategoryId: string;
  title: LocalizedText;
  subtitle: LocalizedText;
  description: LocalizedText;
  imagePath?: string;
  sellingPrice: number;
  visibility: 'PUBLIC' | 'PLUS_ONLY';
  saleStartsAt: string;
  saleEndsAt: string;
  maxPerOrder: number;
  businessPriority: number;
  featured: boolean;
  tags: string[];
  components: ComponentRequest[];
}

export interface AdminCampaign {
  id: string;
  slug: string;
  name: LocalizedText;
  bannerText: LocalizedText;
  audience: 'ALL' | 'PLUS';
  startsAt: string;
  endsAt: string;
  bonusPointsPerUnit: number;
  featured: boolean;
  status: 'ACTIVE' | 'INACTIVE';
  offerIds: string[];
}

export type CampaignRequest = Omit<AdminCampaign, 'id' | 'slug'> & { slug?: string };

export interface AdminUser {
  id: string;
  email: string;
  username?: string;
  firstName?: string;
  lastName?: string;
  status: string;
  roles: string[];
  plan: string;
  planSource?: string;
  planEndsAt?: string;
  availablePoints: number;
  tier?: string;
  flaggedForReview: boolean;
  createdAt: string;
}

export interface AdminTask {
  id: string;
  code: string;
  taskType: string;
  title: LocalizedText;
  description: LocalizedText;
  terms: LocalizedText;
  targetValue: number;
  rewardPoints: number;
  periodType: string;
  startsAt: string;
  endsAt?: string;
  active: boolean;
  minOrderAmount: number;
}

export type TaskRequest = Omit<AdminTask, 'id'>;

export interface AdminTier {
  code: string;
  name: LocalizedText;
  thresholdPoints: number;
  earnMultiplier: number;
  benefits: Record<string, string[]>;
}

export interface AdminReferral {
  id: string;
  referrerId: string;
  referrerEmail: string;
  refereeId: string;
  refereeEmail: string;
  status: string;
  reviewNote?: string;
  createdAt: string;
}

export interface AdminPlan {
  code: string;
  name: LocalizedText;
  features: string[];
  purchasable: boolean;
}

export interface RedemptionResult {
  code: string;
  status: string;
  orderNumber: string;
  changed: boolean;
}

export interface AuditEntry {
  id: string;
  actorUserId?: string;
  actorType: string;
  action: string;
  targetType?: string;
  targetId?: string;
  details: Record<string, unknown>;
  requestId?: string;
  createdAt: string;
}

export interface ImpactPreview {
  affectedOffers: number;
  failingOffers: string[];
}

export interface VariantMetrics {
  variant: string;
  impressions: number;
  clicks: number;
  cartAdds: number;
  purchases: number;
  revenue: number;
  contributionMargin: number;
  clickThroughRatePercent: string;
  conversionRatePercent: string;
}

export type SettingsKey = 'pricing' | 'loyalty' | 'membership' | 'recommendation';
