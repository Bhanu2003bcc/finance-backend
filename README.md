# Zorvyn Finance Dashboard — Backend API

A production-grade Finance Data Processing and Access Control backend built with **Java 21**, **Spring Boot 3.2**, and **PostgreSQL** : Supabase a cloud based database service provider.

**POV** : This application is built as a shared, company-wide financial system, not a personal finance tracker. When a **VIEWER** logs in and looks at the dashboard, they are not looking at "their" personal data. They are looking at the aggregated data created by all the **ANALYST** and **ADMIN** users in the system.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Architecture Overview](#architecture-overview)
3. [Project Structure](#project-structure)
4. [Setup & Running Locally](#setup--running-locally)
5. [Environment Variables](#environment-variables)
6. [API Reference](#api-reference)
7. [Role & Access Control Matrix](#role--access-control-matrix)
8. [Authentication Flow](#authentication-flow)
9. [Design Decisions & Assumptions](#design-decisions--assumptions)
10. [Running Tests](#running-tests)

---

## Tech Stack

| Layer          | Technology                          |
|----------------|-------------------------------------|
| Language       | Java 21                             |
| Framework      | Spring Boot 3.2.4                   |
| Security       | Spring Security + JWT (jjwt 0.12.5) |
| Persistence    | Spring Data JPA + Hibernate         |
| Database       | PostgreSQL (cloud)                  |
| Validation     | Jakarta Bean Validation             |
| API Docs       | SpringDoc OpenAPI 2 (Swagger UI)    |
| Build Tool     | Maven                               |
| Testing        | JUnit 5 + Mockito + AssertJ         |

---

## Architecture Overview

```
Request → JwtAuthenticationFilter → SecurityFilterChain
                                          │
                                    Controller
                                          │
                                      Service
                                          │
                                   Repository (JPA)
                                          │
                                     PostgreSQL
```

The project follows a strict layered architecture:

- **Model** — JPA entities (`User`, `Transaction`) with lifecycle hooks
- **Repository** — Spring Data interfaces + `JpaSpecificationExecutor` for dynamic filtering
- **Service** — Business logic, access control enforcement, transaction management
- **Controller** — HTTP layer: request parsing, response wrapping
- **Security** — Stateless JWT authentication; role-based route + method security
- **Exception** — Centralized `GlobalExceptionHandler` → consistent `ApiResponse` envelopes
- **DTO** — Separate `request` and `response` objects; entities never exposed directly

---

## Project Structure

```
src/main/java/com/zorvyn/finance/
├── FinanceBackendApplication.java
├── config/
│   ├── SecurityConfig.java          # Spring Security + JWT filter chain
│   └── OpenApiConfig.java           # Swagger UI with Bearer auth
├── controller/
│   ├── AuthController.java          # POST /api/auth/register, /login
│   ├── UserController.java          # /api/users/**
│   ├── TransactionController.java   # /api/transactions/**
│   └── DashboardController.java     # GET /api/dashboard/summary
├── dto/
│   ├── request/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── UpdateUserRequest.java
│   │   ├── TransactionRequest.java
│   │   └── TransactionFilterRequest.java
│   └── response/
│       ├── ApiResponse.java          # Universal response envelope
│       ├── AuthResponse.java
│       ├── UserResponse.java
│       ├── TransactionResponse.java
│       └── DashboardSummaryResponse.java
├── exception/
│   ├── AppExceptions.java            # Domain exception classes
│   └── GlobalExceptionHandler.java   # @RestControllerAdvice
├── model/
│   ├── User.java
│   ├── Transaction.java
│   ├── Role.java                     # VIEWER | ANALYST | ADMIN
│   └── TransactionType.java          # INCOME | EXPENSE
├── repository/
│   ├── UserRepository.java
│   ├── TransactionRepository.java
│   └── TransactionSpecification.java # Dynamic filter predicates
├── security/
│   ├── JwtUtil.java                  # Token generation & validation
│   └── JwtAuthenticationFilter.java  # OncePerRequestFilter
├── service/
│   ├── AuthService.java
│   ├── UserService.java
│   ├── TransactionService.java
│   ├── DashboardService.java
│   └── impl/
│       ├── AuthServiceImpl.java
│       ├── UserDetailsServiceImpl.java
│       ├── UserServiceImpl.java
│       ├── TransactionServiceImpl.java
│       └── DashboardServiceImpl.java
└── util/
    └── DataSeeder.java              # Seeds default ADMIN on first run
```

---

## Setup & Running Locally

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL database (local or cloud)

### Steps

```bash
# 1. Clone the repository
git clone <repository-url>
cd finance-backend

# 2. Configure your PostgreSQL connection (see Environment Variables below)
#    Either export environment variables OR edit application.properties directly

# 3. Build the project
mvn clean package -DskipTests

# 4. Run the application
java -jar target/finance-backend-1.0.0.jar

# OR run directly with Maven
mvn spring-boot:run
```

The server starts on **http://localhost:8080**

Swagger UI is available at: **http://localhost:8080/swagger-ui.html**

If you feel any problem during testing APIs on Swagger UI, Please use **Postman**

| Authorization | Bearer Token |
|---------------|--------------|


### Default Admin Account (seeded automatically)

| Field    | Value             |
|----------|-------------------|
| Email    | Bhanu12@gmail.com |
| Password | Bhanu@1234        |

> **⚠️ Change this password immediately in production.**

---

## Environment Variables

| Variable          | Description                          | Default                          |
|-------------------|--------------------------------------|----------------------------------|
| `DB_URL`          | JDBC URL to your PostgreSQL database | `jdbc:postgresql://localhost:5432/finance_db` |
| `DB_USERNAME`     | Database username                    | `postgres`                       |
| `DB_PASSWORD`     | Database password                    | `postgres`                       |
| `JWT_SECRET`      | Base64-encoded HMAC-SHA256 secret    | (built-in dev secret)            |
| `JWT_EXPIRATION_MS` | JWT lifetime in milliseconds       | `86400000` (24 hours)            |

**Example (Linux/macOS):**
```bash
export DB_URL=jdbc:postgresql://your-cloud-host:5432/finance_db
export DB_USERNAME=your_user
export DB_PASSWORD=your_password
export JWT_SECRET=YourBase64EncodedSecretKeyHere
mvn spring-boot:run
```

**For cloud PostgreSQL**, simply set `DB_URL` to your provider's JDBC connection string, e.g.:
```
jdbc:postgresql://db.example.supabase.co:5432/postgres?sslmode=require
```

---

## API Reference

All responses use the envelope:
```json
{
  "success": true,
  "message": "...",
  "data": { ... },
  "timestamp": "2024-03-15T10:30:00"
}
```

### Authentication

| Method | Endpoint               | Access  | Description              |
|--------|------------------------|---------|--------------------------|
| POST   | `/api/auth/register`   | Public  | Register a new user      |
| POST   | `/api/auth/login`      | Public  | Login and get JWT token  |

**Register body:**
```json
{
  "fullName": "Lambu",
  "email": "lambu123@gmail.com",
  "password": "lambu@123",
  "role": "ANALYST"
}
```

**Login body:**
```json
{
  "email": "lambu123@gmail.com",
  "password": "lambu@123"
}
```

**Login response (data field):**
```json
{
  "token": "eyJhbGci...",
  "tokenType": "Bearer",
  "userId": 2,
  "fullName": "Lambu",
  "email": "lambu123@gmail.com",
  "role": "ANALYST"
}
```

---

### Users

| Method | Endpoint         | Access        | Description                   |
|--------|------------------|---------------|-------------------------------|
| GET    | `/api/users/me`  | Any role      | Get own profile               |
| GET    | `/api/users`     | ADMIN         | List all users                |
| GET    | `/api/users/{id}`| ADMIN         | Get user by ID                |
| PUT    | `/api/users/{id}`| Self or ADMIN | Update user (role → ADMIN only)|
| DELETE | `/api/users/{id}`| ADMIN         | Delete a user                 |

---

### Transactions

| Method | Endpoint                  | Access              | Description                         |
|--------|---------------------------|---------------------|-------------------------------------|
| POST   | `/api/transactions`       | ANALYST, ADMIN      | Create a financial record           |
| GET    | `/api/transactions`       | All roles           | List with filters + pagination      |
| GET    | `/api/transactions/{id}`  | All roles           | Get single transaction              |
| PUT    | `/api/transactions/{id}`  | ANALYST(own), ADMIN | Update a transaction                |
| DELETE | `/api/transactions/{id}`  | ANALYST(own), ADMIN | Soft-delete a transaction           |

**Query parameters for GET `/api/transactions`:**

| Parameter   | Type   | Description                              |
|-------------|--------|------------------------------------------|
| `type`      | String | `INCOME` or `EXPENSE`                    |
| `category`  | String | Category name (case-insensitive)         |
| `startDate` | Date   | `yyyy-MM-dd` — lower bound on date       |
| `endDate`   | Date   | `yyyy-MM-dd` — upper bound on date       |
| `keyword`   | String | Searches in `notes` and `category`       |
| `page`      | Int    | Page number (0-based, default `0`)       |
| `size`      | Int    | Page size (default `20`, max `100`)      |

**Transaction body (create / update):**
```json
{
  "amount": 4500.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2024-03-01",
  "notes": "March salary"
}
```

---

### Dashboard

| Method | Endpoint                   | Access   | Description                   |
|--------|----------------------------|----------|-------------------------------|
| GET    | `/api/dashboard/summary`   | All roles| Full aggregated summary       |

**Query parameters:**

| Parameter    | Default | Description                                  |
|--------------|---------|----------------------------------------------|
| `monthsBack` | `6`     | Months of trend history to include (1–24)    |

**Response (data field):**
```json
{
  "totalIncome": 15000.00,
  "totalExpenses": 8500.00,
  "netBalance": 6500.00,
  "categoryTotals": {
    "salary": 12000.00,
    "freelance": 3000.00,
    "food": 2500.00,
    "rent": 5000.00
  },
  "recentTransactions": [ ... ],
  "monthlyTrends": [
    {
      "month": "2024-01",
      "income": 5000.00,
      "expenses": 3000.00,
      "net": 2000.00
    }
  ]
}
```

---

## Role & Access Control Matrix

| Action                          | VIEWER | ANALYST       | ADMIN |
|---------------------------------|--------|---------------|-------|
| Login / Register                | ✅     | ✅            | ✅    |
| View own profile                | ✅     | ✅            | ✅    |
| View transactions               | ✅     | ✅            | ✅    |
| View dashboard summary          | ✅     | ✅            | ✅    |
| Create transaction              | ❌     | ✅            | ✅    |
| Update own transaction          | ❌     | ✅            | ✅    |
| Update any transaction          | ❌     | ❌            | ✅    |
| Delete own transaction          | ❌     | ✅            | ✅    |
| Delete any transaction          | ❌     | ❌            | ✅    |
| List all users                  | ❌     | ❌            | ✅    |
| Change user roles               | ❌     | ❌            | ✅    |
| Activate / deactivate users     | ❌     | ❌            | ✅    |
| Delete users                    | ❌     | ❌            | ✅    |

Access control is enforced at **two levels**:
1. **Route level** — `SecurityConfig` via `authorizeHttpRequests()`
2. **Method level** — `@PreAuthorize` annotations on controllers
3. **Business logic level** — ownership checks in service layer

---

## Authentication Flow

```
Client                        Server
  │                              │
  ├── POST /api/auth/login ──────►│
  │   {email, password}          │ Authenticates via AuthenticationManager
  │                              │ Generates JWT (24h expiry)
  │◄── {token: "Bearer eyJ..."} ─┤
  │                              │
  ├── GET /api/transactions ─────►│
  │   Authorization: Bearer eyJ..│ JwtAuthenticationFilter validates token
  │                              │ Sets SecurityContext
  │                              │ Controller → Service → Repository
  │◄── {success: true, data: []} ┤
```

---

## Design Decisions & Assumptions

1. **Soft Delete** — Transactions are never physically deleted. A `deleted` boolean flag is set to `true`. This preserves the audit trail for financial data while hiding records from normal queries.

2. **Role Assignment on Register** — Any user can register with any role via the public `/api/auth/register` endpoint for development convenience. In a real production system, the `role` field would be restricted to ADMIN-authenticated requests only.

3. **Ownership semantics** — ANALYST users can only modify/delete transactions they created themselves. ADMIN can operate on any transaction.

4. **Category normalization** — Categories are stored in lowercase (handled in `@PrePersist`/`@PreUpdate`) to ensure consistent grouping in aggregations (`GROUP BY category`).

5. **Database schema** — `spring.jpa.hibernate.ddl-auto=update` is used for convenience during development. In production this should be changed to `validate` and migrations should be handled with Flyway or Liquibase.

6. **JWT Secret** — A default secret is included for local development. This **must** be replaced with a cryptographically secure secret in any deployed environment via the `JWT_SECRET` environment variable.

7. **Pagination** — All list endpoints return paginated responses. Page size is clamped to a max of 100 to prevent unbounded queries.

8. **Monthly trend query** — Uses a native PostgreSQL `TO_CHAR(date, 'YYYY-MM')` query for efficient server-side aggregation rather than pulling raw rows and grouping in Java.

---

## Running Tests

```bash
# Run all unit tests
mvn test

# Run with verbose output
mvn test -Dsurefire.useFile=false
```

Test coverage includes:
- `AuthServiceTest` — register, login, duplicate email, default role
- `TransactionServiceTest` — CRUD, ownership enforcement, soft delete
- `UserServiceTest` — list, find, update (role guard), delete (self-guard)
