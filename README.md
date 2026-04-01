# Finance Backend API

A Spring Boot REST API for finance data processing with JWT authentication and role-based access control.

## Tech Stack
- Java 17 + Spring Boot 3.2
- MySQL + Spring Data JPA
- JWT Authentication (JJWT)
- Swagger UI
- JUnit 5 + Mockito (21 tests)

## Setup

1. Create MySQL database:
```sql
   CREATE DATABASE finance_db;
```

2. Update `application.properties`:
```properties
   spring.datasource.password=YOUR_PASSWORD
```

3. Run the app:
```bash
   mvn spring-boot:run
```

4. Open Swagger UI:
```
   http://localhost:8080/swagger-ui.html
```

## Roles & Access
| Role | GET transactions | POST/PUT/DELETE | Dashboard |
|------|-----------------|-----------------|-----------|
| VIEWER | Own records only | No | No |
| ANALYST | Own records only | No | Yes |
| ADMIN | All records | Yes | Yes |

## Key Design Decisions
- ADMIN sees all transactions; other roles see only their own
- Soft delete used (records flagged as deleted, not removed)
- JWT expiry: 24 hours
- H2 in-memory DB used for tests (no MySQL needed to run tests)

## Run Tests
```bash
mvn clean test
```
