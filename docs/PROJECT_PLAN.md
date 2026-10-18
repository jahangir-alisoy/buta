# Buta — Ticket Marketplace & Loyalty Platform
## MVP Plan and Project Requirements

| | |
|---|---|
| Version | 1.0 (planning baseline) |
| Date | 2026-10-17 |
| Repository | `jahangir-alisoy/buta` |
| Working name | **Buta** (taken from the repository name — confirm in §22); the product is presented to users as **Naxış** |
| Database migrations | **Liquibase** (Flyway is not used anywhere) |
| Status | Ready to implement. Defaults in §22 apply unless changed. |

## Changes since the planning baseline (2026-10-18)

The app has moved past this baseline in the following ways; the sections below are updated where they described the old behaviour.

- **Stripe Checkout payments** (test mode) next to the sandbox, selected with `APP_PAYMENT_PROVIDER=stripe`: hosted payment page, signed webhooks, return-page confirmation, refunds through Stripe, automatic refund of payments that arrive after the order expired (migration `014`).
- **Paid Plus subscription:** 4.99 ₼ per month, bought in the sandbox or through Stripe (migration `013`).
- **Real Azerbaijani cultural organisations as demo partners** at the owner's request (migration `905`, context `demo`), replacing the fictional ones; one fictional partner (Melodia Stream) remains. They keep the `demo` flag; offers and prices stay illustrative and there are no partner agreements.
- **Brand:** the Naxış logo (`frontend/public/brand/logo.png`, favicon and Apple touch icon).
- **Phone layout:** on screens up to 599 px wide, offers use a two-column grid of compact cards with tighter spacing so more fit on one screen; tablet and desktop are unchanged.
- **Owner accounts and email verification** (migrations `010`, `011`) and demo wording cleanup (`012`).

## Contents
1. How to use this document
2. Current state and build environment
3. Key decisions
4. Product overview
5. Glossary
6. Scope
7. Roles and membership
8. Functional requirements
9. Non-functional requirements
10. Architecture
11. Data model
12. Liquibase migration plan
13. REST API
14. Security design
15. Privacy
16. Configuration
17. Frontend specification
18. Seed and demo data
19. Testing strategy
20. Implementation phases
21. Deployment (Docker, Compose, Render)
22. Decisions to confirm
23. Known limitations and production prerequisites
24. Requirements traceability

---

## 1. How to use this document

- This is the single source of truth for building the MVP. It turns the original brief into concrete decisions, business rules, schema, API and a phased checklist.
- Build in the vertical-slice phases of §20. Every phase ends with: backend and frontend builds green, tests run and green, a commit pushed to the working branch.
- The backend owns every business rule. The frontend only displays values computed by the server (prices, savings, margins, points, statuses, eligibility).
- Never report a build or test as passing unless it was actually run. Record exact commands and results.
- If something is not specified here, choose the simplest option that satisfies the brief and record it in the README "Decisions" section.

### Engineering conventions (team preferences)
- Java 21, Spring Boot 3.x, SOLID, clean code. **No code comments** — names carry intent.
- Smallest change that satisfies the requirement; no speculative abstractions. Interfaces only where an implementation is genuinely replaceable: email, payment, file storage, rate limiting, clock.
- Constructor injection, immutable request/response DTOs as Java records, no field injection, no Lombok, no MapStruct.
- Package-by-feature. Thin controllers, transactions in services, invariants and state transitions inside entities.
- Approved dependency list in §10.5. Adding a dependency requires a one-line justification in the README.

---

## 2. Current state and build environment

**Repository (inspected 2026-10-17):** one commit ("first commit") containing only `.idea/.gitignore`. Greenfield — nothing to migrate. Keep `.idea/.gitignore`; the root `.gitignore` excludes IDE workspace files, build output, `.env` files and local uploads.

**Cloud build container (for the implementing agent):**

| Tool | Status |
|---|---|
| Java | OpenJDK 21.0.12 |
| Gradle | 8.14.3 (the project ships the Gradle Wrapper `gradlew`) |
| Node / npm / pnpm | 22.22.0 / 10.9.4 / 10.28.0 — the project uses **npm** |
| PostgreSQL | 16.15 installed, stopped by default (`pg_ctlcluster 16 main start`) |
| Docker | Engine 29.8 + Compose v5.6. Daemon is not auto-started (`dockerd &`). Docker Hub anonymous pulls hit 429 rate limits; `mirror.gcr.io/library/<image>` works — pull from the mirror and `docker tag` to the Docker Hub name |
| Browser | Chromium for Playwright at `/opt/pw-browsers` (no WebKit) |
| Registries | Maven Central and npm reachable |

**Versions available at planning time:** Spring Boot 3.5.16 (latest 3.x line). Its BOM manages Liquibase, Hibernate 6.6, Spring Security 6.5, PostgreSQL JDBC 42.7, Caffeine 3.2 and Testcontainers 1.21.4. If Testcontainers reports a Docker API version mismatch with Engine 29, raise the `testcontainers.version` property.

---

## 3. Key decisions

| # | Topic | Decision | Rationale |
|---|---|---|---|
| D1 | Monorepo | `/backend`, `/frontend`, `/docker-compose.yml`, `/docs/DEPLOY_RENDER.md`, `/README.md`, `/SECURITY.md`, `/.env.example`, `/docs/` | Layout from the brief plus deployment and security docs |
| D2 | Backend | Java 21, Spring Boot 3.5.16, **Gradle** (Kotlin DSL) single module, modular monolith (package-by-feature) | One deployable with clear boundaries, no microservice overhead |
| D3 | Database | PostgreSQL 16 | Brief; Render managed PostgreSQL |
| D4 | Migrations | **Liquibase**: formatted-SQL changesets included from a YAML master changelog; Hibernate `ddl-auto=validate` | Full PostgreSQL features (CHECK constraints, partial indexes) with explicit rollbacks; schema owned by migrations only |
| D5 | Auth | Passwordless email OTP + **opaque server-side session** in an HttpOnly cookie; CSRF token; roles loaded from the database on every request | Instant revocation, no secrets readable by JavaScript, no refresh-token rotation races between tabs. JWT access/refresh is the rejected alternative |
| D6 | Topology | **Same origin**: Nginx serves the SPA and reverse-proxies `/api/*` to the backend | First-party cookies (Safari blocks third-party cookies, and two `onrender.com` subdomains are different sites), no CORS in production, simpler CSRF |
| D7 | Render | Backend = Docker web service; **frontend = Docker web service (Nginx)**; database = Render PostgreSQL | Nginx gives the same-origin `/api` proxy, SPA refresh fallback and security headers in one versioned config, identical locally and on Render |
| D8 | Catalog model | **Partner products** (what partners supply) vs **offers** (what we sell: `SINGLE` or `BUNDLE`), linked by offer components; inventory lives on products | One pricing, margin and checkout path for single offers and bundles |
| D9 | Money | `BigDecimal` / `NUMERIC(12,2)`, currency AZN, scale 2, `HALF_UP`; rates as percent `NUMERIC(5,2)`; money serialized in JSON as strings | No floating point anywhere |
| D10 | Points | Integer points (`BIGINT`) in an append-only ledger with unique idempotency keys | Auditable, compensating entries, no duplicate rewards |
| D11 | Time | `timestamptz` + `Instant` (UTC); business time zone `Asia/Baku`; injected `Clock` | Consistent storage; testable expiry logic |
| D12 | Content i18n | Localized text as JSONB `{"az": "...", "en": "..."}`, both keys required | New languages without schema changes; no mixed-language fallbacks |
| D13 | Payments | `PaymentProvider` interface with `SandboxPaymentProvider` (default) and `StripePaymentProvider` (Stripe Checkout, hosted page + signed webhooks) | Honest demo by default; real test-mode payments when configured |
| D14 | Email | `EmailSender` interface: Gmail SMTP with an app password (`MAIL_USERNAME`, `MAIL_APP_PASSWORD`) for deployed environments, Mailpit locally (`dev` profile), in-memory mailbox for tests | Provider abstraction and safe development workflow |
| D15 | File storage | `FileStorage` interface: **database** (default, survives Render redeploys) and local disk; S3-compatible later | Render's container filesystem is ephemeral |
| D16 | Recommendations | Explainable weighted rules; weights and signal settings stored in the database | Brief; ML only when real data exists |
| D17 | Rate limiting | `RateLimiter` interface with in-memory (Caffeine) buckets per IP; OTP attempt and lock counters persisted in the database | Works on a single instance; security-critical limits survive restarts |
| D18 | Frontend | React 19, Vite 8, TypeScript 6.0.x, React Router 7, TanStack Query 5, i18next; CSS Modules + design tokens; native `<dialog>` / `<select>`; three.js lazy-loaded on the home page only | Mature, small dependency set. TypeScript pinned below 6.1 because typescript-eslint 8.x supports `<6.1` |
| D19 | Testing | JUnit 5 + Spring Boot Test + Testcontainers (PostgreSQL 16); Vitest + Testing Library; Playwright (Chromium) responsive smoke tests | Real database in tests; fast UI tests |
| D20 | IDs | Application-generated UUID primary keys; slugs for public catalog URLs; human-readable order numbers | Non-enumerable IDs, clean URLs |

---

## 4. Product overview

**Business model:** BUSINESS → PLATFORM → CUSTOMERS. Partners (cinemas, theatres, museums, festivals, exhibitions, workshops, book vendors, public-transport discount providers, digital subscription/service providers where legally and commercially available) supply discounted inventory. The platform aggregates it, creates bundles, increases partner sell-through of under-performing inventory and builds customer loyalty. The platform is usually a reseller/aggregator, not the organizer or issuer: every product records the partner, the partner's terms and how the customer redeems it.

**Objectives and how the MVP measures them:**

| Objective | Measured by |
|---|---|
| Affordable offers | Savings % per offer; share of orders with savings ≥ 20% |
| Useful bundles | Bundle share of orders; bundle conversion vs single offers |
| Repeat purchases | Repeat-purchase rate (30/90 days); share of Plus members |
| Positive contribution margin | Contribution margin per order (snapshotted on order items) |
| Partner value | Units sold, sell-through %, partner payout, redemption rate per partner (admin report) |

**Pricing example (illustrative, never hardcoded):** a popular movie ticket retails at 20 AZN; a lower-demand ticket retails at 15 AZN. A bundle of both sells for 28 AZN against a 35 AZN reference retail value, so the customer saves 7 AZN. Whether that bundle may be sold is decided by the margin rules in §8.3, never by retail value.

**Honesty rules (non-negotiable):**
- No fabricated partners, partner agreements, availability, ratings, reviews, testimonials, sales counts or metrics. Demo content is synthetic and labeled illustrative.
- Demo partners are real Azerbaijani cultural organisations (owner decision, migration `905`) with factual descriptions only; they keep the `demo` badge and no partnership, availability or pricing is claimed for them.
- Sandbox payments are always labeled; nothing pretends a real charge happened.
- Urgency and scarcity messages only come from real inventory numbers and real validity dates.

---

## 5. Glossary

| Term | Meaning |
|---|---|
| Partner | A business supplying products (cinema, museum, book vendor, …) |
| Partner product | A unit the partner supplies: ticket type, admission, coupon, transport pass, subscription voucher, service. Has retail price, partner cost terms, inventory, validity and redemption method |
| Offer | What customers browse and buy. `SINGLE` = one product; `BUNDLE` = two or more products sold at one price |
| Offer component | A product and quantity inside an offer, optionally with a bundle-specific negotiated cost |
| Reference retail value | Sum of component retail price × quantity — the "buy separately" price |
| Savings | Reference retail value − selling price |
| Partner cost | What the platform owes partners for one offer unit (wholesale net cost or commission based) |
| Contribution margin | Selling price − partner costs − payment fee − expected loyalty cost − other variable costs |
| Qualifying spend | AZN paid in fulfilled, non-refunded orders within a window; used to earn Plus. Never confused with points |
| Available points | Points a member can redeem now |
| Tier-qualifying points | Net points earned (redemptions do not reduce them); they decide the tier |
| Award trigger | Order state at which purchase points are awarded (default `FULFILLED`) |
| Redemption | A voucher/ticket code per purchased unit, presented to the partner |
| Sandbox | Simulated payment provider; no money moves |
| Stripe Checkout | Stripe's hosted payment page; used with test keys, no card data reaches our servers |

---

## 6. Scope

**In scope (MVP):** public marketplace (home, categories, offers, bundles, events, partners, search, filters, offer detail); single offers and bundles with margin rules; cart, checkout, sandbox or Stripe Checkout payment, vouchers, cancellation, refunds; email-OTP registration and login with sessions; profile and settings; Azerbaijani and English; light and dark themes; Free/Plus membership; points ledger, tiers, tasks, referrals; rule-based recommendations with event tracking and privacy controls; admin console and admin APIs; Docker, Compose, Render; README and SECURITY.md; automated tests.

**Out of scope (boundaries documented in the README):**

| Item | MVP boundary |
|---|---|
| Live payment acquiring | Stripe Checkout is integrated and used with test keys; going live needs a live Stripe account and legal review |
| Partner self-service portal | Platform admins operate partner data; partner redemption is simulated by the admin "redeem code" endpoint; a future `PARTNER` role gets scoped APIs |
| Partner API integrations (live inventory sync) | Inventory maintained by admins |
| ML ranking | Logged features and A/B plumbing; upgrade path documented |
| Seat maps, showtime pickers | Each sellable showtime or ticket type is its own product |
| SMS / phone verification | Phone optional and stored as unverified |
| Self-service data export and account deletion | Production prerequisite (§23) |
| Points expiry | Not in MVP; the ledger supports adding it later |
| Multi-currency, tax, invoicing | AZN only; production prerequisite |
| Marketing email, push notifications | None |

---

## 7. Roles and membership

| Actor | Capabilities |
|---|---|
| Visitor (anonymous) | Browse everything public, search, filter, view details |
| Customer — FREE | Visitor capabilities + cart, checkout, orders, vouchers, favorites, profile, preferences |
| Customer — PLUS | FREE capabilities + points, tiers, tasks, referral rewards, points redemption, personalized "For you", Plus-only offers |
| Admin | Platform operations (§8.10). No impersonation, no buying on behalf of customers |
| Partner | No login in MVP |

Roles (`CUSTOMER`, `ADMIN`) and membership plans (`FREE`, `PLUS`) are independent. An admin may also be a customer.

---

## 8. Functional requirements

Every requirement has an ID that tests and commit messages reference.

### 8.1 Public marketplace (MKT)

