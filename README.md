# Supplog

Supplog is a health routine management application designed to help users organize medicines, vitamins, supplements, and recurring intake routines in one place.

The project was inspired by a real-life challenge: maintaining consistency while using multiple medications and supplements. Supplog aims to turn that challenge into a structured and trackable routine while providing a foundation for reminders, adherence tracking, progress statistics, and supporter-based accountability.

This repository contains the backend REST API built with Spring Boot and PostgreSQL. The frontend application is currently under development.

---

## Project Overview

Supplog allows users to:

* Create and manage a personal account
* Register medicines, vitamins, and supplements
* Create multiple routines for a supplement
* Update dosage, schedule, and profile information
* Securely authenticate using JWT
* Soft-delete user, supplement, and routine records
* Receive structured and localized validation responses

The project is actively being developed with a focus on backend security, clean architecture, data integrity, and maintainability.

---

## Current Features

### Authentication and Security

* User registration
* User login
* JWT-based stateless authentication
* BCrypt password hashing
* Protected API endpoints
* Authentication through Spring Security
* Invalid credential handling
* Soft-deleted user authentication prevention
* Bearer token support in Swagger UI

### User Management

* Retrieve user by ID
* Retrieve user by email
* Retrieve user by username
* List users
* List active users
* Update profile information
* Change password with current-password verification
* Soft-delete user accounts

### Supplement Management

* Create supplements
* Retrieve supplements by ID
* Retrieve supplements by user
* Update supplement information
* Update supplement dosage
* Validate expiration dates
* Soft-delete supplement records

### Routine Management

* Create routines
* Retrieve routines by user
* Create multiple routines for one supplement
* Update routine time
* Update routine day
* Update routine period
* Soft-delete routine records

### API Quality

* DTO-based request and response models
* Bean Validation
* Centralized exception handling
* Turkish and English validation messages
* Invalid JSON, enum, and date format handling
* JPA auditing for creation and update timestamps
* OpenAPI and Swagger documentation

---

## Technology Stack

### Backend

* Java 25
* Spring Boot 4
* Spring Web
* Spring Data JPA
* Spring Security
* JSON Web Token
* Hibernate
* PostgreSQL
* Maven
* ModelMapper
* Lombok
* Springdoc OpenAPI

### Development Concepts

* Layered architecture
* RESTful API design
* Authentication and authorization
* JWT filter chain
* Password hashing
* DTO pattern
* Entity relationships
* Repository pattern
* Service layer pattern
* Dependency injection
* Soft delete
* Bean Validation
* Global exception handling
* Internationalization
* JPA auditing

---

## Architecture

Supplog follows a layered architecture:

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

### Layer Responsibilities

```text
Controller
Handles HTTP requests, request validation, and response status codes.

Service
Contains business rules and coordinates application operations.

Repository
Provides database access through Spring Data JPA.

Entity
Represents database tables and relationships.

DTO
Defines the request and response contracts exposed by the API.

Security
Validates credentials, processes JWT tokens, and protects endpoints.
```

---

## Authentication Flow

```text
User sends username and password
              ↓
AuthenticationManager receives the credentials
              ↓
CustomUserDetailsService loads the user
              ↓
PasswordEncoder verifies the password
              ↓
JwtService generates an access token
              ↓
The client sends the token in protected requests
              ↓
JwtAuthenticationFilter validates the token
              ↓
Spring Security authorizes access to the endpoint
```

The JWT must be sent in the request header:

```http
Authorization: Bearer <access-token>
```

---

## Database Relationships

```text
User
 ├── Supplements: One-to-Many
 └── Routines: One-to-Many

Supplement
 └── Routines: One-to-Many

Routine
 ├── User: Many-to-One
 └── Supplement: Many-to-One
```

A supplement can have multiple routines, allowing scenarios such as:

```text
Vitamin C
 ├── 08:00
 ├── 14:00
 └── 22:00
```

