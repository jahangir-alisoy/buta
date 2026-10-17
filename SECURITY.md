# Security

This document summarises how Buta protects accounts, money-like assets (orders, vouchers, loyalty points) and personal data, and what must be done before a real launch. The detailed design is in [`docs/PROJECT_PLAN.md`](docs/PROJECT_PLAN.md) §14–15.

## Reporting a vulnerability

Please do not open a public issue. Email the maintainers with steps to reproduce; we aim to acknowledge reports within 3 working days.

## Threat model

**Assets:** accounts and sessions, OTP codes, orders and voucher codes, loyalty points, partner cost data (commercially sensitive), personal data.

| Threat | Mitigation in this codebase |
|---|---|
| OTP brute force | 6-digit codes, 10-minute TTL, 5 attempts per challenge, per-email lockout, per-email and per-IP request limits, 60 s resend cooldown, HMAC-SHA256 hashes with a server-side pepper, constant-time comparison |
| OTP replay | Single-use atomic consumption; a new request supersedes older challenges |
| Account enumeration | One sign-in/sign-up flow with identical responses and limits for known and unknown emails |
| Session theft via XSS | Opaque session token in an `HttpOnly`, `SameSite=Lax` cookie (`__Host-` + `Secure` in production); nothing sensitive in `localStorage`; strict CSP; React escaping; no `dangerouslySetInnerHTML` |
| CSRF | `SameSite=Lax` + CSRF token (`XSRF-TOKEN` cookie / `X-XSRF-TOKEN` header) + Origin check on unsafe methods |
| Session fixation / stale sessions | New token on every sign-in; tokens stored as SHA-256 hashes; idle and absolute timeouts (shorter for admins); "sign out everywhere" and admin session revocation |
| IDOR | Every user-owned query is scoped by the authenticated user; other users' resources return 404; UUID identifiers |
| Mass assignment / privilege escalation | Explicit request DTOs, unknown JSON fields rejected; roles and membership read from the database; admin routes protected by URL rules and method security |
| Price, discount, points or status tampering | All prices, savings, margins, points and statuses are computed on the server; client values are ignored |
| Overselling and races | Conditional atomic inventory updates with CHECK constraints and deterministic lock order; idempotent checkout (Idempotency-Key + request fingerprint) |
| Points double spend / duplicate rewards | Account row locks, append-only ledger with unique idempotency keys, recompute-based task evaluation, reversals on refund |
| Referral and task abuse | Self-referral rejection, network (IP-hash) checks and monthly caps → manual review queue, minimum order amounts |
| SQL injection | Parameter binding only; sort options are whitelisted enums |
| Malicious uploads | Avatar size/type limits, decoder-based format detection, dimension limits, re-encoding to JPEG, `nosniff`, owner-scoped serving |
| Open redirect | `returnTo` accepted only as a relative path (no `//`, backslash or scheme) |
| Spoofed `X-Forwarded-For` | Forwarded headers trusted only from configured internal proxies (`APP_TRUSTED_PROXIES`); per-email limits do not depend on IP |
| Information leakage | Generic errors with a request id, no stack traces or exception messages; partner costs visible to admins only; OTPs and tokens never logged |
| Secret exposure | Secrets only from environment variables; `.env` is git-ignored; `.env.example` contains local placeholders only |
| Clickjacking | `frame-ancestors 'none'` and `X-Frame-Options: DENY` |
| Development features in production | Startup guards (below) |

## Security headers

- **Nginx (SPA):** `Content-Security-Policy` (`default-src 'self'`, `script-src 'self'`, `frame-ancestors 'none'`, `object-src 'none'`), `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy`, `Cross-Origin-Opener-Policy: same-origin`. `index.html` is never cached; hashed assets are immutable.
- **Spring Security (API):** CSP, `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy`, `Permissions-Policy`, `Cache-Control: no-store`, HSTS on secure requests.

## Startup guards (`prod` profile)

The backend refuses to start when:

- `APP_OTP_PEPPER` or `APP_IP_HASH_SECRET` is missing, shorter than 32 bytes, or still a `dev-only` value;
- the dev mailbox or the in-memory email provider is enabled;
- sandbox payments are active while `APP_DEMO_MODE` is not `true`;
- the session cookie is not `Secure`.

## Privacy

- Personalization is **opt-in**; only purchases, favorites, explicit preferences and viewed/searched offers are used — never sensitive attributes.
- Users can turn personalization off and delete the collected activity at any time (**Account → Privacy**). Interaction events are retained for 180 days.
- IP addresses are stored only as keyed hashes for abuse prevention.

## Production checklist

- [ ] All secrets set in Render (generated randomly), never committed; rotation procedure documented
- [ ] HTTPS only with a custom domain, HSTS enabled, `__Host-` session cookie
- [ ] SMTP provider configured with SPF, DKIM and DMARC for the sender domain
- [ ] Dev mailbox off, API docs off (`APP_API_DOCS_ENABLED=false`), `demo` Liquibase context removed for a real launch
- [ ] `APP_ADMIN_EMAILS` reviewed; admin accounts reviewed regularly
- [ ] Database backups / point-in-time recovery enabled and a restore tested
- [ ] Application database user is not a superuser
- [ ] Dependency scanning (Maven and `npm audit`) and container image scanning in CI
- [ ] Logs verified free of OTPs, tokens, cookies and raw emails; retention configured
- [ ] Rate limits tuned; distributed rate limiting before running more than one backend instance
- [ ] Monitoring and alerting (health, error rate, latency, failed sign-ins, OTP lockouts)
- [ ] Real payment provider via a hosted payment page with signed webhooks (no card data on our servers)
- [ ] Terms, privacy policy, refund policy and partner agreements reviewed by legal counsel
- [ ] Penetration test and accessibility audit before public launch
