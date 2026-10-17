# Deploy Buta to Render

You will create **three separate resources** in Render, all in the **same region**:

| # | Render resource | Name used in this guide | What it runs |
|---|---|---|---|
| 1 | PostgreSQL | `buta-db` | The database |
| 2 | Web Service (Docker) | `buta-api` | Spring Boot backend (`backend/Dockerfile`) |
| 3 | Web Service (Docker) | `buta-web` | React app served by Nginx (`frontend/Dockerfile`), which forwards `/api/*` to the backend |

Visitors only open `buta-web`. Because the browser talks to one address, sign-in cookies work in every browser, including Safari, and no CORS setup is needed.

Total time: about 20 minutes.

---

## Before you start

### A. Gmail app password (for sign-in codes)

Buta sends its 6-digit sign-in codes through your Gmail account.

1. Open your Google Account → **Security** and make sure **2-Step Verification** is **on** (app passwords only exist when it is on).
2. Open <https://myaccount.google.com/apppasswords>, enter the name `Buta` and click **Create**.
3. Copy the 16-character password and **remove the spaces** (`abcd efgh ijkl mnop` → `abcdefghijklmnop`).

Keep two values ready:

- `MAIL_USERNAME` = your Gmail address, e.g. `yourname@gmail.com`
- `MAIL_APP_PASSWORD` = the 16 characters without spaces

Emails are sent from your Gmail address with the sender name "Buta". A personal Gmail account can send roughly 500 emails per day.

### B. Two random secrets

Generate two long random strings (at least 32 characters each). With a terminal:

```bash
openssl rand -base64 48
openssl rand -base64 48
```

Any password manager's generator (48+ characters) works too. You will use them as `APP_OTP_PEPPER` and `APP_IP_HASH_SECRET`.

### C. Code on GitHub

The branch you deploy (e.g. `main` after merging `migration`) must be pushed to GitHub, and your GitHub account must be connected to Render.

---

## Step 1 — Create the database

