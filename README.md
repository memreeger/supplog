# Supplog

Supplog is a health routine management application for organizing medicines, vitamins, supplements, and recurring intake routines.

The project was inspired by a simple real-life problem: not always remembering whether a medicine or supplement has already been taken.

The current repository contains the **Spring Boot backend REST API**. The frontend is being developed separately with React and TypeScript.

> 🚧 Supplog is under active development. The V1 backend includes routine execution tracking and supporter-based access; reminders and notifications remain future work.

---

## Features

### Authentication & Security

* User registration and login
* JWT-based stateless authentication
* BCrypt password hashing
* `ROLE_USER` and `ROLE_ADMIN` authorization
* Custom JSON `401` and `403` responses
* JWT `tokenVersion` validation
* Automatic token invalidation after password changes
* Protection against authentication for deactivated users
* Resource ownership validation

### User Management

Authenticated users can:

* View and update their profile
* Change their password
* Deactivate their account
* Manage only their own supplements and routines

### Admin Management

Administrators can:

* Search and manage users
* Activate or deactivate users
* Reset passwords
* Change user roles
* Manage supplements and routines across users

Additional safeguards prevent:

* Admin self-deactivation through admin operations
* Admin self-demotion
* Removal of the final active administrator

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
* Update routine day, time, and period
* Soft-delete routines
* Access only routines belonging to their account

Administrators can additionally activate, deactivate, update, and query routines across users.

### Routine Execution & Support

* `GET /api/v1/routine-executions/today` creates/returns today's scheduled executions
* `GET /api/v1/routine-executions/history` supports date filters and pagination
* Executions transition through `PENDING`, `COMPLETED`, `SKIPPED`, and `MISSED`
* Recovery scans the previous seven local calendar days plus today; longer outages can leave older days without generated execution records
* `PENDING` becomes `MISSED` after the user's local date changes, not merely when the scheduled time passes on the same day
* Backdated or subsequently changed routines do not generate historical `MISSED` records using rules that were not active at that time
* Support relationships expose only explicitly authorized active routines
* `AS_NEEDED` routines are intentionally disabled in V1

---

## Tech Stack

### Backend

* Java 21
* Spring Boot 4
* Spring Web
* Spring Security
* Spring Data JPA
* Hibernate
* PostgreSQL
* JWT
* Maven
* ModelMapper
* Lombok
* Jakarta Bean Validation
* Springdoc OpenAPI

### Frontend

* React
* TypeScript

---

## Architecture

Supplog follows a layered backend architecture:

```text
Client
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
PostgreSQL
```

The backend uses DTO-based API contracts, centralized exception handling, resource ownership validation, soft deletion, JPA auditing, and environment-based configuration.

### Main Relationships

```text
User
 ├── Roles        Many-to-Many
 ├── Supplements  One-to-Many
 └── Routines     One-to-Many

Supplement
 ├── User         Many-to-One
 └── Routines     One-to-Many

Routine
 ├── User         Many-to-One
 └── Supplement   Many-to-One
```

---

## Authentication Flow

```text
Username + Password
        ↓
AuthenticationManager
        ↓
CustomUserDetailsService
        ↓
Password verification
        ↓
JWT generation
        ↓
Bearer Token
        ↓
JwtAuthenticationFilter
        ↓
Username + tokenVersion validation
        ↓
Spring Security Context
```

Protected requests use:

```http
Authorization: Bearer <access-token>
```

JWTs contain a `tokenVersion` claim. When a security-sensitive operation such as a password change occurs, the stored token version is incremented, invalidating previously issued tokens.

---

## API

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