| ID | Requirement | Acceptance criteria |
|---|---|---|
| MKT-01 | Anonymous visitors browse home, categories, offers, bundles, events, partners, search results and offer details | Every public `GET` returns 200 without a session; no login prompt while browsing |
| MKT-02 | Search over offer title, subtitle, partner name and category name in both languages; case- and diacritic-insensitive | `baki` matches "Bakı"; `seher` matches "Səhər". Folding ə→e, ı→i, İ→i, ö→o, ü→u, ç→c, ş→s, ğ→g is applied to the stored `search_key` and to the query |
| MKT-03 | Filters: category, offer type (single/bundle), product type, partner, city, price range, event date range, Plus-only, include sold-out (default off) | Filters combine with AND; invalid values → 400 with field errors |
| MKT-04 | Sorting: recommended, price ascending, price descending, biggest savings, ending soon, newest, event date | Sort values are an enum; unknown value → 400. "Recommended" is personalized for consenting Plus members, popularity-based otherwise |
| MKT-05 | Pagination | `page` (0-based), `size` default 24, max 48; totals returned; deterministic tie-breaker on `id` |
| MKT-06 | Offer detail | Images, title, partner (with demo badge), category, components (bundles), reference retail value, selling price, savings (amount and %), validity, event date/time and venue, availability state, terms, redemption instructions, refund policy, points preview for Plus (with terms), favorite and add-to-cart actions |
| MKT-07 | Bundle comparison | "Separately 35.00 ₼" (component list with prices) vs "Bundle 28.00 ₼ — save 7.00 ₼ (20%)"; every component shows its partner and validity. Amounts are formatted per locale |
| MKT-08 | Auth-gated actions keep the user's intent | Anonymous Add to cart / Favorite / Checkout → `/auth?returnTo=…`. After OTP (and profile completion for new users) the action runs automatically, the user returns to the original page and a toast confirms. If the item became unavailable, a localized message explains why and links back to it. Nothing is silently dropped |
| MKT-09 | Plus-only offers | Listed for everyone with a Plus badge; FREE and anonymous users can open the detail page but cannot buy; the server rejects with `PLUS_REQUIRED` |
| MKT-10 | Truthful urgency | "Only N left" only when real available units ≤ low-stock threshold (default 5); countdowns only to real validity or event times; no fake timers, ratings, testimonials or sales counts |
| MKT-11 | Demo labeling | With `APP_DEMO_MODE=true`: persistent banner "Demo: partners, offers and availability are illustrative; payments are sandbox". Demo partners carry a badge |
| MKT-12 | Honest static pages | About, How it works (aggregator model; partners fulfil), Terms (draft), Privacy (draft). Real pages — no placeholder links |

### 8.2 Catalog (CAT)

| ID | Requirement | Acceptance criteria |
|---|---|---|
| CAT-01 | Categories are configurable by admins: slug, localized name, icon key, sort order, active flag | Seeded in every environment: cinema, theatre, festivals, workshops, museums, exhibitions, book-coupons, transport, services |
| CAT-02 | Partner: name, slug, localized description, city, address, website (https only), logo, `demo` flag, status | Offers containing products of an inactive partner are not sellable |
| CAT-03 | Partner product: partner, category, product type (`EVENT_TICKET`, `ADMISSION`, `COUPON`, `TRANSPORT_PASS`, `DIGITAL_SUBSCRIPTION`, `SERVICE`), localized title/description/terms/redemption instructions, retail price, cost model (`WHOLESALE` + unit net cost, or `COMMISSION` + commission %), validity window, optional event start/end and venue, city, redemption method (`VOUCHER_CODE`, `SHOW_CODE_AT_VENUE`, `PARTNER_WEBSITE_CODE`), refundable flag + refund deadline (hours before event or validity end), restock-on-refund flag, informational eligibility note (e.g. "18+, checked by the venue"), status | Database CHECK constraints enforce cost-model consistency |
| CAT-04 | Inventory per product: total, reserved, sold. Every change is recorded as an inventory movement | CHECK: `reserved ≥ 0`, `sold ≥ 0`, `reserved + sold ≤ total` |
| CAT-05 | Offer: type `SINGLE` (exactly one component) or `BUNDLE` (two or more), slug, primary category, localized title/subtitle/description, image, selling price, visibility (`PUBLIC`, `PLUS_ONLY`), status (`DRAFT`, `PUBLISHED`, `PAUSED`, `ARCHIVED`), sale window, max per order (1–10), tags, business priority (0–100), featured flag | Component-count rule enforced in the service and covered by a test |
| CAT-06 | Offer component: product, quantity (1–10), optional negotiated unit cost (bundle-specific partner deal) | Unique (offer, product) |
| CAT-07 | Derived values computed server-side on read: reference retail value, savings, availability = min over components of ⌊available ÷ quantity⌋, effective validity = intersection of component validity windows and the sale window | Never accepted from clients |
| CAT-08 | Publishing requires active partners and products, both languages present, effective validity in the future, and passing margin rules (§8.3) | Failing rules are returned as a list and publishing is blocked |
| CAT-09 | Campaigns: localized name and banner text, audience (`ALL`, `PLUS`), time window, offers, bonus points per unit (Plus only), featured flag | Attaching bonus points re-runs margin checks for the affected offers |

### 8.3 Pricing, bundles and margin (PRC)

All values are `BigDecimal`, scale 2, `HALF_UP`, unless stated otherwise. Rates are percentages (2.50 means 2.5%).

```
referenceRetail      = Σ (product.retailPrice × component.quantity)
savings              = referenceRetail − sellingPrice                       must be ≥ 0
savingsPercent       = savings ÷ referenceRetail × 100
allocatedPrice_i     = sellingPrice × (retail_i × qty_i) ÷ referenceRetail  remainder goes to the last component
partnerCost_i        = negotiatedUnitCost_i × qty_i                          when a negotiated cost is set
                     = product.unitNetCost × qty_i                           WHOLESALE
                     = allocatedPrice_i × (1 − commissionPercent_i ÷ 100)    COMMISSION
paymentFee           = sellingPrice × paymentFeePercent ÷ 100 + paymentFeeFixed
maxPointsEarned      = floor(sellingPrice × pointsPerAzn × highestTierMultiplier) + maxActiveCampaignBonusPoints
expectedLoyaltyCost  = maxPointsEarned × pointValueAzn × expectedRedemptionPercent ÷ 100
otherVariableCost    = sellingPrice × otherVariableCostPercent ÷ 100 + otherVariableCostFixed
contributionMargin   = sellingPrice − Σ partnerCost_i − paymentFee − expectedLoyaltyCost − otherVariableCost
marginPercent        = contributionMargin ÷ sellingPrice × 100
sellable             ⇔ savings ≥ 0  AND  contributionMargin ≥ minMarginAmount  AND  marginPercent ≥ minMarginPercent
```

The check is deliberately conservative: it assumes the highest tier multiplier and charges the fixed payment fee to a single offer unit.

**Worked example (illustrative values; every number is configurable):**

| Line | Value (AZN) |
|---|---|
| Popular cinema ticket, retail | 20.00 |
| Lower-demand exhibition ticket, retail | 15.00 |
| Reference retail value | 35.00 |
| Bundle selling price | 28.00 |
| Customer savings | 7.00 (20.00%) |
| Partner costs: cinema wholesale 16.00 + exhibition wholesale 9.00 | 25.00 |
| Payment fee: 28.00 × 2.50% + 0.20 | 0.90 |
| Expected loyalty cost: floor(28 × 1 × 1.25) = 35 points × 0.01 × 70% | 0.25 |
| Other variable cost (fixed) | 0.30 |
| **Contribution margin** | **1.55 (5.54%)** |
| Rule: ≥ 1.00 AZN and ≥ 5.00% | Pass → sellable |

- If the cinema's wholesale cost were 18.00, the margin would be −0.45 and publishing is blocked with the breakdown shown.
- Commission variant: allocated cinema price = 28.00 × 20 ÷ 35 = 16.00; at 10% commission the partner cost is 14.40.

| ID | Requirement |
|---|---|
| PRC-01 | Every input is configurable: offer selling price; product retail price and cost terms; negotiated component costs; global fee, margin and loyalty settings (admin-editable, audited) |
| PRC-02 | Margin uses actual partner costs, payment fees, expected loyalty cost and other variable costs — never retail value alone. Customers never see costs or margins |
| PRC-03 | Margin rules run at publish time, again at checkout (a failing offer is not sold: 409 `OFFER_NOT_SELLABLE`), and as an impact preview whenever pricing or loyalty settings change (lists offers that would fail) |
| PRC-04 | Admin margin preview returns the full breakdown for unsaved drafts |
| PRC-05 | Order items snapshot unit price, partner cost, allocated payment fee, expected loyalty cost and contribution margin for reporting |
| PRC-06 | Rounding: amounts scale 2 `HALF_UP`; allocations always sum exactly to the selling price; points always floor |
| PRC-07 | Single currency (AZN) in MVP; currency columns kept for the future |

### 8.4 Cart, checkout, orders, payments, redemption, refunds (ORD)