1. Render Dashboard → **New +** → **Postgres**.
2. Fill in:
   - **Name:** `buta-db`
   - **Database:** `buta`
   - **User:** `buta`
   - **Region:** pick one (e.g. Frankfurt). **Use the same region for all three services.**
   - **PostgreSQL Version:** 16
   - **Plan:** your choice (see [Plans](#plans-and-limits)).
3. Click **Create Database** and wait until the status is **Available**.
4. Open the database page and find the **Connections** section. Note these values:
   - **Hostname** (the *internal* one, e.g. `dpg-xxxxxxxxxxxx-a`)
   - **Port** (`5432`)
   - **Database** (`buta`)
   - **Username** (`buta`)
   - **Password**

You don't need to create any tables; the backend creates the schema on first start (Liquibase).

---

## Step 2 — Create the backend (`buta-api`)

1. **New +** → **Web Service** → choose your GitHub repository.
2. Fill in:
   - **Name:** `buta-api`
   - **Branch:** the branch you deploy (e.g. `main`)
   - **Region:** the same as the database
   - **Language / Runtime:** **Docker**
   - **Instance Type:** your choice (see [Plans](#plans-and-limits))
3. Under **Advanced**:
   - **Dockerfile Path:** `./backend/Dockerfile`
   - **Docker Build Context Directory:** `./backend`
   - **Health Check Path:** leave empty
4. Under **Environment Variables**, add:

| Key | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_HOST` | database **Hostname** (internal) from Step 1 |
| `DB_PORT` | `5432` |
| `DB_NAME` | `buta` |
| `DB_USER` | `buta` |
| `DB_PASSWORD` | database **Password** from Step 1 |
| `APP_PUBLIC_BASE_URL` | `https://buta-web.onrender.com` (your frontend address; you'll confirm it in Step 4) |
| `APP_ADMIN_EMAILS` | your email address, e.g. `yourname@gmail.com` (comma-separate several) |
| `APP_OTP_PEPPER` | random secret #1 |
| `APP_IP_HASH_SECRET` | random secret #2 |
| `MAIL_USERNAME` | your Gmail address |
| `MAIL_APP_PASSWORD` | your 16-character app password, without spaces |
| `APP_DEMO_MODE` | `true` |
| `APP_LIQUIBASE_CONTEXTS` | `default,demo` to load the demo catalog, or `default` for an empty catalog |

5. Click **Create Web Service**. The first build takes a few minutes (Gradle downloads its dependencies).
6. When the deploy succeeds (the log shows `Started ButaApplication`), copy the service address shown at the top, e.g. `https://buta-api.onrender.com`. Check it works by opening `https://buta-api.onrender.com/api/v1/config`; it should show a small JSON with `"demoMode":true`.

Notes:

- `APP_DEMO_MODE=true` is required for now: payments are a sandbox (no real money), and the app labels itself as a demo. The backend refuses to start in `prod` without it.
- Leave `PORT` unset; Render provides it and the app picks it up.

---

## Step 3 — Create the frontend (`buta-web`)

1. **New +** → **Web Service** → the same GitHub repository.
2. Fill in:
   - **Name:** `buta-web`
   - **Branch:** the same branch
   - **Region:** the same region
   - **Language / Runtime:** **Docker**
   - **Instance Type:** your choice
3. Under **Advanced**:
   - **Dockerfile Path:** `./frontend/Dockerfile`
   - **Docker Build Context Directory:** `./frontend`
   - **Health Check Path:** leave empty
4. Under **Environment Variables**, add one variable:

| Key | Value |
|---|---|
| `API_UPSTREAM` | the backend address from Step 2, e.g. `https://buta-api.onrender.com` |

5. Click **Create Web Service** and wait for the deploy to finish.

---

## Step 4 — Connect the two services

1. Copy the frontend address shown on the `buta-web` page (e.g. `https://buta-web.onrender.com`).
2. If it differs from what you entered for `APP_PUBLIC_BASE_URL` in Step 2 (Render adds a suffix when a name is taken), open `buta-api` → **Environment**, set `APP_PUBLIC_BASE_URL` to the exact frontend address (no trailing slash) and click **Save Changes**. Render redeploys the backend.

---

## Step 5 — Check that everything works

1. Open the frontend address. You should see the home page with the demo banner.
2. Click **Sign in**, enter the email you put in `APP_ADMIN_EMAILS` and press **Continue**.
3. The 6-digit code arrives in that inbox within a minute (check **Spam** the first time and mark it "Not spam").
4. After signing in, **Admin** appears in the top navigation (on a phone: **Account → Admin console**).

Done. Every push to the deployed branch redeploys the changed services automatically.

---

## Plans and limits

The setup above works on any plan. Things to know:

- **Free web services sleep** after about 15 minutes without traffic. The next visit wakes them, which takes up to a minute (both `buta-web` and `buta-api` may need to wake).
- **Free Render Postgres** databases are temporary (they expire after a limited time; check Render's current terms). Use a paid database plan for data you want to keep.
- **Sign-in rate limits:** the backend limits sign-in codes per visitor IP (20 per hour) and verifications per IP (30 per 10 minutes). With `API_UPSTREAM` set to the public backend address, all visitors reach the backend through the frontend, so they share one IP. For a public launch on this setup, raise the limits on `buta-api`, e.g. `APP_OTP_MAX_PER_IP_HOUR=300` and `APP_OTP_MAX_VERIFY_PER_IP=500`. The per-email limit (5 codes per hour per address) still protects each account.

### Optional: private network (paid backend)

If `buta-api` is on a **paid** instance type, the frontend can reach it over Render's private network. That is faster, keeps real visitor IPs and makes the IP raise above unnecessary:

1. Open `buta-api` → **Connect** → **Internal** and copy the internal address (it looks like `buta-api-xxxx:10000`).
2. Set `API_UPSTREAM` on `buta-web` to that address (e.g. `http://buta-api-xxxx:10000`) and save.

Free web services cannot *receive* private-network traffic, which is why the public address is the default above.

---

## Custom domain (optional)

1. `buta-web` → **Settings** → **Custom Domains** → add e.g. `buta.az` and follow the DNS instructions (Render issues the TLS certificate).
2. Set `APP_PUBLIC_BASE_URL` on `buta-api` to `https://buta.az` and save.

---

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `buta-api` deploy fails and the log shows `Unsafe configuration: …` | A required variable is missing or too short. The message names it (e.g. `MAIL_USERNAME and MAIL_APP_PASSWORD are required in production`, `APP_OTP_PEPPER must be at least 32 bytes`). Fix it under **Environment** and save. |
| `buta-api` log shows a database connection error | `DB_HOST` must be the **internal** hostname, and the database must be in the same region. Double-check `DB_PASSWORD`. |
| Sign-in code never arrives | Check Spam. In the `buta-api` logs, `Email delivery failed … AuthenticationFailedException` means a wrong Gmail address or app password (remove spaces; 2-Step Verification must be on; Google Workspace admins can disable app passwords). |
| "The request came from an origin that is not allowed" when pressing Continue | `APP_PUBLIC_BASE_URL` on `buta-api` does not exactly match the address in the browser bar. |
| The site loads but every action fails, or `/api/...` returns 502 | `API_UPSTREAM` on `buta-web` is wrong, or the backend is still waking up (free plan); wait a minute and retry. |
| The first visit takes a long time | Free services are waking up from sleep (see [Plans and limits](#plans-and-limits)). |

---

## Reference: all environment variables

**`buta-api` (backend)**

| Key | Required | Default | Purpose |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | yes | — | Must be `prod` on Render |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | yes | — | Database connection |
| `APP_PUBLIC_BASE_URL` | yes | — | Exact frontend address |
| `APP_ADMIN_EMAILS` | yes | — | Who becomes an admin on sign-in |
| `APP_OTP_PEPPER`, `APP_IP_HASH_SECRET` | yes | — | Random secrets, at least 32 characters |
| `MAIL_USERNAME`, `MAIL_APP_PASSWORD` | yes | — | Gmail address and app password |
| `APP_DEMO_MODE` | yes | `false` | Must be `true` while payments are a sandbox |
| `APP_LIQUIBASE_CONTEXTS` | no | `default` | `default,demo` loads demo content |
| `APP_OTP_MAX_PER_IP_HOUR`, `APP_OTP_MAX_VERIFY_PER_IP` | no | `20`, `30` | Per-IP sign-in limits |
| `DB_SSL_MODE` | no | `prefer` | PostgreSQL SSL mode |
| `DB_POOL_SIZE` | no | `10` | Database connection pool size |

**`buta-web` (frontend)**

| Key | Required | Purpose |
|---|---|---|
| `API_UPSTREAM` | yes | Backend address: public `https://…onrender.com` or internal `http://host:port` |
