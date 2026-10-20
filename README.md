# Supplog

Supplog is a health routine management application for organizing medicines, vitamins, supplements, and recurring intake routines.

The project was inspired by a simple real-life problem: not always remembering whether a medicine or supplement has already been taken.

This repository contains the **Spring Boot backend REST API**. The web frontend (React + TypeScript + Vite) lives in a separate repository: [`memreeger/supplog-frontEnd`](https://github.com/memreeger/supplog-frontEnd).

**Live demo:** <https://supplog.vercel.app/> (frontend on Vercel, backend and PostgreSQL on Railway)

> 🚧 Supplog is under active development. The backend includes routine execution tracking, supporter-based access, rotating refresh tokens and e-mail based password reset; push notifications and supporter nudges are planned next (see [Roadmap](#roadmap)).

---

## Features

### Authentication & Security

* User registration and login
* JWT-based stateless authentication with short-lived access tokens (15 min)
* Rotating, opaque refresh tokens stored only as SHA-256 hashes
* Web: refresh token in an `HttpOnly; Secure; SameSite=Strict` cookie; mobile: refresh token in the JSON body
* Refresh token reuse detection (revokes the whole session family)
* Logout and "sign out of all devices"
* BCrypt password hashing
* `ROLE_USER` and `ROLE_ADMIN` authorization
* Custom JSON `401` and `403` responses with stable error codes
* JWT `tokenVersion` validation: password changes and admin actions invalidate existing sessions
* `Origin` validation for cookie-based auth requests
* Per-IP rate limiting for auth and support endpoints
* Protection against authentication for deactivated users
* Resource ownership validation

### User Management

Authenticated users can:

* View and update their profile
* Change their password
* Change their time zone
* Sign out of all devices
* Deactivate their account
* Manage only their own supplements and routines

### Admin Management

Administrators can:

* Search and manage users
* Activate or deactivate users
* Reset passwords
* Change user roles
* Manage supplements and routines across users
* Correct routine execution statuses
* Revoke support relationships
* View the admin audit log and dashboard

Additional safeguards prevent:

* Admin self-deactivation through admin operations
* Admin self-demotion
* Removal of the final active administrator

Every admin write operation is recorded in the audit log with a reason.

### Password Reset

* `POST /api/v1/auth/forgot-password` always answers `202`, so it never reveals whether an e-mail is registered
* The reset link (`FRONTEND_BASE_URL/reset-password?token=...`) is valid for 30 minutes and can be used once; a new request invalidates older links
* At most 3 reset e-mails per user per hour
* A successful reset ends every session (access and refresh tokens)
* E-mails are sent after the transaction commits, on a background thread, through a provider-independent `MailSender` (Brevo HTTP API in production, logging in development)

### Supplement Management

Users can:

* Create medicines, vitamins, and supplements
* View and update their supplements
* Update dosage information
* Soft-delete supplements
* Create multiple routines for a supplement

A supplement cannot be deleted while referenced by an active routine.

### Routine Management

Users can:

* Create and view routines
* Update routine day, time, frequency, day of month, and duration
* Soft-delete routines
* Access only routines belonging to their account

Administrators can additionally activate, deactivate, update, and query routines across users.

### Routine Execution

* `GET /api/v1/routine-executions/today` creates/returns today's scheduled executions
* `GET /api/v1/routine-executions/history` supports date filters and pagination
* Executions transition through `PENDING`, `COMPLETED`, `SKIPPED`, and `MISSED`
* Recovery scans the previous seven local calendar days plus today; longer outages can leave older days without generated execution records
* Each routine has a `missedGracePeriod`: `THIRTY_MINUTES`, `SIXTY_MINUTES`, `NINETY_MINUTES`, or `TWO_HOURS`
* Routine creation defaults to `THIRTY_MINUTES`; an omitted value during update preserves the current setting
* Every execution exposes `missedAt`, calculated from `scheduledAt` plus the routine grace period
* A lightweight database-driven scheduler marks expired `PENDING` executions as `MISSED`; completion or skipping is rejected once `missedAt` is reached
* Backdated or subsequently changed routines do not generate historical `MISSED` records using rules that were not active at that time
* `AS_NEEDED` routines are intentionally disabled in V1

### Support (Supporters)

* Users can invite a supporter; the invitation can be accepted, rejected, cancelled, or later revoked
* Access scope is either `ALL_ROUTINES` or `SELECTED_ROUTINES`
* Supporters see only explicitly authorized, active routines

---

## Tech Stack

### Backend

* Java 21
* Spring Boot 4 (Web MVC, Security, Data JPA, Validation, Actuator)
* Hibernate
* PostgreSQL
* Flyway
* JWT (jjwt)
* Maven
* ModelMapper
* Lombok
* Springdoc OpenAPI
* Docker

### Frontend (separate repository)

* React
* TypeScript
* Vite

### Hosting

* Backend + PostgreSQL: Railway
* Frontend: Vercel (proxies `/api/*` to the backend)

---

## Architecture

Supplog follows a layered backend architecture:

```text
Client
  ↓
Filters (rate limit → JWT)
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
PostgreSQL
```

The backend uses DTO-based API contracts, centralized exception handling, resource ownership validation, soft deletion, JPA auditing, Flyway migrations, and environment-based configuration.

### Main Relationships

```text
User
 ├── Roles                  Many-to-Many
 ├── Supplements            One-to-Many
 ├── Routines               One-to-Many
 ├── RefreshTokens          One-to-Many
 └── SupportRelationships   as supported user or supporter

Supplement
 ├── User                   Many-to-One
 └── Routines               One-to-Many

Routine
 ├── User                   Many-to-One
 ├── Supplement             Many-to-One
 └── RoutineExecutions      One-to-Many (one per routine and date)

SupportRelationship
 └── SupportRoutinePermissions   One-to-Many (selected routines)

AdminAuditLog               admin actions with reason and old/new values
```

A detailed project map (packages, service dependencies, schedulers, troubleshooting) is in [`docs/PROJECT_MAP.md`](docs/PROJECT_MAP.md).

---

## Authentication Flow

```text
Username + Password
        ↓
AuthenticationManager → CustomUserDetailsService → password verification
        ↓
Access token (JWT, 15 min) + refresh token (opaque, rotating)
        ↓
Authorization: Bearer <access-token>
        ↓
JwtAuthenticationFilter (signature, expiry, tokenVersion)
        ↓
Spring Security Context
```

Protected requests use:

```http
Authorization: Bearer <access-token>
```

JWTs contain a `tokenVersion` claim. When a security-sensitive operation such as a password change occurs, the stored token version is incremented, invalidating previously issued tokens.

### Refresh tokens & logout

* Login/register return a short-lived access token (15 min, `JWT_EXPIRATION_MS`) plus an opaque refresh token.
* **Web clients** receive the refresh token as an `HttpOnly; Secure; SameSite=Strict` cookie (`supplog_refresh`, `Path=/api/v1/auth`); `refreshToken` in the body is `null`. Refresh/logout read the cookie and require an allowed `Origin` header (`403 ORIGIN_NOT_ALLOWED` otherwise). In production the frontend proxies `/api/*` through Vercel rewrites so the cookie is first-party.
* **Mobile clients** send `X-Client-Type: mobile` and receive/send the refresh token in the JSON body; the cookie is never read for them.
* Refresh tokens are stored only as SHA-256 hashes. Every `POST /api/v1/auth/refresh` rotates the token (sliding 30 days, capped at 90 days per login).
* Reusing an already rotated token (outside a 10 s grace window) revokes the whole session family (`REFRESH_TOKEN_REUSED`).
* A `tokenVersion` change (password change, admin actions, logout-all) invalidates all refresh tokens.
* `POST /api/v1/auth/logout` revokes the current refresh token (idempotent, `204`); `POST /api/v1/users/me/logout-all` ends every session.
* Clients must keep a single in-flight refresh request; on `TOKEN_EXPIRED` call refresh, on `REFRESH_TOKEN_*` / `TOKEN_REVOKED` send the user to login.

| Code | HTTP | Meaning |
| --- | --- | --- |
| `TOKEN_EXPIRED` | 401 | Access token expired → call refresh |
| `TOKEN_REVOKED` | 401 | Session invalidated (password change, logout-all, admin action) |
| `REFRESH_TOKEN_INVALID` | 401 | Missing, unknown or revoked refresh token |
| `REFRESH_TOKEN_EXPIRED` | 401 | Refresh token expired |
| `REFRESH_TOKEN_REUSED` | 401 | Rotated token reused; session family revoked |
| `PASSWORD_RESET_TOKEN_INVALID` | 400 | Reset link unknown, already used or expired |
| `ORIGIN_NOT_ALLOWED` | 403 | Cookie-based request from an origin outside `CORS_ALLOWED_ORIGINS` |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many auth/support requests |

---

## API

Swagger UI (development profile only):

```text
http://localhost:8080/swagger-ui/index.html
```

Public endpoints:

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
POST /api/v1/auth/forgot-password
POST /api/v1/auth/reset-password
```

### API Groups

| Module             | Base Path                             | Access |
| ------------------ | ------------------------------------- | ------ |
| Authentication     | `/api/v1/auth`                        | Public |
| User Profile       | `/api/v1/users`                       | User   |
| Supplements        | `/api/v1/supplements`                 | User   |
| Routines           | `/api/v1/routines`                    | User   |
| Routine executions | `/api/v1/routine-executions`          | User   |
| Support            | `/api/v1/support`                     | User   |
| Admin Users        | `/api/v1/admin/users`                 | Admin  |
| Admin Supplements  | `/api/v1/admin/supplements`           | Admin  |
| Admin Routines     | `/api/v1/admin/routines`              | Admin  |
| Admin Executions   | `/api/v1/admin/executions`            | Admin  |
| Admin Support      | `/api/v1/admin/support-relationships` | Admin  |
| Admin Audits       | `/api/v1/admin/audits`                | Admin  |
| Admin Dashboard    | `/api/v1/admin/dashboard`             | Admin  |

History example:

```http
GET /api/v1/routine-executions/history?dateFrom=2026-09-01&dateTo=2026-09-30&page=0&size=20
Authorization: Bearer <access-token>
```

Admin list endpoints use the same `page` and `size` parameters. Page size is limited to 100.

---

## Running Locally

### Requirements

* Java 21
* PostgreSQL
* Git

Clone the repository:

```bash
git clone https://github.com/memreeger/supplog.git
cd supplog
```

Create a PostgreSQL database named `supplog_db`. Flyway creates the schema on startup.

### Environment Variables

Copy `.env.example` for the full list of variable names. Spring Boot does not read `.env` automatically; pass the values through your IDE run configuration, your shell, or Docker Compose.

| Variable | Description | Default |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` or `prod` | `dev` |
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/supplog_db` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | – |
| `JWT_SECRET` | JWT signing secret (at least 32 bytes) | – |
| `JWT_EXPIRATION_MS` | Access token lifetime | `900000` (15 min) |
| `REFRESH_TOKEN_EXPIRATION_DAYS` | Sliding refresh token lifetime | `30` |
| `REFRESH_TOKEN_ABSOLUTE_EXPIRATION_DAYS` | Maximum session lifetime per login | `90` |
| `REFRESH_TOKEN_REUSE_GRACE_SECONDS` | Tolerance for parallel refreshes | `10` |
| `REFRESH_COOKIE_SECURE` | `Secure` flag on the refresh cookie | `true` (`false` in `dev`) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins; also used for the `Origin` check | `http://localhost:5173` |
| `TRUSTED_PROXY_HOPS` | Proxies in front of the app; client IP is read from `X-Forwarded-For` (right to left). `0` = use the remote address | `0` |
| `MAIL_PROVIDER` | `log` (no mail is sent, the content is logged) or `brevo` | `log` |
| `BREVO_API_KEY` | Brevo transactional API key (required for `brevo`) | – |
| `MAIL_FROM_ADDRESS` | Verified sender address | – |
| `MAIL_FROM_NAME` | Sender display name | `Supplog` |
| `FRONTEND_BASE_URL` | Base URL used in e-mail links | `http://localhost:5173` |
| `PASSWORD_RESET_TOKEN_TTL_MINUTES` | Reset link lifetime | `30` |
| `PASSWORD_RESET_MAX_REQUESTS_PER_HOUR` | Reset e-mails per user per hour | `3` |
| `AUTH_RATE_LIMIT_MAX_REQUESTS` | Auth requests allowed per window | `20` |
| `AUTH_RATE_LIMIT_WINDOW_SECONDS` | Auth rate-limit window | `60` |
| `SUPPORT_RATE_LIMIT_MAX_REQUESTS` | Support requests allowed per window | `120` |
| `SUPPORT_RATE_LIMIT_WINDOW_SECONDS` | Support rate-limit window | `60` |
| `APP_TIME_ZONE` | Dashboard date boundary time zone | `UTC` |
| `ROUTINE_EXECUTION_MISSED_INTERVAL_MS` | Expired execution scheduler interval | `60000` |
| `ROUTINE_EXECUTION_MISSED_INITIAL_DELAY_MS` | Expired execution scheduler initial delay | `60000` |

Real credentials and secrets should never be committed to the repository. Keep actual `.env` files local.

### Run

Windows PowerShell:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/supplog_db"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your-password"
$env:JWT_SECRET="your-long-random-secret-of-at-least-32-bytes"

.\mvnw.cmd spring-boot:run
```

macOS / Linux:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/supplog_db"
export DB_USERNAME="postgres"
export DB_PASSWORD="your-password"
export JWT_SECRET="your-long-random-secret-of-at-least-32-bytes"

./mvnw spring-boot:run
```

With the frontend running locally, its Vite dev server proxies `/api` to `http://localhost:8080`, so the refresh cookie works without extra configuration.

### Docker Compose

`compose.yaml` starts PostgreSQL and the backend with the `prod` profile:

```bash
docker compose up --build
```

Provide `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` and `JWT_SECRET` through a local `.env` file.

### Optional Admin Seed

Supplog automatically ensures that `ROLE_USER` and `ROLE_ADMIN` exist.

An optional administrator can be initialized using:

```text
ADMIN_SEED_ENABLED
ADMIN_SEED_USERNAME
ADMIN_SEED_EMAIL
ADMIN_SEED_PASSWORD
ADMIN_SEED_TIME_ZONE
```

Admin initialization is disabled by default.

For the first production deployment, enable it for one startup with a unique
temporary password, sign in and change that password, then restart with
`ADMIN_SEED_ENABLED=false`. The seeded administrator is marked to require a
password change. Never leave bootstrap credentials in deployment configuration.

---

## Deployment

### Production Checklist

* Start with `SPRING_PROFILES_ACTIVE=prod`.
* Supply `DB_PASSWORD`, a random JWT secret of at least 32 bytes, and explicit CORS origins through the deployment secret store.
* `CORS_ALLOWED_ORIGINS` must contain the frontend origin (e.g. `https://supplog.vercel.app`); cookie-based refresh/logout requests from other origins are rejected.
* Serve the frontend and the API from the same origin (Vercel rewrite of `/api/*`) so the `SameSite=Strict` refresh cookie is first-party. Keep `REFRESH_COOKIE_SECURE=true`.
* Set `TRUSTED_PROXY_HOPS` to the number of proxies in front of the app only after verifying the `X-Forwarded-For` chain; a wrong value makes all users share one rate-limit bucket.
* Terminate HTTPS at the reverse proxy/load balancer and forward standard proxy headers.
* Restrict database network access and use a least-privilege database account.
* Run scheduled PostgreSQL backups and regularly verify restoration in a separate database.
* Keep Swagger disabled publicly; the production profile disables it by default.
* The built-in rate limit is per application instance. Use a shared gateway limit when running multiple instances.
* Deploy the backend before a frontend that depends on new API behaviour.

Detailed database recovery procedure: [`docs/postgres-backup-restore.md`](docs/postgres-backup-restore.md).

### Database Migrations

Flyway runs on startup and Hibernate only validates the schema (`ddl-auto=validate`). Every schema change needs a new migration.

| Version | Change |
| --- | --- |
| V1 | Baseline schema |
| V2 | Role seed |
| V3 | Core constraints |
| V4 | Admin audit log |
| V5 | Core query indexes |
| V6 | Nullable supplement expire date |
| V7 | Routine missed grace period, execution deadlines, partial index `idx_executions_pending_missed_at` |
| V8 | Unique execution per routine and date |
| V9 | Refresh tokens |
| V10 | Password reset tokens |

---

## Tests

```powershell
.\mvnw.cmd clean test
```

Unit and MockMvc tests run without a database. Database-backed tests use the `test` profile and the separate `TEST_DB_URL` database. Never point `TEST_DB_URL` to development or production; the tests require a database name ending in `_test`.

To run PostgreSQL/Flyway, refresh token and concurrency tests:

```powershell
$env:RUN_DB_INTEGRATION_TESTS="true"
$env:TEST_DB_URL="jdbc:postgresql://localhost:5432/supplog_test"
$env:TEST_DB_USERNAME="postgres"
$env:TEST_DB_PASSWORD="your-test-password"
.\mvnw.cmd test
```

---

## Documentation

* [`docs/PROJECT_MAP.md`](docs/PROJECT_MAP.md): project map and troubleshooting reference
* [`docs/FEATURE_DESIGN.md`](docs/FEATURE_DESIGN.md): design and task breakdown for upcoming features
* [`docs/postgres-backup-restore.md`](docs/postgres-backup-restore.md): backup and restore procedure

---

## Roadmap

### Next

* Push notifications with Firebase Cloud Messaging: routine reminders before the scheduled time, missed-routine alerts for supporters
* Supporter nudges: one-way messages from template or free text
* Notification inbox and preferences

### Later

* Flutter mobile app
* E-mail verification
* Active session list and per-device sign out
* Multi-instance distributed rate limiting and scheduler locking
* GitHub Actions CI
* Adherence statistics and late-intake reporting

---

## V1 Scope Notes

V1 supports scheduled daily, weekly, specific-day, and monthly routines. Weekly routines require exactly one weekday; specific-day routines require two to six weekdays. Historical execution records are retained when users, supplements, or routines are deactivated.

---

## Author

Developed by **Muhammed Emre Eğer**

GitHub: [`github.com/memreeger`](https://github.com/memreeger)