Public endpoints:

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
```

### API Groups

| Module            | Base Path                   | Access |
| ----------------- | --------------------------- | ------ |
| Authentication    | `/api/v1/auth`              | Public |
| User Profile      | `/api/v1/users`             | User   |
| Supplements       | `/api/v1/supplements`       | User   |
| Routines          | `/api/v1/routines`          | User   |
| Routine executions| `/api/v1/routine-executions`| User   |
| Support            | `/api/v1/support`            | User   |
| Admin Users       | `/api/v1/admin/users`       | Admin  |
| Admin Supplements | `/api/v1/admin/supplements` | Admin  |
| Admin Routines    | `/api/v1/admin/routines`    | Admin  |
| Admin Executions  | `/api/v1/admin/executions`  | Admin  |

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

Create a PostgreSQL database:

```text
supplog_db
```

### Environment Variables

| Variable            | Description           |
| ------------------- | --------------------- |
| `DB_URL`            | PostgreSQL JDBC URL   |
| `DB_USERNAME`       | Database username     |
| `DB_PASSWORD`       | Database password     |
| `JWT_SECRET`        | JWT signing secret    |
| `JWT_EXPIRATION_MS` | Token expiration time |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins |
| `AUTH_RATE_LIMIT_MAX_REQUESTS` | Auth requests allowed per window |
| `AUTH_RATE_LIMIT_WINDOW_SECONDS` | Auth rate-limit window |
| `SUPPORT_RATE_LIMIT_MAX_REQUESTS` | Support requests allowed per window |
| `SUPPORT_RATE_LIMIT_WINDOW_SECONDS` | Support rate-limit window |
| `APP_TIME_ZONE` | Dashboard date boundary time zone |

Example:

```text
DB_URL=jdbc:postgresql://localhost:5432/supplog_db
DB_USERNAME=postgres
DB_PASSWORD=your-password
JWT_SECRET=your-long-random-secret
JWT_EXPIRATION_MS=3600000
```

Real credentials and secrets should never be committed to the repository.
Copy `.env.example` for the required variable names, but keep actual `.env` files local.

### Production Checklist

* Start with `SPRING_PROFILES_ACTIVE=prod`.
* Supply `DB_PASSWORD`, a random JWT secret of at least 32 bytes, and explicit CORS origins through the deployment secret store.
* Terminate HTTPS at the reverse proxy/load balancer and forward standard proxy headers.
* Restrict database network access and use a least-privilege database account.
* Run scheduled PostgreSQL backups and regularly verify restoration in a separate database.
* Keep Swagger disabled publicly; production profile disables it by default.
* The built-in auth rate limit is per application instance. Use a shared gateway limit when running multiple instances.

Detailed database recovery procedure: [`docs/postgres-backup-restore.md`](docs/postgres-backup-restore.md).

### Optional Admin Seed

Supplog automatically ensures that `ROLE_USER` and `ROLE_ADMIN` exist.

An optional administrator can be initialized using:

```text
ADMIN_SEED_ENABLED
ADMIN_SEED_USERNAME
ADMIN_SEED_EMAIL
ADMIN_SEED_PASSWORD
```

Admin initialization is disabled by default.

For the first production deployment, enable it for one startup with a unique
temporary password, sign in and change that password, then restart with
`ADMIN_SEED_ENABLED=false`. The seeded administrator is marked to require a
password change. Never leave bootstrap credentials in deployment configuration.

### Run

Windows PowerShell:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/supplog_db"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your-password"
$env:JWT_SECRET="your-long-random-secret"

.\mvnw.cmd spring-boot:run
```

macOS / Linux:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/supplog_db"
export DB_USERNAME="postgres"
export DB_PASSWORD="your-password"
export JWT_SECRET="your-long-random-secret"

./mvnw spring-boot:run
```

---

## Tests

```powershell
.\mvnw.cmd clean test
```

Database-backed tests use the `test` profile and the separate `TEST_DB_URL` database. Never point `TEST_DB_URL` to development or production.

Automated test coverage is currently being expanded.

---

## Roadmap

### Backend

* Automated unit and integration test expansion
* Multi-instance distributed rate limiting
* Docker and GitHub Actions

### Product

* Adherence statistics
* Late-intake reporting
* Reminder scheduling
* Notification support
* Supporter relationships
* React frontend integration

---

## V1 Scope Notes

V1 supports scheduled daily, weekly, specific-day, and monthly routines. Weekly routines require exactly one weekday; specific-day routines require two to six weekdays. Historical execution records are retained when users, supplements, or routines are deactivated.

---

## Author

Developed by **Muhammed Emre Eğer**

GitHub: `github.com/memreeger`