| ID | Requirement |
|---|---|
| ORD-01 | One active cart per user; items are offer + quantity (1 to min(offer max per order, 10)); at most 20 distinct items |
| ORD-02 | The cart never stores trusted prices. It stores `unitPriceAtAdd` only to flag price changes. Every cart read recomputes prices and availability; unavailable items are flagged, not silently removed |
| ORD-03 | Add to cart validates: offer published, inside its sale window, visible to the user (`PLUS_ONLY` needs Plus), enough availability |
| ORD-04 | `POST /checkout/quote` returns a server quote: lines, subtotal, points discount, cash total, points to earn |
| ORD-05 | `POST /checkout` with an `Idempotency-Key` header, in one transaction: re-validate everything; reserve inventory for every component (ordered by product id to avoid deadlocks); deduct redeemed points (ledger `REDEEM`, loyalty account row locked); create the order (`PENDING_PAYMENT`, `expires_at = now + reservation TTL`, default 15 min); create the payment (`PENDING`). The same key from the same user returns the same order; the same key with a different cart → 409 `IDEMPOTENCY_CONFLICT` |
| ORD-06 | Oversell prevention: `UPDATE product_inventory SET reserved = reserved + :q WHERE product_id = :id AND total - reserved - sold >= :q`. Zero updated rows → the whole checkout fails with 409 `INSUFFICIENT_INVENTORY` and rolls back. CHECK constraints are the last line of defence |
| ORD-07 | Sandbox payment success: payment `CAPTURED`; reserved → sold; one redemption code per purchased unit; order `FULFILLED`; loyalty evaluation in the same transaction. Failure: payment `FAILED`; reservations released; points redemption reversed; order `CANCELLED` (reason `PAYMENT_FAILED`). Confirmation is idempotent. With Stripe the same success path runs when a paid Checkout session is confirmed (webhook or return page); a payment for an order that is no longer awaiting payment is refunded automatically |
| ORD-08 | Expiry job (every minute, `FOR UPDATE SKIP LOCKED`): `PENDING_PAYMENT` orders past `expires_at` → `EXPIRED`, reservations released, points redemption reversed |
| ORD-09 | Customer cancel: only `PENDING_PAYMENT` → `CANCELLED` |
| ORD-10 | Refund: a customer may refund units whose redemption is `ISSUED` (not redeemed), whose product is refundable and whose refund deadline has not passed. Sandbox refund; redemptions `VOIDED`; inventory returned when restock-on-refund is set; points for refunded units reversed; tasks re-evaluated; order `PARTIALLY_REFUNDED` or `REFUNDED`. Admins may refund regardless of deadline, with a mandatory reason (audited) |
| ORD-11 | Redemption: admin endpoint marks a code `REDEEMED` (simulates the partner's scan): code exists, is `ISSUED`, is within validity; idempotent. Hourly job: `ISSUED` codes past validity → `EXPIRED`. An order is `COMPLETED` once every unit is `REDEEMED`, `EXPIRED` or `VOIDED` with at least one not voided |
| ORD-12 | Payment step: with the sandbox, a "Pay" and a "Cancel payment" button; with Stripe, a button that opens Stripe Checkout and a confirmation state on return. `GET /config` exposes `paymentMode` (`SANDBOX` or `STRIPE`) |
| ORD-13 | Order history shows status, items, amounts, points earned/redeemed, voucher codes with redemption instructions, and the refund action when eligible |

**Order states**
```
PENDING_PAYMENT ──payment captured──▶ FULFILLED ──all units redeemed/expired──▶ COMPLETED
PENDING_PAYMENT ──cancel / payment failed──▶ CANCELLED
PENDING_PAYMENT ──reservation timeout──▶ EXPIRED
FULFILLED ──partial refund──▶ PARTIALLY_REFUNDED ──remaining refunded──▶ REFUNDED
FULFILLED ──full refund──▶ REFUNDED
PARTIALLY_REFUNDED ──remaining units redeemed/expired──▶ COMPLETED
```
**Payment states:** `PENDING → CAPTURED | FAILED`; `CAPTURED → PARTIALLY_REFUNDED → REFUNDED`; `CAPTURED → REFUNDED`.
**Redemption states (per unit):** `ISSUED → REDEEMED | EXPIRED | VOIDED`.
Transitions live in entity methods; illegal transitions throw and are covered by tests.

### 8.5 Authentication and sessions (AUTH)

**One unified flow serves registration and login**, so responses are identical for known and unknown emails:
1. `POST /auth/otp/request {email}` → always `202 {"resendAfterSeconds": 60}`. UI text: "If this address can receive email, we sent a 6-digit code. It expires in 10 minutes."
2. `POST /auth/otp/verify {email, code}` → existing `ACTIVE` user: session created. Unknown email: user created with status `PROFILE_INCOMPLETE` and verified email, session created; the response says `nextStep: "COMPLETE_PROFILE"`.
3. `POST /auth/complete-profile {firstName, lastName, username, referralCode?}` → status `ACTIVE`.

**OTP rules**

| ID | Requirement |
|---|---|
| AUTH-01 | 6-digit numeric code from `SecureRandom`, uniform over 000000–999999 |
| AUTH-02 | Stored as HMAC-SHA256(`APP_OTP_PEPPER`, challengeId + normalized email + code); constant-time comparison. Plain SHA-256 is not acceptable: one million possible codes are trivially brute-forced from a leaked database |
| AUTH-03 | TTL configurable, default 10 minutes (allowed range 5–10) |
| AUTH-04 | Single use: `consumed_at` set by a conditional update on success, so replay is impossible |
| AUTH-05 | A new request supersedes earlier active challenges for the same email and purpose |
| AUTH-06 | Max 5 wrong attempts per challenge, then the challenge is locked |
| AUTH-07 | Resend cooldown 60 s per email |
| AUTH-08 | Rate limits (configurable): requests 5/hour per email and 20/hour per IP; verifications 30 per 10 min per IP; 3 locked challenges within 1 hour block the email for 30 minutes. Exceeding a limit → 429 with `Retry-After`. Behaviour is identical for registered and unregistered emails, so limits never reveal whether an account exists |
| AUTH-09 | Attempt counters and lock state are persisted in the database; IP buckets are in memory |
| AUTH-10 | OTP values are never logged, never returned by any API in any profile, never placed in exception messages. They appear only in the email body |
| AUTH-11 | Development-only test mechanism: an in-memory mailbox and `GET /api/v1/dev/mailbox` exist only when the `dev` profile is active **and** `APP_DEV_MAILBOX_ENABLED=true`. Startup fails if that flag is set together with the `prod` profile. Docker Compose uses Mailpit (UI at http://localhost:8025) |
| AUTH-12 | Email normalization for identity: trim + lowercase (`Locale.ROOT`), max 254 characters, format validated; unique index on the normalized email. A separate canonical form (Gmail dots and `+tag` removed) is used only for referral abuse detection |
| AUTH-13 | Username: `^[a-z][a-z0-9._]{2,29}$`, stored lowercase, unique, reserved names rejected (admin, administrator, support, buta, root, system, api, help, security, null) |
| AUTH-14 | Account status: `PROFILE_INCOMPLETE` (only profile completion, session and logout endpoints allowed), `ACTIVE`, `SUSPENDED` (cannot sign in; sessions revoked) |
| AUTH-15 | Verify failure returns a generic 400 `OTP_INVALID` (wrong, expired, consumed and missing challenges look the same); a locked challenge or blocked email returns 429 `OTP_LOCKED` |

**Sessions and request protection**

| ID | Requirement |
|---|---|
| AUTH-20 | Session token: 32 bytes from `SecureRandom`, base64url. Cookie `__Host-buta_session` in production (`buta_session` in local HTTP dev): HttpOnly, Secure (production), SameSite=Lax, Path=/. Stored as SHA-256 hash (high-entropy token, so a plain hash is sufficient) |
| AUTH-21 | Idle timeout 7 days, absolute lifetime 30 days (configurable); `last_seen_at` updated at most every 5 minutes |
| AUTH-22 | Rotation: a brand-new token on every successful OTP verification (no pre-login token is ever upgraded); on email change the current session is rotated and all others revoked; on role change or suspension all sessions of that user are revoked |
| AUTH-23 | Logout revokes the current session and clears the cookie; "Log out everywhere" revokes all of the user's sessions; admins can revoke a user's sessions |
| AUTH-24 | Expired and revoked sessions purged daily |
| AUTH-25 | CSRF: Spring Security CSRF on every state-changing request, OTP endpoints included. Cookie `XSRF-TOKEN` (readable by JS, SameSite=Lax, Secure in production) + header `X-XSRF-TOKEN`, using the SPA configuration from the Spring Security reference. In addition an Origin check rejects non-GET requests whose `Origin` header is present and not in the allowed list |
| AUTH-26 | Authorization comes from server state only: each request resolves session → user → status, roles and membership from the database. Role or membership flags in the frontend are cosmetic |
| AUTH-27 | Email change: `POST /me/email-change {newEmail}` sends an OTP (purpose `EMAIL_CHANGE`, bound to the user) to the new address with a generic response; `POST /me/email-change/confirm {code}` updates the email, rotates the session, revokes other sessions and notifies the old address. An address already in use fails at confirmation with a generic `EMAIL_UNAVAILABLE` |
| AUTH-28 | Admin bootstrap: `APP_ADMIN_EMAILS` (comma-separated). On every successful sign-in the `ADMIN` role granted through bootstrap is synchronised with that list (granted or removed) and audited. Public registration can never grant admin; there are no default admin credentials |

### 8.6 Profile and settings (PRF)

| ID | Requirement |
|---|---|
| PRF-01 | Profile fields: avatar, first name (1–50), surname (1–50), username (unique), verified email (read-only; change through AUTH-27), optional phone (E.164 `^\+[1-9]\d{7,14}$`, shown as "not verified") |
| PRF-02 | Avatar: JPEG or PNG only, ≤ 2 MB upload, magic-byte check, decoded and re-encoded to a 256×256 JPEG (strips EXIF/GPS and neutralises polyglots), stored through `FileStorage` (database by default), served only to its owner with `Cache-Control: private` and `X-Content-Type-Options: nosniff`. `accept="image/jpeg,image/png"` lets iOS convert HEIC photos to JPEG on upload |
| PRF-03 | Account area: profile, membership and loyalty status, points history, completed tasks, order history with vouchers, favorites, preferences and privacy, language, theme, logout, log out everywhere |
| PRF-04 | Theme: light, dark or system. Stored in `localStorage` for instant apply before first paint and in user preferences when signed in |
| PRF-05 | Language: az or en. Stored locally and in user preferences; emails use the saved preference |
| PRF-06 | Data minimization: no birth date, gender, ID numbers or address are collected |

### 8.7 Membership (MEM)

| ID | Requirement |
|---|---|
| MEM-01 | Plans `FREE` and `PLUS` stored in `membership_plans` with feature flags: `LOYALTY_POINTS`, `LOYALTY_TASKS`, `TIERS`, `POINTS_REDEMPTION`, `PERSONALIZED_RECOMMENDATIONS`, `EXCLUSIVE_OFFERS`. Default: all enabled for PLUS only. Admins can toggle flags (audited) |
| MEM-02 | Plus is obtained by (a) earning it: qualifying spend (cash paid in fulfilled, non-refunded orders during the last 180 days) ≥ 100.00 AZN — a spend threshold, clearly named, never shown as points; or (b) admin assignment with optional end date and mandatory reason (audited) |
| MEM-03 | Earned Plus lasts 365 days from qualification and is re-evaluated nightly and after order events; admin-assigned Plus ends at its end date. When Plus ends, the member becomes FREE; the points balance is kept but cannot be earned or redeemed until Plus returns |
| MEM-04 | Paid subscription: Plus is purchasable for 4.99 ₼ per month (migration `013`); purchases are recorded in `membership_purchases` with the provider and, for Stripe, a unique provider reference; memberships record `source` (`DEFAULT`, `EARNED`, `ADMIN`, `SUBSCRIPTION`) |
| MEM-05 | FREE members browse everything, see all public discounts and bundles, buy, and see an honest Plus explanation with their qualifying-spend progress (e.g. "72.00 ₼ more qualifying spend unlocks Plus") |
| MEM-06 | Purchases made while FREE never earn points retroactively |

### 8.8 Loyalty engine (LOY)

**Ledger**

| ID | Requirement |
|---|---|
| LOY-01 | `loyalty_ledger_entries` is append-only and the source of truth (signed integer points). `loyalty_accounts` caches balances and is updated in the same transaction with the account row locked. Ledger rows are never updated or deleted |
| LOY-02 | Entry types: `EARN_PURCHASE`, `EARN_TASK`, `REDEEM`, `REVERSAL` (references the reversed entry), `ADJUSTMENT` (admin; reason mandatory; audited) |
| LOY-03 | Idempotency through a unique `idempotency_key`: `EARN_PURCHASE:{orderItemId}`, `REDEEM:{orderId}`, `REVERSAL:{reversedEntryId}`, `EARN_TASK:{taskId}:{userId}:{periodKey}:{awardSeq}`. Duplicate delivery is a no-op |
| LOY-04 | Shown separately everywhere: qualifying spend (AZN), lifetime points earned (net of reversals), points redeemed, available points, tier-qualifying points |
| LOY-05 | Reconciliation test: Σ ledger points per user = cached available points |

**Earning**

| ID | Requirement |
|---|---|
| LOY-10 | Only members whose plan has `LOYALTY_POINTS` earn. Points are awarded only when the order reaches the configured award trigger — default `FULFILLED` (payment captured, vouchers issued); option `COMPLETED`. Never when an order is merely placed |
| LOY-11 | Points per order item = floor(cash paid for the item × points per AZN × tier multiplier) + campaign bonus points × quantity. Cash paid excludes the points discount (allocated proportionally across items) |
| LOY-12 | Refund or cancellation after an award creates `REVERSAL` entries for the refunded items' points. If the available balance would become negative it is allowed to, points redemption is blocked until it is ≥ 0 again, and the account is flagged for admin review |

**Redemption**

| ID | Requirement |
|---|---|
| LOY-20 | Plus members redeem in steps of 100 points (100 points = 1.00 AZN at the default point value), minimum 100, maximum 20% of the order subtotal, never more than available. Deducted at checkout (`REDEEM`) under the account row lock; reversed when the order is cancelled, expires or the payment fails |
| LOY-21 | The points discount is funded by the loyalty liability already charged as expected loyalty cost when the points were earned, so checkout margin checks use the list selling price (accounting assumption — confirm with finance) |

**Tiers**

| ID | Requirement |
|---|---|
| LOY-30 | Tiers are configurable: code, localized name, threshold (tier-qualifying points), earn multiplier, localized benefits list. Defaults: BRONZE 0 (×1.00), SILVER 100 (×1.10), GOLD 500 (×1.25) |
| LOY-31 | Tier-qualifying points = lifetime net earned points (redemptions do not lower the tier; reversals do). Recomputed on every ledger change. Optional rolling window in months (0 = lifetime, default) |
| LOY-32 | Dashboard: current tier, next tier, points to the next tier, progress bar, tier benefits |

**Tasks**

| ID | Requirement |
|---|---|
| LOY-40 | Task definition: code, localized title/description/terms, type, parameters (JSONB), reward points, period (`ONE_TIME` or `CALENDAR_MONTH` in Asia/Baku), active window, minimum cash value per qualifying order (default 5.00 AZN), one completion per period |
| LOY-41 | MVP task types: `DISTINCT_PARTNERS` (e.g. 3 partners in a month), `BUNDLE_PURCHASE` (n bundles), `EVENT_ATTENDANCE` (n redeemed `EVENT_TICKET` units — attendance is proven by redemption), `NEW_CATEGORY` (first purchase in a category the member never bought before), `REFERRAL` (n qualified referrals) |
| LOY-42 | Evaluation is derived from qualifying facts (orders at the award trigger and not refunded, redemptions, qualified referrals) and recomputed on `OrderFulfilled`, `RedemptionRedeemed`, `OrderRefunded` and `ReferralQualified` events, with the `task_progress` row locked. Satisfied and not awarded → `EARN_TASK`. Awarded and no longer satisfied after a refund → `REVERSAL` |
| LOY-43 | Anti-abuse: an order counts once per task; orders below the minimum value never count; only the member's own paid orders count |

**Referrals**

| ID | Requirement |
|---|---|
| LOY-50 | Every active user has an 8-character referral code (unambiguous alphabet). A referee applies it during profile completion, or via a `?ref=` link captured before registration |
| LOY-51 | A referral qualifies when the referee's first order reaches the award trigger with cash paid ≥ 10.00 AZN. The `REFERRAL` task rewards the referrer (if Plus); a later refund of that order reverses the reward |
| LOY-52 | Anti-abuse: no self-referral (same user or same canonical email); the referee must be a new account (applied the code at registration, first purchase within 30 days); one referrer per referee; max 5 rewarded referrals per referrer per month; suspicious patterns (same registration IP hash as the referrer within 24 h, many referees from one IP hash) → `UNDER_REVIEW` for an admin to approve or reject. Every decision is audited |

**Audit:** admin adjustments, task and tier changes, and referral decisions are written to the audit log. Loyalty costs are part of contribution margin (§8.3).

### 8.9 Personalization and recommendations (REC)

**Signals** — collected for signed-in users only while personalization is enabled. Anonymous interactions are stored without any user or device identifier and feed aggregate popularity only.

| Signal | Source | Initial weight | Notes |
|---|---|---|---|
| Purchase (award trigger reached) | server | 5.0 | strongest signal |
| Explicit category preference | settings | 4.0 | edited by the user |
| Favorite | server | 3.0 | removing the favorite removes the signal |
| Add to cart | server | 2.0 | |
| Detail view | client | 0.5 | max one per offer per day; a click alone never establishes a preference |
| Search matching a category | client | 0.5 | query text truncated to 100 characters, kept ≤ 90 days |
| Impression (served) | server | 0 | frequency capping and evaluation only |

Time decay half-life 60 days; interaction retention 180 days (both configurable).

**Interest profile** (`user_interest_profiles`): category affinity (0–100), partner affinity, price band (from unit prices actually paid), price sensitivity (share of purchases with savings ≥ 25%), signal count, computed time. Recomputed when older than 10 minutes on request, and right after strong signals.

**Hard eligibility (before any scoring):** offer `PUBLISHED`, inside its sale window, availability > 0, all components valid, visibility allowed for the user, margin rules pass, not a one-time item the user already bought. No sensitive attributes exist in the model (no gender, age, religion, ethnicity, health); location is used only at city level if the user sets it.

**Scoring** — each component is normalized to 0–100:

| Component | Default weight | Definition |
|---|---|---|
| Interest match | 30 | Affinity for the offer's categories (bundles: max over components) plus partner affinity |
| Budget fit | 20 | 100 inside the user's price band, falling linearly to 0 at twice the band distance; cheaper scores higher for price-sensitive users; 50 when unknown |
| Customer value | 15 | Savings %: 0% → 0, ≥ 40% → 100, linear between |
| Loyalty relevance | 15 | 100 if buying completes an active task or reaches the next benefit threshold (e.g. 85/100 points and the offer earns ≥ 15); 60 if it advances a task; 0 otherwise. Counted only when interest match ≥ minimum relevance (default 20) |
| Discovery | 10 | Categories the user has not bought yet; decays with exposure |
| Business objective | 10 | Offer business priority (0–100), e.g. partner sell-through targets for under-performing inventory |

`score = Σ weight × component ÷ 100`; weights must sum to 100 (validated). These weights are starting hypotheses, not proven values.

**Re-ranking:** frequency cap (served ≥ 3 times in 7 days without interaction → demoted); diversity (top 10 holds max 2 offers per partner and 3 per category, greedy MMR-style); exploration (about 15% of slots, labeled "Try something new", drawn from unexplored categories with customer value ≥ 50).

**Guardrails:** business objective and loyalty relevance can never place an offer with interest match below the minimum relevance in a personalized slot (labeled exploration slots excepted). Business profitability never overrides relevance and quality checks.

**Fallbacks:** Plus member without history → selected preferences + popularity (real fulfilled-order counts over 30 days; editorial featured order when data is thin) + recency + category diversity. FREE and anonymous users → non-personalized "Popular", "Ending soon", "Best-value bundles".

**Reasons** (i18n keys with parameters): `REASON_CATEGORY_INTEREST` ("Based on your interest in cinema"), `REASON_PARTNER_INTEREST`, `REASON_TASK_PROGRESS` (points, task name, link to terms), `REASON_NEXT_BENEFIT` ("15 points to Silver"), `REASON_BUDGET_FIT`, `REASON_GREAT_VALUE`, `REASON_TRY_NEW`, `REASON_POPULAR` (only when computed from real order counts).

**Evaluation:** every served list logs request id, user, placement, algorithm version, variant, offer, position, score and components. Clicks, cart additions and purchases carry the request id (7-day attribution window), giving CTR, add-to-cart rate, conversion rate, revenue and contribution margin per 1,000 impressions, catalog coverage and diversity. Admin metrics endpoint per variant.

**A/B design:** deterministic assignment `hash(userId, experimentKey) mod 100`; each variant defines a weight set; guardrail metrics (refund rate, "not interested" rate); minimum sample size before decisions. The MVP ships the plumbing with a single `control` variant.

**ML upgrade path:** once enough legitimate data exists (illustrative threshold: ≥ 50k impressions and ≥ 1k conversions), train an offline learning-to-rank model on the logged components and context; evaluate offline (NDCG, AUC), run in shadow mode, then A/B test. The rules stay as eligibility guardrails.

**Privacy controls:** personalization toggle (opt-in prompt; when off, no behavioral collection and non-personalized results), editable explicit preferences (categories, budget range, city), "Reset personalization" (deletes interaction events and the interest profile), plain-language explanation page.

### 8.10 Admin and partner operations (ADM)

| ID | Requirement |
|---|---|
| ADM-01 | Admin console at `/admin` (role `ADMIN`) backed by `/api/v1/admin/**`: partners, categories, products (cost model, redemption method, validity, inventory adjustments with reason), offers and bundle composition with live margin preview and publish/pause/archive, campaigns, loyalty tasks, tiers, membership plan features, business settings (pricing, loyalty, membership, recommendation weights with sum = 100 validation and impact preview), users (search, assign/revoke Plus, suspend/reactivate, revoke sessions, points adjustment), orders (search, refund), redemptions (redeem by code), referral review queue, audit log viewer, partner performance report, recommendation metrics |
| ADM-02 | Every admin mutation writes an audit entry: actor, action, target, non-sensitive before/after values, timestamp, request id |
| ADM-03 | The admin role is never reachable through public flows — only `APP_ADMIN_EMAILS` bootstrap (and seed data in demo mode) |
| ADM-04 | Admin input is validated like customer input: plain text only (no HTML), https or allow-listed relative paths for URLs and images, numeric ranges enforced |
| ADM-05 | Partner performance report per partner and period: units sold, partner payout, sell-through % per product, refunds, redemption rate |
| ADM-06 | Partner portal is out of scope; a future `PARTNER` role would get APIs scoped to its own products, redemptions and reports |

### 8.11 Internationalization (I18N)

| ID | Requirement |
|---|---|
| I18N-01 | Locales `az` (default) and `en`. Language switcher in the header and in settings. Detection order: saved preference → browser language (`en*` → en) → az |
| I18N-02 | No hardcoded UI strings; all text through i18next namespaces; automated az/en key-parity test |
| I18N-03 | Backend messages (validation, errors, emails) from `messages_az.properties` / `messages_en.properties` (UTF-8), resolved from `Accept-Language` (the frontend sends the current UI language) or the saved preference for emails |
| I18N-04 | Content (categories, partners, products, offers, tasks, tiers, campaigns, plans) stored as `{az, en}`; both required on write; customer APIs return the requested locale, admin APIs return both |
| I18N-05 | Formatting: `Intl.NumberFormat(locale, {style: "currency", currency: "AZN", currencyDisplay: "narrowSymbol"})` (₼), dates and times with `Intl.DateTimeFormat` in `Asia/Baku`, i18next plural rules. Proper Azerbaijani letters (Ə ə, Ğ ğ, I ı, İ i, Ö ö, Ü ü, Ç ç, Ş ş); fonts must contain them and ₼ |
| I18N-06 | Never mix languages: a missing translation is a validation error at write time, not a fallback at read time. `<html lang>` follows the active locale |

### 8.12 Design and UX (UX)

Premium, modern, mobile-first, inspired by iOS interface design with restrained glassmorphism, depth, translucent layered surfaces, strong typography and fluid motion — without copying Apple's proprietary UI, fonts or colors. No generic AI landing-page look: no huge empty hero, no emoji icons, no fake testimonials or metrics, no excessive gradients, no repetitive card walls. Full specification in §17.

---

## 9. Non-functional requirements

| Area | Requirement |
|---|---|
| Performance | Catalog API p95 < 300 ms at demo scale. Home LCP < 2.5 s on a mid-range phone (4G throttling); CLS < 0.1; initial-route JS ≤ 200 KB gzip excluding the lazy 3D and admin chunks |
| Reliability | Idempotent checkout, payment confirmation and loyalty awards; scheduled jobs safe on multiple instances (`SKIP LOCKED`, idempotent updates); graceful shutdown |
| Security | §14; OWASP ASVS level 1 as the baseline for the MVP |
| Privacy | §15 |
| Accessibility | WCAG 2.2 AA target: contrast, keyboard navigation, visible focus, dialog focus management, reduced motion and reduced transparency |
| Observability | JSON logs in production with request id; no health-check endpoints (owner decision); no secrets, OTPs, tokens or raw personal data in logs (emails masked) |
| Maintainability | Package-by-feature, SOLID, no code comments, small components, approved dependency list, Prettier/ESLint on the frontend |
| Time | All timestamps stored in UTC; business periods computed in Asia/Baku; injected `Clock` |
| Portability | Runs identically with Docker Compose locally and on Render |

---

## 10. Architecture

### 10.1 Runtime topology
```
Browser (SPA)
   │  https://<frontend-host>/        UI and API on the same origin
   ▼
Nginx (frontend container)
   ├── /              static SPA, try_files → /index.html (client routes survive refresh)
   ├── /assets/*      hashed files, Cache-Control: public, max-age=31536000, immutable
   └── /api/*         proxy_pass ${API_UPSTREAM}
                          ▼
              Spring Boot backend (Java 21)
                 ├── PostgreSQL 16 (schema managed by Liquibase)
                 └── SMTP provider (Mailpit locally)
```
Local hot-reload development: the Vite dev server (port 5173) proxies `/api` to `localhost:8080`, so development is same-origin too.

### 10.2 Backend packages (package-by-feature)
```
backend/src/main/java/az/buta/
  ButaApplication.java
  common/          money, time, paging, Problem Details errors, i18n, LocalizedText JSONB type, web config
  security/        SecurityConfig, SessionAuthenticationFilter, CSRF and Origin check, RateLimiter, ClientIpResolver, CurrentUser
  audit/           audit log entity, service, admin read API
  auth/            OTP challenges, sessions, sign-in flow, profile completion, email change, admin bootstrap, dev mailbox
  user/            users, profile, avatar, preferences, privacy actions
  storage/         FileStorage (DatabaseFileStorage, LocalFileStorage)
  notification/    EmailSender (SmtpEmailSender, InMemoryEmailSender), EmailTemplateRenderer
  catalog/         categories, partners, products, inventory, offers, components, campaigns, search
  pricing/         PriceCalculator, MarginPolicy, BusinessSettingsService
  cart/
  order/           checkout, orders, reservations, redemptions, refunds, scheduled jobs
  payment/         PaymentProvider, SandboxPaymentProvider, payments; stripe/ (Stripe Checkout, webhooks, refunds)
  membership/
  loyalty/         accounts, ledger, tiers, task evaluators (one class per task type), referrals
  recommendation/  interactions, interest profiles, scorer, re-ranker, impressions, metrics
  demo/            DemoCatalogRefresher (active only in demo mode)
```
Each feature holds `XController`, `admin/AdminXController`, `XService`, `XRepository`, entities, `dto/` records and a mapper class. Features call each other through services only — never another feature's repository. Order events reach loyalty and recommendation through Spring application events published inside the transaction.

### 10.3 Layer rules
- **Controllers:** HTTP mapping, `@Valid` request DTOs, current user → service call → response DTO. No business logic.
- **Services:** `@Transactional` boundaries, business rules, ownership checks (queries scoped by user id, e.g. `findByIdAndUserId`).
- **Entities:** invariants and state transitions as intent-revealing methods (`order.markFulfilled(clock)`); no public setters for state fields; `@Version` on concurrently edited aggregates.
- **Repositories:** Spring Data JPA with parameter binding; inventory changes as `@Modifying` conditional updates; dynamic filters via Specifications; sort keys mapped from enums.
- **DTOs:** Java records; separate request and response types; admin DTOs separate from customer DTOs. Unknown JSON properties are rejected (`fail-on-unknown-properties=true`) — mass-assignment protection.
- **Replaceable infrastructure (interfaces):** `EmailSender`, `PaymentProvider`, `FileStorage`, `RateLimiter`, `Clock`.

### 10.4 Cross-cutting conventions
- Errors: RFC 9457 `application/problem+json` with stable `code`, localized `detail`, field `errors[]` and `requestId`.
- Request id: `X-Request-Id` accepted or generated, echoed in responses, placed in the logging MDC.
- JSON: instants as ISO-8601 UTC (`2026-10-17T11:00:00Z`); money as `{"amount": "28.00", "currency": "AZN"}`.
- `spring.jpa.open-in-view=false`; `server.error.include-stacktrace=never`; `server.error.include-message=never`.
- Scheduled jobs: every minute (expire pending orders), hourly (expire redemptions, complete orders), daily 03:00 Asia/Baku (membership expiry and qualification, purge sessions and OTP challenges, interaction retention, demo date roll-forward).

### 10.5 Approved dependencies
**Backend:** `spring-boot-starter-web`, `-validation`, `-security`, `-data-jpa`, `-mail`, `-cache`; `liquibase-core`; `postgresql`; `springdoc-openapi-starter-webmvc-ui` 2.8.x (Boot 3.5 line — confirm at build); `caffeine`. No test dependencies (§19). Not used: Lombok, MapStruct, JWT libraries, Thymeleaf (emails are rendered from `MessageSource` with HTML escaping).

**Frontend (latest at planning time; caret ranges except TypeScript):** `react` 19.3, `react-dom`, `react-router` 7.18, `@tanstack/react-query` 5.104, `i18next` 26.4, `react-i18next` 17.0, `react-hook-form`, `zod` 4.6, `@hookform/resolvers` (zod 4 compatibility verified at install), `lucide-react`, `three` 0.186, one self-hosted variable font via `@fontsource-variable/*`. Dev: `vite` 8.3, `@vitejs/plugin-react` 6.1, `typescript` ~6.0.3, `eslint` 9, `typescript-eslint` 8.71, `eslint-plugin-react-hooks`, `eslint-plugin-jsx-a11y`, `prettier`, `vitest` 5.0, `jsdom`, `@testing-library/react` 16.3, `@testing-library/user-event`, `@testing-library/jest-dom`, `@playwright/test`, `@types/react`, `@types/react-dom`, `@types/three`.

### 10.6 Frontend structure
```
frontend/
  index.html                     viewport-fit=cover, theme-init script reference
  public/theme-init.js           applies saved theme before first paint (external file, CSP-safe)
  public/images/demo/            locally generated SVG artwork for demo content
  nginx/default.conf.template
  src/
    app/                         App, router, providers, layouts (AppShell, AdminLayout)
    design-system/               tokens.css, global.css, components (Button, Chip, Card, Sheet, Dialog, Field, OtpInput, SegmentedControl, Toast, Skeleton, EmptyState, ErrorState, PriceTag, SavingsBadge, ProgressRing, TabBar)
    features/
      catalog/                   HomePage, OffersPage, OfferDetailPage, BundlesPage, CategoryPage, EventsPage, PartnersPage, PartnerPage, OfferCard, BundleComparison, FilterSheet, SearchBar
      auth/                      AuthPage, OtpStep, CompleteProfilePage, AuthProvider, RequireAuth, pendingAction
      cart/  checkout/  orders/
      account/                   AccountPage, SettingsPage, PrivacyPage, FavoritesPage
      loyalty/                   LoyaltyPage, TierProgress, TaskList, LedgerList, ReferralCard
      recommendations/           ForYouSection, ForYouPage, useImpressionTracking
      admin/                     resource pages (lazy-loaded chunk)
      info/                      AboutPage, HowItWorksPage, TermsPage, PrivacyPolicyPage, NotFoundPage
    hero3d/                      HeroScene (lazy-loaded chunk)
    shared/                      api (client, errors, types), i18n (az, en namespaces), format (money, date), hooks, safeReturnTo
    test/                        setup and helpers
```

---

## 11. Data model

Conventions: UUID primary keys generated by the application; `created_at` / `updated_at` `timestamptz NOT NULL`; `version BIGINT` on concurrently edited rows; money `NUMERIC(12,2)` with `currency CHAR(3) DEFAULT 'AZN' CHECK (currency = 'AZN')`; percentages `NUMERIC(5,2)`; points `BIGINT`; localized text `JSONB` with `CHECK (col ? 'az' AND col ? 'en')`; enums as `VARCHAR` + CHECK, mapped with `@Enumerated(STRING)`. Foreign keys indexed.

### 11.1 Identity, access, audit
- **users**: `id`, `email` (unique, normalized), `email_canonical` (indexed, not unique), `username` (unique, null until profile completion), `first_name`, `last_name`, `phone`, `status` (PROFILE_INCOMPLETE, ACTIVE, SUSPENDED), `avatar_file_id` → stored_files, `referral_code` (unique), `registration_ip_hash`, `created_at`, `updated_at`, `version`.
- **user_roles**: `user_id`, `role` (CUSTOMER, ADMIN), `source` (SYSTEM, BOOTSTRAP, SEED), `granted_by`, `granted_at`; PK (`user_id`, `role`).
- **user_sessions**: `id`, `user_id`, `token_hash` CHAR(64) unique, `created_at`, `last_seen_at`, `idle_expires_at`, `absolute_expires_at`, `revoked_at`, `revoke_reason`, `user_agent` (≤ 200 chars), `ip_hash`; indexes on `user_id`, `absolute_expires_at`.
- **otp_challenges**: `id`, `email`, `purpose` (SIGN_IN, EMAIL_CHANGE), `user_id` (EMAIL_CHANGE only), `code_hash` CHAR(64), `expires_at`, `attempt_count`, `max_attempts`, `consumed_at`, `locked_at`, `superseded_at`, `request_ip_hash`, `created_at`; index (`email`, `purpose`, `created_at` DESC).
- **user_preferences**: `user_id` PK, `locale` (az, en), `theme` (LIGHT, DARK, SYSTEM), `personalization_enabled`, `personalization_decided_at`, `preferred_category_ids` UUID[], `budget_min`, `budget_max`, `city`, `updated_at`.
- **stored_files**: `id`, `owner_user_id`, `purpose` (AVATAR), `content_type`, `size_bytes`, `sha256`, `storage_provider` (DATABASE, LOCAL), `storage_key`, `data` BYTEA (database provider), `created_at`.
- **audit_logs**: `id`, `actor_user_id` (null for system), `actor_type` (USER, ADMIN, SYSTEM), `action`, `target_type`, `target_id`, `details` JSONB (never secrets), `request_id`, `ip_hash`, `created_at`; indexes (`target_type`, `target_id`), (`actor_user_id`, `created_at`), (`created_at`).
- **business_settings**: `key` PK (pricing, loyalty, membership, recommendation), `value` JSONB (validated by typed records), `updated_by`, `updated_at`, `version`.

### 11.2 Catalog
- **categories**: `id`, `slug` unique, `name` JSONB, `icon_key`, `sort_order`, `active`, timestamps.
- **partners**: `id`, `slug` unique, `name`, `description` JSONB, `city`, `address`, `website_url`, `logo_path`, `demo`, `status` (ACTIVE, INACTIVE), timestamps, `version`.
- **partner_products**: `id`, `partner_id`, `category_id`, `product_type`, `title`/`description`/`terms`/`redemption_instructions` JSONB, `eligibility_note` JSONB (nullable), `retail_price`, `currency`, `cost_model` (WHOLESALE, COMMISSION), `unit_net_cost`, `commission_percent`, `valid_from`, `valid_until`, `event_starts_at`, `event_ends_at`, `venue_name`, `venue_address`, `city`, `redemption_method`, `refundable`, `refund_deadline_hours`, `restock_on_refund`, `status` (ACTIVE, INACTIVE), timestamps, `version`. CHECK: `(cost_model = 'WHOLESALE' AND unit_net_cost >= 0) OR (cost_model = 'COMMISSION' AND commission_percent BETWEEN 0 AND 100)`; `valid_until > valid_from`. Indexes: `partner_id`, `category_id`, `valid_until`, `event_starts_at`.
- **product_inventory**: `product_id` PK, `total_quantity`, `reserved_quantity`, `sold_quantity`, `updated_at`. CHECK `reserved_quantity >= 0 AND sold_quantity >= 0 AND reserved_quantity + sold_quantity <= total_quantity`.
- **inventory_movements**: `id`, `product_id`, `movement_type` (ADJUSTMENT, RESERVE, RELEASE, SELL, RESTOCK), `quantity`, `order_id`, `reason`, `actor_user_id`, `created_at`.
- **offers**: `id`, `slug` unique, `offer_type` (SINGLE, BUNDLE), `primary_category_id`, `title`/`subtitle`/`description` JSONB, `image_path`, `selling_price`, `currency`, `visibility` (PUBLIC, PLUS_ONLY), `status` (DRAFT, PUBLISHED, PAUSED, ARCHIVED), `sale_starts_at`, `sale_ends_at`, `max_per_order` (1–10), `business_priority` (0–100), `featured`, `tags` TEXT[], `search_key`, `published_at`, timestamps, `version`. Indexes: (`status`, `sale_ends_at`), `primary_category_id`, partial index on `featured`, trigram GIN on `search_key` when `pg_trgm` is available.
- **offer_components**: `id`, `offer_id`, `product_id`, `quantity` (1–10), `negotiated_unit_cost` (nullable, ≥ 0), `sort_order`; unique (`offer_id`, `product_id`).
- **campaigns**: `id`, `slug` unique, `name`/`banner_text` JSONB, `audience` (ALL, PLUS), `starts_at`, `ends_at`, `bonus_points_per_unit`, `featured`, `status`, timestamps.
- **campaign_offers**: `campaign_id`, `offer_id`; PK both.

### 11.3 Commerce
- **carts**: `id`, `user_id` unique, `updated_at`.
- **cart_items**: `id`, `cart_id`, `offer_id`, `quantity`, `unit_price_at_add`, `added_at`; unique (`cart_id`, `offer_id`).
- **orders**: `id`, `order_number` unique (`BT-` + zero-padded sequence `order_number_seq`), `user_id`, `status`, `currency`, `subtotal_amount`, `points_redeemed`, `points_discount_amount`, `total_amount` (cash), `checkout_idempotency_key`, `expires_at`, `fulfilled_at`, `completed_at`, `cancelled_at`, `cancel_reason`, timestamps, `version`. Unique (`user_id`, `checkout_idempotency_key`); indexes (`user_id`, `created_at` DESC), (`status`, `expires_at`).
- **order_items**: `id`, `order_id`, `offer_id`, `offer_type`, `title_snapshot` JSONB, `quantity`, `unit_price`, `line_subtotal`, `points_discount_allocated`, `cash_paid`, `partner_cost_total`, `payment_fee_allocated`, `expected_loyalty_cost`, `contribution_margin`, `points_earned`, `refunded_quantity`, `recommendation_request_id`, `created_at`.
- **order_item_components**: `id`, `order_item_id`, `product_id`, `partner_id`, `quantity`, `allocated_price`, `partner_cost` — snapshot for partner settlement and reports.
- **payments**: `id`, `order_id` unique, `provider` (SANDBOX, STRIPE), `provider_reference`, `status`, `amount`, `currency`, `refunded_amount`, `captured_at`, `failed_at`, `failure_reason`, timestamps, `version`.
- **refunds**: `id`, `order_id`, `payment_id`, `amount`, `reason`, `initiated_by` (CUSTOMER, ADMIN), `actor_user_id`, `status` (SUCCEEDED, FAILED), `provider_reference`, `created_at`.
- **redemptions**: `id`, `order_item_id`, `product_id`, `code` unique (12 characters, Crockford Base32, displayed as `XXXX-XXXX-XXXX`), `status`, `valid_from`, `valid_until`, `redeemed_at`, `redeemed_by`, `voided_at`, `created_at`; index (`status`, `valid_until`).
- **favorites**: `user_id`, `offer_id`, `created_at`; PK (`user_id`, `offer_id`).

### 11.4 Membership and loyalty
- **membership_plans**: `code` PK (FREE, PLUS), `name`/`description` JSONB, `features` TEXT[], `price` (nullable), `billing_period` (nullable), `purchasable` (default false), `updated_at`.
- **memberships**: `user_id` PK, `plan_code`, `source` (DEFAULT, EARNED, ADMIN, SUBSCRIPTION), `starts_at`, `ends_at`, `granted_by`, `reason`, `updated_at`, `version`. History lives in the audit log.
- **loyalty_accounts**: `user_id` PK, `available_points`, `lifetime_earned_points`, `lifetime_redeemed_points`, `tier_qualifying_points`, `tier_code`, `flagged_for_review`, `updated_at`, `version`.
- **loyalty_ledger_entries**: `id`, `user_id`, `entry_type`, `points` (non-zero), `idempotency_key` unique, `source_type` (ORDER_ITEM, ORDER, TASK, ADMIN), `source_id`, `reverses_entry_id`, `description_key`, `description_params` JSONB, `actor_user_id`, `created_at`; index (`user_id`, `created_at` DESC). CHECK: earn types > 0, `REDEEM` < 0.
- **loyalty_tiers**: `code` PK, `name` JSONB, `threshold_points` unique, `earn_multiplier` NUMERIC(4,2), `benefits` JSONB, `sort_order`.
- **loyalty_tasks**: `id`, `code` unique, `task_type`, `title`/`description`/`terms` JSONB, `params` JSONB, `reward_points`, `period_type` (ONE_TIME, CALENDAR_MONTH), `starts_at`, `ends_at`, `active`, `min_order_amount`, timestamps, `version`.
- **task_progress**: `id`, `user_id`, `task_id`, `period_key` (`ONE_TIME` or `2026-10`), `progress_value`, `target_value`, `status` (IN_PROGRESS, COMPLETED), `completed_at`, `awarded`, `award_seq`, `updated_at`, `version`; unique (`user_id`, `task_id`, `period_key`).
- **referrals**: `id`, `referrer_user_id`, `referee_user_id` unique, `code_used`, `status` (PENDING, QUALIFIED, UNDER_REVIEW, REJECTED, REVERSED), `qualifying_order_id`, `review_note`, `reviewed_by`, timestamps.

### 11.5 Personalization
- **interaction_events**: `id`, `user_id` (nullable), `event_type` (DETAIL_VIEW, SEARCH, CATEGORY_VIEW, CART_ADD, FAVORITE_ADD, FAVORITE_REMOVE, PURCHASE, RECOMMENDATION_CLICK), `offer_id`, `category_id`, `query_text` (≤ 100), `recommendation_request_id`, `placement`, `occurred_at`; indexes (`user_id`, `occurred_at` DESC), (`offer_id`, `event_type`, `occurred_at`).
- **user_interest_profiles**: `user_id` PK, `category_affinity` JSONB, `partner_affinity` JSONB, `price_band_low`, `price_band_high`, `price_sensitivity`, `signals_count`, `computed_at`.
- **recommendation_impressions**: `id`, `request_id`, `user_id` (nullable), `placement`, `algorithm_version`, `variant`, `offer_id`, `position`, `score` NUMERIC(6,2), `components` JSONB, `reason_code`, `served_at`; indexes (`user_id`, `offer_id`, `served_at`), (`request_id`).

---

## 12. Liquibase migration plan

```
backend/src/main/resources/db/changelog/
  db.changelog-master.yaml
  changes/
    001-extensions.sql                 phase 0  pg_trgm (guarded by precondition)
    002-identity-audit-settings.sql    phase 0  users, roles, sessions, otp, preferences, stored_files, audit_logs, business_settings
    003-settings-defaults.sql          phase 0  default business settings (all environments)
    004-catalog.sql                    phase 1  categories, partners, products, inventory, movements, offers, components, campaigns
    005-catalog-reference-data.sql     phase 1  the 9 categories (all environments)
    006-commerce.sql                   phase 3  carts, orders, items, components, payments, refunds, redemptions, favorites, order_number_seq
    007-membership-loyalty.sql         phase 4  plans, memberships, accounts, ledger, tiers, tasks, task_progress, referrals
    008-loyalty-reference-data.sql     phase 4  FREE/PLUS plans and default tiers (all environments)
    009-personalization.sql            phase 5  interaction_events, user_interest_profiles, recommendation_impressions
    901-demo-catalog.sql               phase 1  context demo
    902-demo-users.sql                 phase 2  context demo
    903-demo-orders.sql                phase 3  context demo
    904-demo-loyalty.sql               phase 4  context demo
    010-owner-accounts.sql                      owner admin accounts (all environments)
    011-email-verification.sql                  users.email_verified_at
    012-content-wording.sql                     demo content wording cleanup
    013-plus-subscription.sql                   paid Plus plan, membership_purchases
    014-stripe-payments.sql                     STRIPE payment provider, purchase provider reference
    905-real-partners.sql                       context demo: real Azerbaijani cultural partners
```

**Master changelog** lists every file explicitly, schema files (0xx) first and demo files (9xx) last:
```yaml
databaseChangeLog:
  - include:
      file: db/changelog/changes/001-extensions.sql
  - include:
      file: db/changelog/changes/002-identity-audit-settings.sql
```

**Formatted SQL changeset style:**
```sql
--liquibase formatted sql

--changeset buta:004-01
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    slug VARCHAR(60) NOT NULL UNIQUE,
    name JSONB NOT NULL CHECK (name ? 'az' AND name ? 'en')
);
--rollback DROP TABLE categories;

--changeset buta:001-01
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT count(*) FROM pg_available_extensions WHERE name = 'pg_trgm'
CREATE EXTENSION IF NOT EXISTS pg_trgm;

--changeset buta:901-01 context:demo
INSERT INTO partners (...) VALUES (...);
```

**Rules**
- `spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml`; Hibernate `ddl-auto=validate`.
- One logical change per changeset, each with a `--rollback`.
- **Contexts pitfall:** when no context is set, Liquibase runs every changeset, including `demo` ones. Always set `spring.liquibase.contexts` explicitly through `APP_LIQUIBASE_CONTEXTS`: production `default`; local and demo deployments `default,demo`.
- Reference data (`003`, `005`, `008`) is insert-only (`ON CONFLICT DO NOTHING`) so it never overwrites admin edits.
- Demo data uses fixed UUIDs and `now()`-relative dates; `DemoCatalogRefresher` rolls expired demo dates forward at startup and daily (demo mode only).
- Until the first shared deployment, changesets may still be edited (reset locally with `docker compose down -v`). After the first Render deployment they are immutable; fixes go into new changesets.
- Migrations run automatically at application startup; Liquibase's `DATABASECHANGELOGLOCK` prevents concurrent runs across instances. Readiness is reported only after migrations finish.
- `pg_trgm` is a trusted extension on PostgreSQL 13+, so the database owner can install it; if unavailable, the guarded changesets are skipped and search falls back to `ILIKE` on `search_key`.
- Integration tests apply the same changelog to a Testcontainers PostgreSQL 16; one test also applies the `demo` context and asserts every demo offer passes the margin rules and every demo ledger reconciles.

---

## 13. REST API

### 13.1 Conventions
- Base path `/api/v1`, JSON, `Accept-Language: az` or `en`.
- Authentication by session cookie; `X-XSRF-TOKEN` header on every non-GET request.
- Paging: `?page=0&size=24&sort=PRICE_ASC` → `{"items": [...], "page": 0, "size": 24, "totalItems": 123, "totalPages": 6}`.
- Status codes: 200/201/202/204; 400 validation; 401 `AUTH_REQUIRED`; 403 forbidden or CSRF; 404 not found (also returned for other users' resources); 409 conflicts (inventory, state, idempotency); 422 business rules (e.g. `MARGIN_RULE_FAILED`); 429 rate limit with `Retry-After`; 500 generic with request id only.
- OpenAPI/Swagger UI at `/api/v1/docs`: enabled in dev and demo, disabled in production unless `APP_API_DOCS_ENABLED=true`.

Error body example:
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "detail": "Bəzi sahələr düzgün doldurulmayıb",
  "errors": [{"field": "username", "code": "USERNAME_TAKEN", "message": "Bu istifadəçi adı artıq tutulub"}],
  "requestId": "4f1c2a9e7b3d"
}
```
Error codes (each with az/en messages): `VALIDATION_FAILED`, `AUTH_REQUIRED`, `FORBIDDEN`, `CSRF_INVALID`, `NOT_FOUND`, `RATE_LIMITED`, `OTP_INVALID`, `OTP_LOCKED`, `PROFILE_INCOMPLETE`, `USERNAME_TAKEN`, `EMAIL_UNAVAILABLE`, `OFFER_UNAVAILABLE`, `OFFER_NOT_SELLABLE`, `PLUS_REQUIRED`, `INSUFFICIENT_INVENTORY`, `QUANTITY_LIMIT`, `INSUFFICIENT_POINTS`, `POINTS_REDEMPTION_LIMIT`, `ORDER_STATE_INVALID`, `REFUND_NOT_ALLOWED`, `MARGIN_RULE_FAILED`, `FILE_TYPE_NOT_ALLOWED`, `FILE_TOO_LARGE`, `IDEMPOTENCY_CONFLICT`, `INTERNAL_ERROR`.

### 13.2 Public (no session required)
| Method | Path | Purpose |
|---|---|---|
| GET | `/config` | Client config: locales, default locale, demo mode, payment mode, feature flags |
| GET | `/home` | Aggregated home sections (featured campaign, popular, best-value bundles, this week's events, ending soon, categories) |
| GET | `/categories` | Active categories |
| GET | `/partners` · `/partners/{slug}` | Partners and partner detail with offers |
| GET | `/offers` | Search/filter/sort/paginate: `q`, `category`, `offerType`, `productType`, `partner`, `city`, `minPrice`, `maxPrice`, `eventFrom`, `eventTo`, `plusOnly`, `includeSoldOut`, `sort`, `page`, `size` |
| GET | `/offers/{slug}` | Offer detail (bundle components, savings, validity, terms, availability state, points preview when Plus) |
| GET | `/recommendations` | `placement` (HOME, FOR_YOU, OFFER_DETAIL), `offerId`, `limit`; non-personalized for anonymous and FREE users |
| POST | `/interactions` | Batch of up to 50 behavioral events; ≤ 32 KB; rate-limited; ignored when personalization is off |

### 13.3 Authentication
| Method | Path | Purpose |
|---|---|---|
| GET | `/auth/session` | `{authenticated, user?}` (id, username, names, status, roles, membership); issues the `XSRF-TOKEN` cookie |
| POST | `/auth/otp/request` | `{email}` → 202 generic |
| POST | `/auth/otp/verify` | `{email, code}` → 200 `{nextStep: DONE or COMPLETE_PROFILE, user}` + session cookie |
| POST | `/auth/complete-profile` | `{firstName, lastName, username, referralCode?}` |
| GET | `/auth/username-availability?username=` | Rate-limited availability check |
| POST | `/auth/logout` · `/auth/logout-all` | Revoke the current session or all sessions |
| GET | `/dev/mailbox?email=` | Dev profile + explicit flag only (AUTH-11) |

### 13.4 Account (session required)
| Method | Path | Purpose |
|---|---|---|
| GET / PATCH | `/me` | Profile; PATCH accepts only `firstName`, `lastName`, `username`, `phone` |
| GET / PUT / DELETE | `/me/avatar` | Owner-only avatar (PUT is multipart) |
| POST | `/me/email-change` · `/me/email-change/confirm` | Verified email change (AUTH-27) |
| GET / PUT | `/me/preferences` | Locale, theme, personalization toggle, categories, budget, city |
| DELETE | `/me/personalization-data` | Reset interactions and interest profile |
| GET | `/me/favorites` | Favorites |
| PUT / DELETE | `/me/favorites/{offerId}` | Add or remove a favorite (idempotent) |
| GET | `/me/membership` | Plan, source, dates, qualifying-spend progress |
| GET | `/me/loyalty` | Available, earned, redeemed, tier-qualifying points, tier progress, qualifying spend |
| GET | `/me/loyalty/ledger` | Paged points history |
| GET | `/me/loyalty/tasks` | Active tasks with progress, rewards and terms; completed tasks |
| GET | `/me/referral` | Referral code, link and statuses |

### 13.5 Commerce (session required)
| Method | Path | Purpose |
|---|---|---|
| GET | `/cart` | Cart with server prices, availability and price-change flags |
| POST | `/cart/items` | `{offerId, quantity}` |
| PATCH / DELETE | `/cart/items/{itemId}` | Change quantity or remove |
| POST | `/checkout/quote` | `{pointsToRedeem}` → server quote |
| POST | `/checkout` | `{pointsToRedeem}` + `Idempotency-Key` header → order + payment |
| POST | `/payments/{paymentId}/sandbox-confirmation` | `{outcome: SUCCEEDED or FAILED}`; exists only while the sandbox provider is active |
| POST | `/payments/{paymentId}/stripe-checkout` | Stripe only: creates a Checkout session for the order and returns its `url` |
| POST | `/payments/stripe/sessions/{sessionId}/confirmation` | Stripe only: confirms a session on return from Stripe (owner-checked) |
| POST | `/me/plus/stripe-checkout` | Stripe only: Checkout session for buying Plus |
| POST | `/payments/stripe/webhook` | Stripe only: signed webhook (`checkout.session.completed`, `checkout.session.async_payment_succeeded`); no session or CSRF token |
| GET | `/orders` · `/orders/{id}` | Owner-only order history and detail with vouchers |
| POST | `/orders/{id}/cancel` | `PENDING_PAYMENT` only |
| POST | `/orders/{id}/refund` | `{orderItemIds?}` — default all eligible units |

### 13.6 Admin (role ADMIN)
| Method | Path |
|---|---|
| GET, POST / GET, PUT | `/admin/partners` · `/admin/partners/{id}` · GET `/admin/partners/{id}/performance` |
| GET, POST / PUT | `/admin/categories` · `/admin/categories/{id}` |
| GET, POST / GET, PUT | `/admin/products` · `/admin/products/{id}` · POST `/admin/products/{id}/inventory-adjustments` `{delta, reason}` |
| GET, POST / GET, PUT | `/admin/offers` · `/admin/offers/{id}` · PUT `/admin/offers/{id}/components` |
| POST | `/admin/offers/margin-preview` · `/admin/offers/{id}/publish` · `/pause` · `/archive` |
| GET, POST / PUT | `/admin/campaigns` · `/admin/campaigns/{id}` |
| GET, POST / PUT | `/admin/loyalty/tasks` · `/admin/loyalty/tasks/{id}` |
| GET / PUT | `/admin/loyalty/tiers` · `/admin/membership/plans/{code}` |
| GET / PUT | `/admin/settings/{key}` · POST `/admin/settings/{key}/impact-preview` |
| GET | `/admin/users?query=` · `/admin/users/{id}` |
| POST | `/admin/users/{id}/membership` · `/suspend` · `/reactivate` · `/revoke-sessions` · `/loyalty-adjustments` |
| GET / POST | `/admin/orders` · `/admin/orders/{id}` · POST `/admin/orders/{id}/refunds` |
| POST | `/admin/redemptions/{code}/redeem` |
| GET / POST | `/admin/referrals?status=UNDER_REVIEW` · POST `/admin/referrals/{id}/approve` · `/reject` |
| GET | `/admin/audit-logs` · `/admin/recommendations/metrics` |

Margin preview response example:
```json
{
  "sellingPrice": "28.00", "referenceRetail": "35.00", "savings": "7.00", "savingsPercent": "20.00",
  "components": [
    {"productId": "…", "quantity": 1, "allocatedPrice": "16.00", "costModel": "WHOLESALE", "partnerCost": "16.00"},
    {"productId": "…", "quantity": 1, "allocatedPrice": "12.00", "costModel": "WHOLESALE", "partnerCost": "9.00"}
  ],
  "partnerCostTotal": "25.00", "paymentFee": "0.90", "expectedLoyaltyCost": "0.25", "otherVariableCost": "0.30",
  "contributionMargin": "1.55", "marginPercent": "5.54",
  "rules": [
    {"code": "NON_NEGATIVE_SAVINGS", "passed": true},
    {"code": "MIN_MARGIN_AMOUNT", "threshold": "1.00", "passed": true},
    {"code": "MIN_MARGIN_PERCENT", "threshold": "5.00", "passed": true}
  ],
  "sellable": true
}
```

### 13.7 Operations
No actuator or health-check endpoints are exposed (owner decision).

---

## 14. Security design

### 14.1 Threat model (summary)
Assets: accounts and sessions, OTP codes, orders and vouchers, loyalty points (monetary value), partner cost data (commercially sensitive), personal data.

| Threat | Mitigation |
|---|---|
| OTP brute force | 5 attempts per challenge, per-email lockout, per-IP limits, 10-minute TTL, peppered HMAC hashes, constant-time compare |
| OTP replay | Atomic single-use consumption; new requests supersede old challenges |
| Account enumeration | One unified sign-in flow; identical responses and limits for known and unknown emails; generic email-change responses |
| Session theft via XSS | HttpOnly cookie; no tokens in JavaScript or `localStorage`; strict CSP; React escaping |
| CSRF | SameSite=Lax session cookie + CSRF token header + Origin check |
| Session fixation | New token on every sign-in; pre-login tokens are never upgraded |
| IDOR | Every user-owned query scoped by the authenticated user id; 404 for other users' resources; UUIDs |
| Mass assignment, privilege escalation | Explicit request DTOs; unknown fields rejected; roles and membership from the database; admin routes protected by URL rules **and** method security |
| Price, discount, points or status tampering | All money, savings, points and statuses computed server-side; client-sent values ignored |
| Overselling / race conditions | Conditional atomic inventory updates + CHECK constraints; deterministic lock order |
| Points double spend | Account row lock + ledger idempotency keys |
| Duplicate rewards | Unique idempotency keys; recompute-based task evaluation |
| Referral and task abuse | Canonical-email checks, caps, minimum order values, review queue, reversals on refund |
| SQL injection | Parameter binding only; whitelisted sort enums; Specifications for filters |
| XSS through content | Plain-text content only; no `dangerouslySetInnerHTML` (lint rule); URL scheme validation; CSP |
| Malicious uploads | Type, size and magic-byte checks; re-encoding; `nosniff`; owner-only serving |
| Open redirect | `returnTo` accepted only as a relative path (starts with `/`, not `//`, no backslash or scheme) |
| Rate-limit bypass with spoofed `X-Forwarded-For` | Forwarded headers trusted only from configured proxies (`APP_TRUSTED_PROXIES`); OTP limits per email do not depend on IP |
| Information leakage | Generic 500 with request id; no stack traces; masked logs; partner costs visible to admins only |
| Secret exposure | Secrets only from environment variables; `.env` ignored by git; `.env.example` holds placeholders; production refuses to start without required secrets |
| Clickjacking | `frame-ancestors 'none'` |
| Admin account compromise | Admin only via bootstrap list; audit log; revoke-sessions; shorter idle timeout for admin sessions (12 h) |
| Development features leaking into production | Startup guards (§14.3) |

### 14.2 Security headers
Set by Nginx for the SPA and by Spring Security for API responses:
- `Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob: https:; font-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'`
- `Strict-Transport-Security: max-age=31536000; includeSubDomains` (production only)
- `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy: camera=(), microphone=(), geolocation=()`
- API responses: `Cache-Control: no-store`.

### 14.3 Startup guards (`prod` profile)
The application refuses to start when any of these hold:
- `APP_OTP_PEPPER` or `APP_IP_HASH_SECRET` missing or shorter than 32 bytes;
- `APP_DEV_MAILBOX_ENABLED=true` or the in-memory email provider is selected;
- sandbox payments are active while `APP_DEMO_MODE` is not `true` (sandbox is only allowed in an explicitly labeled demo);
- session cookie not marked Secure.

### 14.4 Production security checklist (copied into SECURITY.md)
- [ ] All secrets set in Render, generated randomly, never committed; rotation procedure documented
- [ ] HTTPS only, HSTS enabled, `__Host-` session cookie, custom domain configured
- [ ] Gmail sender account protected with 2-Step Verification; app password stored only in Render
- [ ] Dev mailbox disabled, API docs disabled, `demo` Liquibase context off for real production
- [ ] `APP_ADMIN_EMAILS` reviewed; admin accounts reviewed quarterly
- [ ] Database backups / point-in-time recovery enabled; restore tested
- [ ] Application database user is not a superuser
- [ ] Dependency scanning (Gradle dependency scanning, `npm audit`) and container image scanning in CI
- [ ] Logs verified free of OTPs, tokens, cookies and raw emails; retention configured
- [ ] Rate limits tuned; distributed rate limiting in place before running more than one backend instance
- [ ] Monitoring and alerting (health, error rate, latency, failed logins, OTP lockouts)
- [x] Payment provider integrated through a hosted payment page with signed webhooks (Stripe Checkout; no card data touches our servers)
- [ ] Stripe live keys and live webhook signing secret configured only in the hosting environment
- [ ] Privacy policy, terms of service, partner agreements and refund policy reviewed by legal counsel
- [ ] Penetration test and accessibility audit before public launch

---

## 15. Privacy

| Data | Purpose | Retention |
|---|---|---|
| Email, names, username, optional phone, avatar | Account and order fulfilment | Account lifetime |
| Orders, vouchers, payments | Fulfilment, refunds, accounting | Per legal requirements (to be confirmed) |
| Loyalty ledger | Points and audit | Account lifetime |
| Interaction events (opt-in) | Personalization and evaluation | 180 days, then deleted |
| Interest profile | Personalization | Until reset or personalization disabled |
| Session metadata (truncated user agent, IP hash) | Security | Purged 30 days after expiry |
| OTP challenges | Authentication | Deleted after 7 days |
| Audit logs | Accountability | 1 year (configurable) |

- IP addresses are stored only as keyed hashes (`APP_IP_HASH_SECRET`); raw IPs exist only in memory for rate limiting.
- No sensitive attributes are collected or inferred; recommendations never use them.
- Users can view and edit their profile, toggle personalization, edit preferences and reset personalization data. Export and deletion are production prerequisites (§23).
- Logs never contain OTPs, tokens, cookies or full emails (masked as `j***@example.com`).
- Demo data is synthetic only; demo users use the reserved `.test` domain.
- Azerbaijan's personal-data legislation and any applicable foreign rules must be reviewed by legal counsel before launch.

---

## 16. Configuration

Operational and security settings come from environment variables. Business rules that admins tune live in `business_settings` (database, audited).

### 16.1 Backend environment variables
| Variable | Purpose | Local default | Production |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` or `prod` (`test` used by tests) | `dev` | `prod` |
| `PORT` | HTTP port (Render injects it) | `8080` | set by Render |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Database; URL built as `jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?sslmode=${DB_SSL_MODE}` | localhost, 5432, buta, buta, buta | from Render database |
| `DB_SSL_MODE` | pgjdbc SSL mode | `prefer` | `prefer` (internal) or `require` (external) |
| `DB_POOL_SIZE` | Hikari maximum pool size | `10` | sized to the database plan |
| `APP_LIQUIBASE_CONTEXTS` | Liquibase contexts (§12) | `default,demo` | `default` (`default,demo` for a demo deployment) |
| `APP_DEMO_MODE` | Demo banner, demo refresher, sandbox allowance | `true` | `false` (`true` for a demo deployment) |
| `APP_PUBLIC_BASE_URL` | Public URL used in emails and the Origin check | `http://localhost:5173` | frontend URL |
| `APP_ALLOWED_ORIGINS` | Extra allowed origins (split-domain deployments only) | empty | empty |
| `APP_TRUSTED_PROXIES` | Proxies whose `X-Forwarded-For` is trusted | loopback, private ranges | private network ranges |
| `APP_SESSION_COOKIE_NAME` | Session cookie name | `buta_session` | `__Host-buta_session` |
| `APP_SESSION_COOKIE_SECURE` | Secure cookie flag | `false` | `true` |
| `APP_SESSION_IDLE_TIMEOUT` / `APP_SESSION_ABSOLUTE_TIMEOUT` | Session lifetimes (ISO-8601) | `P7D` / `P30D` | same |
| `APP_OTP_PEPPER` | OTP HMAC secret (≥ 32 bytes) | dev-only value in `.env.example` | generated secret |
| `APP_IP_HASH_SECRET` | Keyed hashing of IP addresses (≥ 32 bytes) | dev-only value | generated secret |
| `APP_OTP_TTL`, `APP_OTP_MAX_ATTEMPTS`, `APP_OTP_RESEND_COOLDOWN` | OTP rules | `PT10M`, `5`, `PT60S` | same |
| `APP_OTP_MAX_PER_EMAIL_HOUR`, `APP_OTP_MAX_PER_IP_HOUR` | OTP request limits | `5`, `20` | tuned |
| `APP_ADMIN_EMAILS` | Admin bootstrap list | `admin@buta.test` | real admin emails |
| `MAIL_USERNAME`, `MAIL_APP_PASSWORD` | Gmail address and app password (smtp.gmail.com:587, STARTTLS); sender is `Buta <MAIL_USERNAME>` | unused (`dev` profile sends to Mailpit) | required |
| `APP_DEV_MAILBOX_ENABLED` | Dev mailbox endpoint (dev profile only) | `false` | must be `false` |
| `APP_PAYMENT_PROVIDER` | Payment provider | `sandbox` | `sandbox`, or `stripe` for Stripe Checkout |
| `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` | Stripe secret key and webhook signing secret (required when the provider is `stripe`) | — | — |
| `APP_STORAGE_PROVIDER`, `APP_STORAGE_LOCAL_DIR` | `database` or `local` | `database`, `./data/uploads` | `database` |
| `APP_AVATAR_MAX_BYTES` | Upload limit | `2097152` | same |
| `APP_API_DOCS_ENABLED` | Swagger UI | `true` | `false` |
| `JAVA_OPTS` | JVM flags | empty | `-XX:MaxRAMPercentage=75` |

### 16.2 Frontend configuration
| Variable | When | Purpose | Default |
|---|---|---|---|
| `VITE_API_BASE_URL` | build time | API base path | `/api` |
| `VITE_DEFAULT_LOCALE` | build time | Default locale | `az` |
| `API_UPSTREAM` | container start (Nginx) | Backend address for `/api` proxying | `http://backend:8080` (Compose) |
| `PORT` | container start | Nginx listen port | `8080` |

### 16.3 Business settings (database defaults, admin-editable)
```json
{
  "pricing": {
    "paymentFeePercent": "2.50", "paymentFeeFixed": "0.20",
    "otherVariableCostPercent": "0.00", "otherVariableCostFixed": "0.30",
    "minMarginAmount": "1.00", "minMarginPercent": "5.00",
    "lowStockThreshold": 5, "reservationTtlMinutes": 15
  },
  "loyalty": {
    "pointsPerAzn": 1, "pointValueAzn": "0.01", "expectedRedemptionPercent": "70.00",
    "redemptionStepPoints": 100, "minRedeemPoints": 100, "maxPointsDiscountPercent": "20.00",
    "awardTrigger": "FULFILLED", "tierWindowMonths": 0,
    "referral": { "minFirstOrderAmount": "10.00", "maxRewardsPerMonth": 5, "maxRefereeAccountAgeDays": 30 }
  },
  "membership": {
    "plusQualifyingSpendAmount": "100.00", "plusQualifyingWindowDays": 180, "earnedPlusDurationDays": 365
  },
  "recommendation": {
    "weights": { "interest": 30, "budget": 20, "value": 15, "loyalty": 15, "discovery": 10, "business": 10 },
    "signalWeights": { "purchase": 5.0, "explicitPreference": 4.0, "favorite": 3.0, "cartAdd": 2.0, "detailView": 0.5, "search": 0.5 },
    "minRelevance": 20, "explorationPercent": 15, "decayHalfLifeDays": 60, "eventRetentionDays": 180,
    "frequencyCap": { "maxServes": 3, "windowDays": 7 },
    "diversity": { "maxPerPartnerTop10": 2, "maxPerCategoryTop10": 3 },
    "variants": [ { "name": "control", "allocationPercent": 100 } ]
  }
}
```
All values are placeholders for business validation (e.g. the payment fee must come from the real acquirer contract).

---

## 17. Frontend specification

### 17.1 Routes
| Path | Page | Access |
|---|---|---|
| `/` | Home | public |
| `/offers` | Explore: search, filters, sort; tabs All / Bundles / Single | public |
| `/offers/:slug` | Offer detail | public |
| `/bundles` | Bundles with comparison | public |
| `/categories/:slug` | Category | public |
| `/events` | Upcoming events by date | public |
| `/partners` · `/partners/:slug` | Partners | public |
| `/about` · `/how-it-works` · `/terms` · `/privacy` | Info pages | public |
| `/auth` | Email → OTP | public (signed-in users are redirected) |
| `/auth/complete-profile` | Profile completion | status PROFILE_INCOMPLETE |
| `/cart` | Cart | signed in |
| `/checkout` | Review and points | signed in |
| `/checkout/pay/:orderId` | Payment step (sandbox or Stripe Checkout) | signed in, owner |
| `/orders` · `/orders/:id` | Orders and vouchers | signed in, owner |
| `/for-you` | Personalized recommendations (FREE users see the Plus explainer) | signed in |
| `/loyalty` | Points, tier, tasks, history, referral (FREE users see qualifying-spend progress) | signed in |
| `/account` · `/account/settings` · `/account/privacy` · `/account/favorites` | Account area | signed in |
| `/admin/*` | Admin console (lazy chunk) | ADMIN |
| `*` | Not found | public |

Mobile bottom tab bar: Home, Explore, For you, Cart, Account. Desktop: glass top navigation with search.

### 17.2 State and data
- Server state with TanStack Query (query keys per feature; catalog `staleTime` 60 s; cart and session always fresh).
- React contexts only for session (`AuthProvider` backed by `GET /auth/session`), locale and theme. No global store library.
- Forms with react-hook-form + zod; validation messages come from i18n keys; server field errors are mapped onto fields.

### 17.3 API client
- Thin typed wrapper over `fetch`: `credentials: "same-origin"`, `Accept-Language` from the active locale, `X-XSRF-TOKEN` read from the `XSRF-TOKEN` cookie on non-GET requests, `X-Request-Id` per request.
- Problem Details parsed into `ApiError {status, code, message, fieldErrors, requestId}`; `code` maps to `errors.<CODE>` translations with a generic fallback.
- 401 `AUTH_REQUIRED` marks the session as signed out and starts the auth-gate flow when it came from a user action.
- Hand-written TypeScript types per feature mirror the backend DTOs.

### 17.4 Auth gate with intent preservation
1. An anonymous user triggers a gated action (Add to cart, Favorite, Checkout).
2. `requireAuth(action)` stores `{type, offerId, quantity, returnTo, createdAt}` in `sessionStorage` (`buta.pendingAction`, non-secret) and navigates to `/auth?returnTo=<encoded path>`.
3. After OTP verification a new user goes through `/auth/complete-profile`; the pending action is kept.
4. `resumePendingAction()` runs exactly once: checks age (≤ 30 minutes) and shape, calls the API (the server re-validates price, availability and visibility), clears the entry, navigates to the validated `returnTo` (relative paths only, otherwise `/`) and shows a toast — success, or the localized reason (e.g. `OFFER_UNAVAILABLE`) with a link back to the offer.
5. Supported actions: `ADD_TO_CART`, `ADD_FAVORITE`, `START_CHECKOUT`.

### 17.5 Design system
**Principles:** content first; glass only on chrome (navigation, tab bar, sheets, sticky bars), never behind long text; depth through layered surfaces and soft shadows; one accent color used with intent; generous but information-dense layouts; motion that explains state changes.

**Tokens** (CSS custom properties, light and dark sets under `[data-theme]`):
- Color: `--color-bg`, `--color-bg-elevated`, `--color-surface-glass`, `--color-surface-glass-strong`, `--color-border-subtle`, `--color-text`, `--color-text-secondary`, `--color-text-tertiary`, `--color-accent`, `--color-accent-contrast`, `--color-accent-2`, `--color-success`, `--color-warning`, `--color-danger`, `--color-focus-ring`.
- Starting palette (verify contrast in implementation): light background `#F4F5F7`, text `#0E1116`; dark background `#0B0D12`, elevated `#12151C`, text `#F2F4F7`; accent "pomegranate" `#B4233C` on light (≈ 6.5:1 against white) and `#FF5C74` on dark (≈ 6.5:1 against `#0B0D12`); secondary accent "saffron" `#E8A33D` for badges with dark text.
- Glass: blur 12 px / 20 px, saturate 160%, always with `-webkit-backdrop-filter`; solid fallback under `@supports not (backdrop-filter: blur(1px))` and `prefers-reduced-transparency`.
- Radius 8 / 12 / 16 / 24 / pill. Spacing 4-pt scale: 4, 8, 12, 16, 20, 24, 32, 40, 48, 64.
- Typography: fluid `clamp()` scale — caption 12–13, body 15–17, title 20–24, headline 28–40, display 40–56 (desktop only); inputs ≥ 16 px. One self-hosted variable font that contains Ə ə and ₼ (verified before adoption), system UI stack as fallback. No Apple font files.
- Elevation: three soft layered shadow levels. Motion: 120 / 200 / 320 ms, `cubic-bezier(0.2, 0.8, 0.2, 1)`; spring easing only for small confirmations.

**Components:** Button (primary, secondary, ghost, destructive; loading state), IconButton (lucide icons, no emoji), Chip and FilterChip, OfferCard, BundleCard, BundleComparison, PriceTag, SavingsBadge, LowStockNote, Sheet (bottom sheet on mobile, side panel on desktop, native `<dialog>`), Dialog, Tabs, SegmentedControl, Field (input, select, textarea with label, hint, error), OtpInput (`inputmode="numeric"`, `autocomplete="one-time-code"`, paste support), Toast (aria-live), Skeleton, EmptyState, ErrorState, Pagination / load more, ProgressRing, TierProgress, LanguageSwitcher, ThemeSwitcher, TabBar, StickyActionBar, DemoBanner.

### 17.6 Key pages
- **Home** (no oversized empty hero): compact hero with headline, search field and category chips, 3D accent beside it on desktop and small on mobile, content visible above the fold; then "For you" (Plus with consent) or "Popular now"; "Bundles that save the most"; "This week" events; "Ending soon" (real validity); categories; honest Plus explainer for FREE users; demo partners strip.
- **Explore:** search bar, filter sheet with applied-filter chips, sort selector, result count, grid or list, skeleton loading, empty state with suggestions.
- **Offer detail:** media, price block (selling price, reference value, savings), bundle comparison and components, validity and event info, availability, terms, redemption instructions, refund policy, points preview for Plus, sticky Add-to-cart bar on mobile.
- **Cart:** server-priced lines with price-change and unavailable flags, quantity steppers, subtotal, sticky checkout bar.
- **Checkout:** review → points (Plus) → payment (sandbox or Stripe Checkout) → confirmation with voucher codes and instructions.
- **Loyalty:** available points, earned, redeemed, qualifying spend (clearly separated), tier progress, tasks with reward and terms, history, referral card.
- **Account:** profile, avatar, email change, phone, language, theme, privacy controls, favorites, orders, logout and log out everywhere.
- **Admin:** desktop-first and tablet-usable; tables with filters; forms with validation; offer editor with a live margin panel (debounced server preview) and a publish button disabled with reasons when rules fail.

### 17.7 Responsive design and iPhone/Safari checklist
- `<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">`.
- Safe-area insets on header, tab bar, sticky bars and sheets: `padding-bottom: max(16px, env(safe-area-inset-bottom))`.
- Dynamic viewport units: `min-height: 100dvh` (with `100vh` fallback); sheets `max-height: 85dvh`.
- Touch targets ≥ 44 × 44 px with ≥ 8 px spacing; no hover-only interactions (hover styles only under `@media (hover: hover)`).
- Inputs ≥ 16 px font size (prevents iOS zoom); correct `inputmode`, `autocomplete` (`email`, `one-time-code`, `given-name`, `family-name`, `username`, `tel`) and `enterkeyhint`.
- Keyboard-safe layouts: no fixed elements covering focused inputs; forms scroll the focused field into view.
- Phones (≤ 599 px): two-column offer grid with compact cards, compact home rails (about two cards visible), tighter page and section spacing, so several offers fit in one viewport (priority device: iPhone 15, 393 × 852).
- `overscroll-behavior: contain` in sheets and dialogs; background scroll locked while a modal is open.
- Sticky header with matching `scroll-padding-top`.
- No horizontal overflow at 320 px; long Azerbaijani words wrap (`overflow-wrap: anywhere` on titles, `hyphens: auto` with `<html lang>`).
- Breakpoints: base 320, 600, 900, 1200. Raster images with `srcset`/`sizes`, explicit width and height, lazy loading.
- Manual device matrix (documented in README): iPhone SE 375×667, iPhone 15/16 393×852, iPhone Pro Max 430×932, Pixel-class Android 412×915, iPad 820×1180, desktop 1280 and 1440. Automated checks run in Chromium emulation; real Safari checks are manual.

### 17.8 Three.js hero
- Home page only; one canvas; loaded via dynamic `import()` after first paint and idle time, only while the hero is visible (IntersectionObserver).
- Enabled only when WebGL is available, `prefers-reduced-motion` is not `reduce`, and Save-Data is off. Otherwise a static CSS/SVG gradient composition.
- Scene: one abstract, buta-inspired (paisley/teardrop) form with a soft gradient material and slow idle motion; ≤ 5k triangles; no post-processing; device pixel ratio capped at 1.5 (1.0 on phones).
- Pauses when the tab is hidden or the hero is off-screen; disposes geometry, materials and renderer on unmount.
- `pointer-events: none` and `aria-hidden="true"`: it never intercepts touches or reaches assistive technology. Text never sits on the canvas without a scrim that keeps ≥ 4.5:1 contrast.
- Lazy chunk ≤ 200 KB gzip; never part of the initial bundle.

### 17.9 Accessibility
Semantic landmarks and a skip link; visible `:focus-visible` rings; focus moved into and restored from dialogs and sheets; `aria-live` for toasts and cart updates; errors linked with `aria-describedby`; color never the only signal (savings badge includes text); reduced motion and reduced transparency respected; `lang` attribute follows the locale.

### 17.10 Performance budget
Initial-route JS ≤ 200 KB gzip (excluding lazy 3D and admin chunks); route-level code splitting; one variable font with latin and latin-ext subsets, preloaded; images lazy with dimensions; catalog responses cached by TanStack Query; build output sizes checked in Phase 8.

---

## 18. Seed and demo data

**Reference data (every environment):** the 9 categories, FREE and PLUS plans with default feature flags, BRONZE/SILVER/GOLD tiers, default business settings.

**Demo data (Liquibase context `demo` only):**
- 22 partners with the `demo` flag. Originally 10 fictional partners; migration `905` turned nine of them into real Azerbaijani cultural organisations in the same categories (CineMastercard, Azerbaijan State Academic National Drama Theatre, Tusi-Bohm Planetarium, YARAT Contemporary Art Space, Ali and Nino, Icherisheher (Old City), Baku Jazz Festival, BakuBus, CafeCity) and added 12 more without offers (Park Cinema, Nizami Cinema Center, the Opera and Ballet Theatre, the State Philharmonic Hall, the International Mugham Center, the State Puppet Theatre, the Heydar Aliyev Center, the National Carpet Museum, the Baku Museum of Modern Art, the National Art Museum, the Gabala International Music Festival, Libraff). "Melodia Stream" stays fictional. Descriptions contain verified facts only; logos are neutral category images.
- About 30 partner products: event tickets with dates 3–45 days ahead, museum/exhibition admissions, book coupons valid 60–90 days, transport discount passes, digital subscription vouchers, workshop seats, café vouchers (services). Mixed WHOLESALE and COMMISSION cost models.
- About 22 SINGLE offers and 8 BUNDLE offers, including:
  - "Cinema + exhibition evening" — the 20 + 15 → 28 AZN example (§8.3);
  - "Movie + coffee" (cinema ticket + café voucher);
  - "Theatre + museum weekend";
  - "Book coupon + workshop";
  - "Transit pass + museum day";
  - "Festival + 1-month streaming voucher".
  A test asserts every demo offer passes the margin rules.
- Inventory from 5 to 200 units; a few products at ≤ 5 units to show truthful low-stock notes; one sold-out offer.
- Campaign "Autumn culture week (demo)" for Plus with bonus points; one Plus-only offer.
- Tasks: "Culture explorer" (3 partners in a month, 60 pts), "Bundle up" (1 bundle, one-time, 25 pts), "Night out" (1 event attended per month, 20 pts), "Something new" (new category, one-time, 15 pts), "Invite a friend" (1 qualified referral, one-time, 50 pts).
- Synthetic users on the reserved `.test` domain (sign in locally through Mailpit):

| Email | Profile | Purpose |
|---|---|---|
| `aysel.free@buta.test` | FREE, one past order (28.00 AZN qualifying spend) | Shows "72.00 ₼ more qualifying spend unlocks Plus" |
| `kamran.plus@buta.test` | PLUS (earned), cinema-heavy history, 85 points, BRONZE | Shows "15 points to Silver" and a relevant cinema offer earning ≥ 15 points |
| `leyla.plus@buta.test` | PLUS (admin-assigned), theatre and exhibition history, GOLD | Shows cultural bundle recommendations |
| `admin@buta.test` | ADMIN | Admin console |

- Demo orders, redemptions and ledgers are internally consistent (ledger sums equal account balances; a test checks this).
- Images: locally generated abstract SVG artwork per category under `frontend/public/images/demo/`; no hotlinked or real venue photos.
- `DemoCatalogRefresher` keeps demo dates in the future so the demo stays reliable.

---

## 19. Testing strategy

**Owner decision:** the project ships **without automated tests** (no backend unit or integration tests, no frontend tests); the Gradle `test` task is disabled. Verification is done by building (`cd backend && ./gradlew build`, `cd frontend && npm run build`) and by manual smoke checks of the running stack (`docker compose up --build`).

---

## 20. Implementation phases

> Test items in the phase checklists below are superseded by §19: the project ships without automated tests.

Vertical slices: each phase delivers backend, frontend, migrations, admin APIs and tests for its feature, so integration problems surface early. The admin **UI** is built in Phase 7; admin **APIs** are built in the phase that owns the feature.

**Routine for every phase:** Liquibase changesets → entities, repositories, services, controllers, DTOs → frontend pages and components with az + en keys → run `./gradlew build` and `npm run build` → manual smoke test through the Vite proxy → commit `Phase N: <summary>` and push.

### Phase 0 — Foundations
- [ ] Monorepo skeleton: `/backend` (Spring Boot 3.5.16, Java 21, Gradle Wrapper), `/frontend` (Vite + React + TS), root `.gitignore`, `.editorconfig`, `.env.example`, `docker-compose.yml` (postgres:16-alpine, axllent/mailpit), README stub
- [ ] Backend: `@ConfigurationProperties` records, `Clock` bean, Problem Details handler, request-id filter, `MessageSource` az/en, Liquibase master + `001`–`003`, springdoc, security skeleton (public catalog GETs, CSRF, Origin check, headers)
- [ ] Frontend: tokens and global styles, AppShell (glass header, mobile tab bar), router, i18n (az/en), theme provider + `theme-init.js`, API client, error boundary, not-found page, demo banner
- [ ] Testcontainers base class; Vitest setup; ESLint/Prettier
- **Done when:** backend context-load IT passes on PostgreSQL; frontend lint, typecheck, tests and build pass; `docker compose up db mailpit` works.

### Phase 1 — Catalog and pricing
- [ ] `004`, `005`, `901`; entities and repositories for categories, partners, products, inventory, offers, components, campaigns
- [ ] `PriceCalculator`, `MarginPolicy`, `BusinessSettingsService`; search-key folding
- [ ] Public APIs: config, home, categories, partners, offers (search/filter/sort/paginate), offer detail; admin APIs for catalog, margin preview, publish/pause/archive, inventory adjustments, settings with impact preview
- [ ] Frontend: Home (without 3D), Explore with filter sheet, Offer detail, Bundles with comparison, Category, Events, Partners, info pages; skeleton, empty and error states
- [ ] Tests: pricing and margin units, worked example, search folding, anonymous catalog access IT, demo-offer sellability IT, frontend browsing tests
- **Done when:** a visitor browses the seeded catalog end to end in both languages.

### Phase 2 — Authentication, sessions, favorites
- [ ] `002` identity tables in use, `902`; OTP service, `EmailSender` (SMTP + in-memory), localized email templates, rate limiter, session service and filter, profile completion, username availability, logout and logout-all, admin bootstrap, dev mailbox with startup guard, auth audit events
- [ ] Favorites API (first gated action)
- [ ] Frontend: AuthPage (email → OTP with resend countdown), CompleteProfilePage, AuthProvider, RequireAuth, pending-action store and resume, safe `returnTo`, logout
- [ ] Tests: every auth row in §19.1; frontend auth gate and resume tests
- **Done when:** registration and login work locally via Mailpit and an anonymous Favorite resumes after login.

### Phase 3 — Cart, checkout, orders, sandbox payments, vouchers, refunds
- [ ] `006`, `903`; cart service; checkout quote and placement with idempotency; atomic reservations; `PaymentProvider` + sandbox; confirmation; expiry job; cancel; refund; redemption codes; admin redeem and refund APIs
- [ ] Frontend: cart, checkout, sandbox payment page, confirmation with vouchers, orders list and detail, cancel and refund actions; Add to cart resume through the auth gate
- [ ] Tests: concurrency, state machines, idempotency, server-side pricing, expiry, refund restock, IDOR
- **Done when:** the full purchase flow works with honest sandbox labeling and no overselling under concurrency.

### Phase 4 — Membership and loyalty
- [ ] `007`, `008`, `904`; membership service (earned and admin Plus, features); ledger and accounts; earning at the award trigger; redemption at checkout; reversals; tiers; task evaluators; referrals with anti-abuse; nightly membership job; admin APIs (tasks, tiers, plans, Plus assignment, adjustments, referral review)
- [ ] Frontend: loyalty dashboard, tasks with terms, ledger history, referral card, Plus explainer with qualifying-spend progress, points preview on offers, points redemption at checkout
- [ ] Tests: every loyalty, tier, task, referral and membership row in §19.1
- **Done when:** demo users show correct, reconciled balances and tasks complete and reverse correctly.

### Phase 5 — Personalization and recommendations
- [ ] `009`; interactions API and server-side events; interest profile builder; scorer, re-ranker, reasons; impression logging and attribution; metrics endpoint; privacy toggle, preferences and reset; retention job
- [ ] Frontend: "For you" section and page with reasons, opt-in prompt, preference editor, privacy page, impression tracking hook
- [ ] Tests: recommendation rows in §19.1
- **Done when:** Kamran sees cinema-focused results with reasons, Leyla sees cultural bundles, a new Plus user gets the fallback, and FREE users get non-personalized lists.

### Phase 6 — Profile and settings
- [ ] Avatar upload through `FileStorage` (database + local), profile edit, email change with OTP, phone, preferences synced (language, theme), favorites page, log out everywhere
- [ ] Tests: upload validation, email-change flow, profile validation; frontend settings and theme tests
- **Done when:** every PRF requirement works in both languages and both themes.

### Phase 7 — Admin console
- [ ] Admin UI for every admin API: partners, categories, products and inventory, offers and bundles with live margin panel, campaigns, tasks, tiers, plans, settings and weights, users, orders and refunds, redemptions, referral queue, audit log, partner performance, recommendation metrics
- [ ] Tests: authorization matrix for every admin route; audit entries written for every mutation
- **Done when:** an admin can create, price-check and publish a new bundle without touching the database.

### Phase 8 — Experience polish
- [ ] Three.js hero with fallbacks (§17.8); motion system; glass refinement; empty and error states reviewed
- [ ] Accessibility pass (§17.9); responsive Playwright checks (§19.3); performance budget check (§17.10)
- **Done when:** no horizontal overflow at any tested viewport, budgets met, reduced-motion and no-WebGL fallbacks verified.

### Phase 9 — Delivery
- [ ] Production Dockerfiles, Nginx template, full-stack `docker-compose.yml`, Render deployment guide, complete `.env.example`
- [ ] README: architecture, local setup, demo workflow and accounts, environment variables, migrations, tests, deployment, limitations, decisions, manual device checklist
- [ ] SECURITY.md: threat model and production checklist (§14)
- [ ] Optional: GitHub Actions CI (backend verify, frontend lint/typecheck/test/build)
- [ ] Final full test and build run, results reported exactly
- **Done when:** `docker compose up --build` serves the complete app and every deliverable in the brief is checked off.

---

## 21. Deployment (Docker, Compose, Render)

### 21.1 Backend Dockerfile (multi-stage)
- Build stage: `eclipse-temurin:21-jdk-alpine` with the Gradle Wrapper; dependency layer cached (`./gradlew dependencies`), then `./gradlew bootJar`. Tests run locally and in CI, not inside the image build.
- Runtime stage: `eclipse-temurin:21-jre-alpine`, non-root user, `EXPOSE 8080`, `ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]`.
- Spring settings: `server.port=${PORT:8080}`, `server.shutdown=graceful`, `server.forward-headers-strategy=native` with trusted internal proxies.

### 21.2 Frontend Dockerfile (multi-stage)
- Build stage: `node:22-alpine`, `npm ci && npm run build`.
- Runtime stage: `nginxinc/nginx-unprivileged:1.29-alpine` (non-root, listens on 8080) with `nginx/default.conf.template`; the image's entrypoint substitutes `${PORT}` and `${API_UPSTREAM}` at start.
- Nginx essentials: `listen ${PORT}`; `location / { try_files $uri $uri/ /index.html; }`; long-lived immutable caching for `/assets/`; `Cache-Control: no-cache` for `index.html`; `location /api/ { proxy_pass ${API_UPSTREAM}; }` forwarding `X-Forwarded-For`, `X-Forwarded-Proto` and `X-Request-Id`, with `proxy_ssl_server_name on` for HTTPS upstreams and runtime DNS resolution (resolver + variable) so Nginx starts even if the backend is not up yet; `client_max_body_size 3m`; gzip; security headers (§14.2).

### 21.3 Docker Compose (local)
| Service | Image / build | Ports | Notes |
|---|---|---|---|
| `db` | `postgres:16-alpine` | 5432 | named volume, `pg_isready` healthcheck |
| `mailpit` | `axllent/mailpit` | 1025 (SMTP), 8025 (UI) | catches every OTP email locally |
| `backend` | `./backend` | 8080 | profile `dev`, contexts `default,demo`, mail → `mailpit:1025`, waits for healthy `db` |
| `frontend` | `./frontend` | 8081 → 8080 | `API_UPSTREAM=http://backend:8080` |

Workflows:
- Full stack: `cp .env.example .env && docker compose up --build` → app http://localhost:8081, Mailpit http://localhost:8025, Swagger http://localhost:8080/api/v1/docs.
- Hot reload: `docker compose up -d db mailpit`, `cd backend && ./gradlew bootRun`, `cd frontend && npm run dev` → http://localhost:5173.
- Demo sign-in: enter `kamran.plus@buta.test`, open Mailpit, copy the code.
- Reset the database: `docker compose down -v`.

### 21.4 Render
**Selected topology:** Render PostgreSQL `buta-db`; backend `buta-backend` as a Docker web service; frontend `buta-frontend` as a Docker web service (Nginx). The frontend is a Docker service rather than a static site so that Nginx can proxy `/api` on the same origin, keeping cookies first-party on Safari and removing CORS from production.

**Deployment steps:** see [`docs/DEPLOY_RENDER.md`](DEPLOY_RENDER.md) — database, backend and frontend are created as three separate Render resources in one region; `API_UPSTREAM` on the frontend points to the backend's public URL (any plan) or its private-network address (paid backend); email uses Gmail with an app password.

---

## 22. Decisions to confirm

The build proceeds with the default unless told otherwise.

| # | Question | Default |
|---|---|---|
| Q1 | Brand name and Java base package | "Buta", `az.buta` |
| Q2 | Session model | Opaque server-side session cookie (D5); alternative: JWT access + rotating refresh cookie |
| Q3 | Default language | Azerbaijani (`az`), English secondary |
| Q4 | Who earns points | Plus members only (as in the brief); FREE members earn nothing |
| Q5 | How Plus is earned | 100.00 AZN qualifying spend within 180 days, valid 365 days; plus admin assignment |
| Q6 | Points award trigger | `FULFILLED` (payment captured, vouchers issued); alternative `COMPLETED` (after redemption) |
| Q7 | Refund after points were spent | Balance may go negative and redemption is blocked; alternative: refuse such refunds |
| Q8 | Personalization consent | Opt-in prompt (off until the member enables it) |
| Q9 | Points economics | 1 point per AZN, 100 points = 1.00 AZN, max 20% of an order |
| Q10 | Margin rule and fee placeholders | ≥ 1.00 AZN and ≥ 5%; payment fee 2.50% + 0.20 AZN until a real acquirer contract exists |
| Q11 | Refund policy defaults | Unredeemed units only, until 24 h before the event or validity end; per-product overrides |
| Q12 | Avatar storage | Database (works on Render without extra services); alternative: S3-compatible storage |
| Q13 | Lombok | Not used |
| Q14 | Render plan and custom domain | Unknown — affects private networking, cold starts, HSTS and cookie settings |
| Q15 | Production SMTP provider | Unknown — any SMTP provider works through configuration |
| Q16 | Admin UI depth | Minimal UI for every admin API |

---

## 23. Known limitations and production prerequisites

**MVP limitations**
- Payments are a sandbox by default; Stripe Checkout runs with test keys.
- No partner portal or partner API integrations; inventory is maintained by admins.
- IP-based rate limiting is in memory (single backend instance).
- No self-service data export or account deletion; no SMS verification; no seat selection; no points expiry.
- Rule-based recommendations only; A/B plumbing ships with one variant.
- Demo content is illustrative; sign-in on Render needs a real SMTP provider.
- Real Safari/iOS behaviour is verified manually (no WebKit in the build container).

**Before a real launch**
- Live Stripe account and keys (hosted payment page, signed webhooks and refunds are already integrated).
- Signed partner agreements (including permission to use partner names and logos); real partner data; settlement and reconciliation process.
- Legal review: terms, privacy policy, refund policy, consumer protection, personal-data compliance, tax and invoicing.
- SMTP with SPF/DKIM/DMARC; custom domain; HSTS.
- Distributed rate limiting (Redis or database) before scaling out; monitoring and alerting; backups with tested restores.
- Data export and deletion flows; data retention jobs verified.
- Security review / penetration test; accessibility audit; load test of checkout concurrency.

---

## 24. Requirements traceability

| Brief section | Covered in | Phase |
|---|---|---|
| Product and business model | §4, §8.3 | 1 |
| Tech stack and monorepo | §3, §10, §21 | 0, 9 |
| Architecture | §10–§13 | all |
| Public marketplace | §8.1, §17 | 1, 2 |
| Smart bundles and pricing | §8.2–§8.4 | 1, 3 |
| Free and Plus membership | §8.7 | 4 |
| Loyalty engine | §8.8 | 4 |
| Personalization and recommendations | §8.9, §15 | 5 |
| Email OTP authentication | §8.5, §14 | 2 |
| Profile and settings | §8.6 | 6 |
| Internationalization | §8.11 | all |
| Design direction | §8.12, §17.5–§17.6, §17.8 | 1, 8 |
| Responsive design | §17.7 | 8 |
| Security and privacy | §14, §15 | all, 9 |
| Admin and partner operations | §8.10 | each phase (APIs), 7 (UI) |
| API and data model | §11–§13 | all |
| Testing and quality | §19 | all |
| Deliverables 1–10 | §20 (Phase 9), §21 | 9 |

---

**Build kickoff instruction (for the next session):** "Implement `docs/PROJECT_PLAN.md` phase by phase, starting with Phase 0. Use Liquibase for all migrations. After each phase run the backend and frontend checks from §19.4, fix failures, commit and push, and report the exact results."
