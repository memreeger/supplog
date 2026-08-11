# Supplog

Supplog is a health routine management application for organizing medicines, vitamins, supplements, and recurring intake routines.

The project was inspired by a simple real-life problem: not always remembering whether a medicine or supplement has already been taken.

The current repository contains the **Spring Boot backend REST API**. The frontend is being developed separately with React and TypeScript.

> 🚧 Supplog is under active development. Upcoming features include intake tracking, adherence statistics, reminders, and supporter-based accountability.

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
| Admin Users       | `/api/v1/admin/users`       | Admin  |
| Admin Supplements | `/api/v1/admin/supplements` | Admin  |
| Admin Routines    | `/api/v1/admin/routines`    | Admin  |

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

Example:

```text
DB_URL=jdbc:postgresql://localhost:5432/supplog_db
DB_USERNAME=postgres
DB_PASSWORD=your-password
JWT_SECRET=your-long-random-secret
JWT_EXPIRATION_MS=3600000
```

Real credentials and secrets should never be committed to the repository.

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

Automated test coverage is currently being expanded.

---

## Roadmap

### Backend

* Consistent transaction boundaries
* `readOnly` transaction strategy
* Disable Open Session in View
* N+1 query analysis and optimization
* Pagination
* Flyway database migrations
* `dev`, `test`, and `prod` profiles
* Automated unit and integration tests
* Docker and GitHub Actions

### Product

* Intake and adherence tracking
* `TAKEN`, `MISSED`, `SKIPPED`, and `LATE` intake states
* Intake history and adherence statistics
* Reminder scheduling
* Notification support
* Supporter relationships
* React frontend integration

---

## Next Major Feature

The next major product feature is intake tracking:

```text
Routine
   ↓
Scheduled Intake
   ↓
TAKEN / MISSED / SKIPPED / LATE
   ↓
History
   ↓
Adherence Statistics
```

This will extend Supplog from routine management into actual medicine and supplement adherence tracking.

---

## Author

Developed by **Muhammed Emre Eğer**

GitHub: `github.com/memreeger`
