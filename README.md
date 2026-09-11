# Shoe Shop Source

Full-stack shoe e-commerce project with a Spring Boot backend and a React/Vite frontend.

## Project Structure

```text
shoe_shop_source/
|-- BE/                       Spring Boot backend
|-- FE/                       React + Vite frontend
|-- docs/                     Project-level documentation and audit notes
|-- docker-compose.prod.yml   Production-style Docker Compose stack
|-- .env.production.example   Example production environment file
`-- README.md
```

## Tech Stack

Backend:

- Java 17
- Spring Boot 4
- Spring Security + JWT
- Spring Data JPA / Hibernate
- MySQL
- Redis
- Flyway
- MapStruct
- Cloudinary
- GHN shipping integration
- VNPay payment integration

Frontend:

- React 18
- TypeScript
- Vite
- React Router
- TanStack Query
- Axios
- Lucide React

## Main Features

- Email/password authentication, Google login, JWT access token, refresh token, logout, and email verification.
- Customer storefront with product browsing, product detail, cart, checkout, orders, addresses, wishlist, reviews, and payment result pages.
- Admin pages for products, variants, categories, brands, users, orders, inventory, vouchers, and product discounts.
- Checkout flow with inventory locking, voucher validation, product discounts, shipping fee snapshot, COD, and VNPay.
- GHN shipping support with mock/real mode, webhook verification, and optional polling reconciliation.
- Product review flow scoped to delivered order items.
- OpenAI-powered shop chatbot endpoint with fallback mode when disabled or missing an API key.
- Production-oriented Docker setup with MySQL, Redis, backend, frontend, healthchecks, Flyway, and startup secret validation.

## Requirements

- JDK 17
- Node.js 18+
- npm
- MySQL 8+
- Redis
- Docker and Docker Compose, only if running the production-style stack

## Backend Setup

Create local backend environment variables from the example file:

```powershell
cd BE
Copy-Item .env.example .env
```

Update `BE/.env` for your local database, Redis, JWT secret, mail, Cloudinary, GHN, VNPay, and OAuth settings.

Run backend tests:

```powershell
cd BE
.\mvnw.cmd -q test
```

Run backend locally:

```powershell
cd BE
.\mvnw.cmd spring-boot:run
```

The backend defaults to:

```text
http://localhost:8080
```

## Frontend Setup

Create local frontend environment variables from the example file:

```powershell
cd FE
Copy-Item .env.example .env
```

Make sure `VITE_API_BASE_URL` points to the backend:

```text
VITE_API_BASE_URL=http://localhost:8080
```

Install dependencies:

```powershell
cd FE
npm install
```

Run frontend locally:

```powershell
cd FE
npm run dev
```

The frontend dev server defaults to:

```text
http://localhost:5173
```

Build frontend:

```powershell
cd FE
npm run build
```

## Production-Style Docker Run

Create a production environment file:

```powershell
Copy-Item .env.production.example .env.production
```

Update `.env.production` with real values. At minimum, replace:

- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `JWT_SECRET`
- `GHN_WEBHOOK_SECRET`

Start the stack:

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml up --build
```

Default exposed services:

```text
Frontend: http://localhost
Backend:  http://localhost:8080
MySQL:    localhost:3306
Redis:    localhost:6379
```

## Verification

Before committing or deploying, run:

```powershell
cd BE
.\mvnw.cmd -q test
```

```powershell
cd FE
npm run build
```

Both commands should pass.

## Environment And Secrets

Do not commit real secrets. Commit only example files such as:

- `.env.production.example`
- `BE/.env.example`
- `FE/.env.example`

Local files such as `.env`, `.env.local`, and `.env.production` are ignored by Git.

## Git Notes

This repository is intended to be used as a monorepo. `BE` and `FE` should be normal folders inside the root repository, not nested Git repositories or submodules.

If `BE/.git` or `FE/.git` exists, remove or rename those nested Git directories before committing from the root repository.