---

## Project Structure

```text
supplog
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.supplog
│   │   │       ├── config
│   │   │       ├── controller
│   │   │       ├── dto
│   │   │       ├── entity
│   │   │       ├── enums
│   │   │       ├── exception
│   │   │       ├── filter
│   │   │       ├── repository
│   │   │       └── service
│   │   └── resources
│   │       ├── application.properties
│   │       ├── messages.properties
│   │       └── messages_en.properties
│   └── test
├── pom.xml
├── mvnw
└── README.md
```

---

## API Documentation

Swagger UI is available while the application is running:

```text
http://localhost:8080/swagger-ui/index.html
```

### Public Endpoints

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
```

All other endpoints require a valid JWT access token.

### Main API Groups

| Module         | Base Path             | Access       |
| -------------- | --------------------- | ------------ |
| Authentication | `/api/v1/auth`        | Public       |
| Users          | `/api/v1/users`       | JWT required |
| Supplements    | `/api/v1/supplements` | JWT required |
| Routines       | `/api/v1/routines`    | JWT required |

---

## Running the Project

### Requirements

* Java 25
* PostgreSQL
* Git

### Clone the Repository

```bash
git clone https://github.com/memreeger/supplog.git
cd supplog
```

### Create the Database

Create a PostgreSQL database:

```text
supplog_db
```

### Environment Variables

The application uses the following environment variables:

| Variable            | Description                        |
| ------------------- | ---------------------------------- |
| `DB_URL`            | PostgreSQL connection URL          |
| `DB_USERNAME`       | PostgreSQL username                |
| `DB_PASSWORD`       | PostgreSQL password                |
| `JWT_SECRET`        | Secret key used to sign JWT tokens |
| `JWT_EXPIRATION_MS` | Access token expiration time       |

Example values:

```text
DB_URL=jdbc:postgresql://localhost:5432/supplog_db
DB_USERNAME=postgres
DB_PASSWORD=your-password
JWT_SECRET=your-long-random-secret
JWT_EXPIRATION_MS=3600000
```

Real credentials and secrets should never be committed to the repository.

### Run the Application

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS or Linux:

```bash
./mvnw spring-boot:run
```

The application starts at:

```text
http://localhost:8080
```

---

## Development Roadmap

### Backend Security

* Add `USER` and `ADMIN` roles
* Create dedicated admin endpoints
* Match accessed resources with the authenticated JWT user
* Prevent users from modifying another user’s data
* Separate normal-user and admin queries
* Standardize `401 Unauthorized` and `403 Forbidden` responses

### Data and Infrastructure

* Complete soft-delete filtering
* Add restore operations for administrators
* Add Flyway database migrations
* Add Docker support
* Add GitHub Actions continuous integration
* Externalize sensitive configuration

### Testing

* Unit tests for service classes
* Integration tests for authentication
* Repository tests
* Resource ownership tests
* Soft-delete behavior tests

### Product Features

* Daily intake tracking
* Routine history
* Missed and completed intake records
* Notification scheduling
* Progress statistics
* Score and achievement system
* Supporter and accountability system
* Google and Facebook authentication
* React frontend
* React Native mobile application

---

## Known Limitations

The following areas are still under development:

* Role-based authorization
* Admin operations
* Resource ownership validation
* Automated test coverage
* Database migration management
* Containerized deployment

These limitations are being addressed as part of the active development roadmap.

---

## Project Status

🚧 **Active Development**

### Completed

* Authentication structure
* JWT security structure
* User management
* Supplement management
* Routine management
* DTO and validation structure
* Global exception handling
* Internationalized messages
* Soft-delete infrastructure
* Swagger documentation

### Current Focus

* Role and authorization structure
* Admin operations
* Resource ownership security
* Automated tests
* Backend code quality
* React frontend integration

---

## Author

Developed by **Muhammed Emre Eger**

GitHub: `github.com/memreeger`
