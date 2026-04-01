# Finance Data Processing & Access Control Backend

A production-ready **Spring Boot 3.2 REST API** implementing a finance dashboard system with role-based access control, financial record management, and analytics APIs.

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.4 |
| Security | Spring Security 6 + JWT (JJWT 0.12.x) |
| Database | MySQL 8 |
| ORM | Spring Data JPA / Hibernate |
| Docs | Springdoc OpenAPI 3 (Swagger UI) |
| Tests | JUnit 5 + Mockito |
| Build | Maven 3.9 |

---

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8 running on `localhost:3306`

---

## Setup & Running

### 1. Clone / Open the project
```bash
cd finance-backend
```

### 2. Configure database
Edit `src/main/resources/application.properties` if needed:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/finance_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=root
```
> The database `finance_db` is created automatically on first run.

### 3. Run the application
```bash
mvn spring-boot:run
```

### 4. Run tests
```bash
mvn clean test
```
All **21 unit tests** should pass.

### 5. Access Swagger UI
```
http://localhost:8080/swagger-ui.html
```

---

## Role Model

| Role | Permissions |
|------|------------|
| **VIEWER** | View dashboard summary and category breakdown |
| **ANALYST** | View dashboard (including monthly trends) + view/filter all their own transactions |
| **ADMIN** | Full access: create/update/delete transactions, manage all users, view all data system-wide |

> **Default role on registration**: `VIEWER`

---

## API Reference

### Authentication (Public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login, returns JWT token |

**Register body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "role": "ADMIN"
}
```

**Login response:**
```json
{
  "token": "eyJhbGciOiJIUzI1...",
  "email": "john@example.com",
  "name": "John Doe",
  "role": "ADMIN",
  "message": "Login successful"
}
```

---

### Transactions (`ANALYST` and `ADMIN` read; `ADMIN` write)

| Method | Endpoint | Roles | Description |
|--------|----------|-------|-------------|
| POST | `/api/transactions` | ADMIN | Create a transaction |
| GET | `/api/transactions` | ANALYST, ADMIN | List with pagination + filters |
| GET | `/api/transactions/{id}` | ANALYST, ADMIN | Get by ID |
| PUT | `/api/transactions/{id}` | ADMIN | Update |
| DELETE | `/api/transactions/{id}` | ADMIN | Delete |

**Filter parameters for GET `/api/transactions`:**

| Param | Type | Example | Description |
|-------|------|---------|-------------|
| `page` | int | `0` | Page number (0-indexed) |
| `size` | int | `10` | Records per page |
| `sortBy` | string | `date` | Field to sort by |
| `sortDir` | string | `desc` | `asc` or `desc` |
| `keyword` | string | `rent` | Search in title/category/description |
| `type` | enum | `CREDIT` | `CREDIT` or `DEBIT` |
| `category` | string | `Housing` | Exact category match |
| `dateFrom` | date | `2024-01-01` | Start date (yyyy-MM-dd) |
| `dateTo` | date | `2024-12-31` | End date (yyyy-MM-dd) |

**Transaction body:**
```json
{
  "title": "Monthly Salary",
  "amount": 50000.00,
  "type": "CREDIT",
  "category": "Income",
  "date": "2024-01-15",
  "description": "January salary"
}
```

---

### Dashboard (`VIEWER`, `ANALYST`, `ADMIN`)

| Method | Endpoint | Roles | Description |
|--------|----------|-------|-------------|
| GET | `/api/dashboard/summary` | VIEWER, ANALYST, ADMIN | Full summary |
| GET | `/api/dashboard/categories` | VIEWER, ANALYST, ADMIN | Category-wise totals |
| GET | `/api/dashboard/trends` | ANALYST, ADMIN | Monthly trends (last 6 months) |

**Summary response:**
```json
{
  "totalIncome": 50000.00,
  "totalExpenses": 15000.00,
  "netBalance": 35000.00,
  "totalTransactions": 12,
  "totalCategories": 5,
  "categoryBreakdown": [
    { "category": "Housing", "total": 9000.00, "percentage": "60.0%" }
  ],
  "recentTransactions": [...]
}
```

