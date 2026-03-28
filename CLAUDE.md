# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Start infrastructure (PostgreSQL, Redis, RabbitMQ)
docker-compose up -d

# Run the application
./mvnw spring-boot:run

# Build
./mvnw clean install

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ClassName

# Run a single test method
./mvnw test -Dtest=ClassName#methodName
```

Swagger UI is available at `http://localhost:8080/swagger-ui.html` when the app is running.

## Architecture

This is a Spring Boot 3.5 / Java 21 REST API with a layered architecture. The layers, in order from bottom to top:

- **`entity/`** — JPA entities mapped to PostgreSQL. `User`, `Product`, `Order`, `OrderItem`. Products use `@Version` for optimistic locking and an `active` flag for soft deletes. Orders cascade deletes to `OrderItem`.
- **`repository/`** — Spring Data JPA repositories. One interface per entity.
- **`security/`** — JWT-based stateless auth. `JwtUtil` signs/validates tokens (JJWT 0.12); `JwtAuthFilter` extracts the Bearer token and sets `SecurityContext`; `UserDetailsServiceImpl` loads users by username; `SecurityConfig` wires everything together and permits `/api/auth/**` and Swagger endpoints without authentication.
- **`enums/`** — `OrderStatus` enum.

Service and controller layers have not yet been implemented.

## Key Configuration

`src/main/resources/application.yaml` — configures:
- PostgreSQL at `localhost:5432`, database `ecommerce`
- Redis at `localhost:6379`
- RabbitMQ at `localhost:5672`
- Server port `8080`
- JWT secret (`jwt.secret`) and expiration (`jwt.expiration`, default 24 h)

The JWT secret in the repo is a placeholder; it must be replaced before production use.

## Infrastructure

`docker-compose.yml` defines PostgreSQL 16, Redis, and RabbitMQ for local development. Start with `docker-compose up -d` before running the application.
