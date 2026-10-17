# Buta — tickets, bundles and loyalty

Buta is an MVP marketplace that sits between cultural and leisure **partners** (cinemas, theatres, museums, festivals, workshops, transport, book shops) and **customers**. The platform negotiates discounted prices, sells single offers and margin-checked **bundles**, issues voucher codes, and rewards repeat visits with a **loyalty programme** (points, tasks, tiers, referrals) and explainable **recommendations**.

> The repository runs in **demo mode** by default: partners, offers and availability are fictional and payments are a **sandbox** (no money moves, no card data is collected).

The full product and engineering plan lives in [`docs/PROJECT_PLAN.md`](docs/PROJECT_PLAN.md). Security notes are in [`SECURITY.md`](SECURITY.md).

---

## Contents

1. [Repository layout](#repository-layout)
2. [Architecture](#architecture)
3. [Quick start with Docker Compose](#quick-start-with-docker-compose)
4. [Local development](#local-development)
5. [Demo accounts and walkthrough](#demo-accounts-and-walkthrough)
6. [Configuration](#configuration)
7. [Database migrations (Liquibase)](#database-migrations-liquibase)
8. [Building and verification](#building-and-verification)
9. [Deployment on Render](#deployment-on-render)
10. [Business rules in short](#business-rules-in-short)
11. [Decisions and deviations](#decisions-and-deviations)
12. [Known limitations](#known-limitations)

---

## Repository layout

```
/backend             Spring Boot 3.5 (Java 21, Gradle) REST API and Liquibase migrations
/frontend            React 19 + TypeScript + Vite SPA, Nginx runtime image
/docs/PROJECT_PLAN.md  Requirements, architecture and delivery plan
/docker-compose.yml  PostgreSQL, Mailpit, backend and frontend for local use
/docs/DEPLOY_RENDER.md  Step-by-step Render deployment
/.env.example        Every environment variable with safe local defaults
/SECURITY.md         Threat model and production checklist
```

## Architecture

```
Browser ──HTTPS──▶ frontend (Nginx)
                    ├─ serves the SPA (static, immutable assets, SPA fallback)
                    └─ /api/* ──proxy──▶ backend (Spring Boot) ──▶ PostgreSQL
                                              └─▶ Gmail SMTP (Mailpit locally)
```

- **Same origin.** The SPA calls `/api/v1/...` on its own origin; Nginx proxies to the backend. Session and CSRF cookies therefore stay first-party (`SameSite=Lax`, `HttpOnly`, `__Host-` prefix in production) and no CORS is needed.
- **Backend** — modular monolith by business capability: `auth`, `user`, `catalog`, `pricing`, `cart`, `order`, `payment`, `loyalty`, `membership`, `recommendation`, `favorite`, `admin`, `notification`, `security`, `common`. Opaque server-side sessions (SHA-256 hashed tokens), email OTP sign-in, RFC 9457 problem responses with localized messages (`az` default, `en`), Hibernate in `validate` mode, Liquibase-owned schema.
- **Frontend** — React Router 7 (lazy routes), TanStack Query 5, i18next (`az`/`en`), a small global CSS design system (tokens, light/dark themes, iOS-inspired glass surfaces, safe-area aware layout), lucide icons, and a restrained Three.js hero that is lazy-loaded, paused off-screen and static under `prefers-reduced-motion`.

## Quick start with Docker Compose

Requirements: Docker with Compose v2.

```bash
docker compose up --build
```

| URL | What |
|---|---|
| http://localhost:8081 | The web app (frontend + proxied API) |
| http://localhost:8025 | Mailpit inbox — sign-in codes arrive here |
| http://localhost:8080/api/v1/docs | Swagger UI (dev profile) |

Compose runs the backend with the `dev` profile, the `default,demo` Liquibase contexts (demo catalog, users, orders and loyalty data) and local-only secrets. Stop with `docker compose down` (add `-v` to wipe the database).

## Local development

Start the infrastructure only:

```bash
docker compose up -d db mailpit
```

Backend (Java 21):

```bash
cd backend
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

Frontend (Node 22):

```bash
cd frontend
npm ci
npm run dev          # http://localhost:5173, proxies /api to http://localhost:8080
npm run build        # type-check + production build into dist/
```

## Demo accounts and walkthrough

Sign-in is passwordless: enter an email, then the 6-digit code from Mailpit.

| Email | Role / state |
|---|---|
| `admin@buta.test` | Administrator (bootstrap admin via `APP_ADMIN_EMAILS`) |
| `aysel.free@buta.test` | Free member, 28.00 ₼ qualifying spend (72.00 ₼ to Plus) |
| `kamran.plus@buta.test` | Plus (earned by spend), 85 points, Bronze — 15 points to Silver |
| `leyla.plus@buta.test` | Plus (assigned by admin), 558 points, Gold |

Any other email creates a new account (you'll be asked for name and username).

Owner accounts `jahangir.alisoy@gmail.com` and `cahangiralisoy313@gmail.com` are created as admins in every environment by migration `010-owner-accounts.sql`. How to use the admin console is described in [docs/DEPLOY_RENDER.md](docs/DEPLOY_RENDER.md#using-the-admin-console).

Suggested walkthrough:

1. Browse the home page, **Explore**, **Bundles** and **Events** without signing in; open a bundle to see the separate-vs-bundle price comparison and what is included.
2. Press **Add to cart** while signed out — you are sent to sign-in and, after verifying the code, the item is added and you return to the offer.
3. Check out as `kamran.plus@buta.test`, use points, and complete the **sandbox payment** (success or failure). Vouchers appear on the order page.
4. Open **Rewards** to see points, tier progress, tasks and the referral code; open **For you** to see recommendations with reasons (personalization asks for consent first).
5. Sign in as `admin@buta.test` and open **Admin**: create or edit an offer and watch the live **margin preview**, publish/pause offers, adjust inventory, assign Plus, review referrals, refund orders, **redeem a voucher code** (this completes the order and can complete the "night out" task), change business settings with an impact preview, and read the audit log.

## Configuration

All configuration comes from environment variables; [`.env.example`](.env.example) lists every variable with local defaults. The most important ones:

| Variable | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` locally, `prod` when deployed |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_SSL_MODE` | PostgreSQL connection |
| `APP_PUBLIC_BASE_URL` | Public URL of the web app (used for Origin checks and emails) |
| `APP_ALLOWED_ORIGINS` | Extra allowed origins (comma-separated), e.g. the Vite dev server |
| `APP_ADMIN_EMAILS` | Emails that receive the admin role on sign-in |
| `APP_OTP_PEPPER`, `APP_IP_HASH_SECRET` | Secrets (≥ 32 bytes in `prod`; generated by Render) |
| `APP_SESSION_COOKIE_SECURE`, `APP_SESSION_COOKIE_NAME` | Cookie hardening (`true` / `__Host-buta_session` in `prod`) |
| `APP_DEMO_MODE` | Shows the demo banner; required for sandbox payments in `prod` |
| `APP_LIQUIBASE_CONTEXTS` | `default` for real data, `default,demo` to load demo content |
| `MAIL_USERNAME`, `MAIL_APP_PASSWORD` | Gmail address and its app password used to send sign-in codes (the `dev` profile sends to Mailpit instead) |
| `PORT` / `API_UPSTREAM` | Frontend container: listen port and backend address (`https://…` public URL or `http://host:port` internal address) |

Business settings (pricing, loyalty, membership, recommendation weights) live in the database and are edited in **Admin → Business settings**; every change is validated and audited.

## Database migrations (Liquibase)

- Master changelog: `backend/src/main/resources/db/changelog/db.changelog-master.yaml`, with formatted-SQL change sets in `changes/`.
- `001`–`009`: schema and reference data (context `default`).
- `901`–`904`: demo catalog, users, orders and loyalty data (context `demo`).
- Migrations run automatically on startup; Hibernate only validates the schema.
- Production sets `APP_LIQUIBASE_CONTEXTS` explicitly (`default` for real launches, `default,demo` for the hosted demo).

## Building and verification

The project has **no automated tests** (backend and frontend), as decided by the project owner; the Gradle `test` task is disabled. Quality gates are the builds:

```bash
cd backend && ./gradlew build      # compiles and packages build/libs/app.jar
cd frontend && npm run build       # strict TypeScript type-check + Vite production build
```

Both builds pass. The Docker images were built and the Compose stack was started; the SPA, deep links, security headers, the `/api` proxy and a full OTP sign-in through Nginx (code received in Mailpit) were checked manually.

## Deployment on Render

Three separate Render resources: a PostgreSQL database, the backend as a Docker web service and the frontend as a Docker web service (Nginx) that forwards `/api` to the backend. Email goes through a Gmail account with an app password.

Follow the step-by-step guide: **[docs/DEPLOY_RENDER.md](docs/DEPLOY_RENDER.md)**.

## Business rules in short

- **Pricing** — selling price is allocated across bundle components (remainder to the last component); partner cost follows each product's model (wholesale net cost, commission, or negotiated cost); expected payment fee is 2.5 % + 0.20 ₼; loyalty cost is estimated at the highest tier multiplier; offers must keep a contribution margin of at least 1.00 ₼ **and** 5 %, and may never cost more than the reference retail price. Example: a 28.00 ₼ bundle leaves a 1.55 ₼ (5.54 %) margin and can be published.
- **Checkout** — idempotent order placement, atomic inventory reservation (15-minute TTL), sandbox payment confirmation, one voucher per unit, refunds with restock and points reversal, scheduled expiry of unpaid orders.
- **Membership** — Free and Plus. Plus is earned automatically at 100 ₼ qualifying spend in 180 days (or assigned by an admin) and unlocks points, tasks, tiers, points redemption, personal recommendations and exclusive offers.
- **Loyalty** — append-only points ledger with idempotency keys; tiers Bronze (0), Silver (100, ×1.10) and Gold (500, ×1.25); tasks for distinct partners, bundles, event attendance, new categories and referrals; referrals from the same network go to manual review.
- **Recommendations** — rule-based scoring (interest 30, budget 20, value 15, loyalty 15, discovery 10, business 10), minimum relevance, frequency caps, diversity re-ranking and exploration slots; every item carries a human-readable reason. Personalization requires explicit consent and can be reset.

## Decisions and deviations

- **Liquibase** instead of Flyway, as requested.
- **Opaque server-side sessions** instead of JWTs: instant revocation ("sign out everywhere", admin revoke) and no tokens readable by JavaScript.
- **Global CSS design system** (tokens + component classes) instead of CSS Modules, and **plain controlled forms** instead of react-hook-form/zod: fewer dependencies for an MVP; the backend remains the source of truth for validation and returns field errors that the forms display.
- **No automated tests** (backend or frontend) and **no health-check endpoints**, as instructed; quality gates are the Gradle build, strict TypeScript and the production build.
- **Three.js** is isolated in a lazily loaded chunk and only starts after the page is interactive; a CSS gradient is shown when WebGL is unavailable or data saving is on.
- **Admin settings forms** render the stored configuration generically, so labels use the configuration keys.

## Known limitations

- Payments are sandbox only; no partner portal or partner integrations (admins maintain inventory).
- IP-based rate limiting is in memory, so the backend should run as a single instance until a distributed limiter is added.
- No self-service data export/account deletion, SMS verification, seat selection or points expiry.
- Recommendations are rule-based; the A/B plumbing ships with a single variant.
- Real Safari/iOS behaviour still needs manual verification on devices.
- Demo content is illustrative; on Render, sign-in requires the Gmail credentials (`MAIL_USERNAME`, `MAIL_APP_PASSWORD`).