**Monthly trends response:**
```json
[
  { "year": 2024, "month": 1, "monthName": "JAN", "income": 50000, "expenses": 15000, "net": 35000 }
]
```

---

### User Management (`ADMIN` only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users` | List all users |
| GET | `/api/users/{id}` | Get user by ID |
| PUT | `/api/users/{id}/status` | Activate or deactivate user |
| PUT | `/api/users/{id}/role` | Change user role |

**Update status body:**
```json
{ "active": false }
```

**Update role body:**
```json
{ "role": "ANALYST" }
```

---

## Error Handling

All errors return structured JSON:
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to access this transaction"
}
```

| Status | When |
|--------|------|
| 400 | Validation failure (with field-level details) |
| 401 | Missing or invalid JWT token |
| 403 | Insufficient role/permissions |
| 404 | Resource not found |
| 500 | Unexpected server error |

---

## Project Structure

```
src/main/java/com/finance/
├── FinanceApplication.java
├── config/
│   ├── ApplicationConfig.java    ← Beans: UserDetailsService, PasswordEncoder, AuthManager
│   ├── SecurityConfig.java       ← Spring Security 6 + JWT filter chain
│   └── SwaggerConfig.java        ← OpenAPI 3 with Bearer auth
├── controller/
│   ├── AuthController.java
│   ├── TransactionController.java
│   ├── DashboardController.java
│   └── UserManagementController.java
├── dto/                          ← Request/Response objects
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
├── model/
│   ├── User.java                 ← JPA entity, implements UserDetails
│   ├── Transaction.java
│   ├── Role.java                 ← VIEWER, ANALYST, ADMIN
│   └── TransactionType.java      ← CREDIT, DEBIT
├── repository/
│   ├── UserRepository.java
│   └── TransactionRepository.java ← JpaSpecificationExecutor + dashboard queries
├── security/
│   └── JwtAuthFilter.java
├── service/
│   ├── AuthService.java
│   ├── JwtService.java
│   ├── TransactionService.java
│   ├── DashboardService.java
│   └── UserManagementService.java
└── specification/
    └── TransactionSpecification.java ← Composable JPA Specifications for filtering
```

---

## Assumptions & Design Decisions

1. **Default role is VIEWER** — New registrations get the most restrictive role unless specified.
2. **ADMIN sees system-wide data** — On dashboard and transactions, ADMIN gets all users' data; ANALYST and VIEWER see only their own.
3. **Monthly trends restricted to ANALYST+** — VIEWER can see summary/category totals but not time-series trends (more analytical).
4. **Soft-deactivation** — Users can be deactivated (not deleted). Deactivated users cannot log in because `isEnabled()` returns `false`, which Spring Security enforces automatically.
5. **JWT expiry is 24 hours** — Configurable via `application.security.jwt.expiration` in milliseconds.
6. **No soft delete on transactions** — Transactions are hard deleted. A soft-delete column (`deleted_at`) could be added as an enhancement.
7. **`createDatabaseIfNotExist=true`** — MySQL creates the database automatically; no manual SQL script needed.
8. **`ddl-auto=update`** — Hibernate manages schema changes automatically in development. For production, use Flyway/Liquibase migrations.

---

## Quick Start (Postman / Swagger)

```bash
# 1. Register an admin
POST /api/auth/register
Body: { "name": "Admin", "email": "admin@test.com", "password": "admin123", "role": "ADMIN" }

# 2. Login and get token
POST /api/auth/login
Body: { "email": "admin@test.com", "password": "admin123" }

# 3. Use token in Swagger: Click "Authorize" → Enter: Bearer <token>

# 4. Create a transaction
POST /api/transactions
Body: { "title": "Salary", "amount": 50000, "type": "CREDIT", "category": "Income", "date": "2024-01-15" }

# 5. View dashboard
GET /api/dashboard/summary

# 6. Filter transactions
GET /api/transactions?type=CREDIT&dateFrom=2024-01-01&dateTo=2024-12-31
```
