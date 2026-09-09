# Shoe Shop Backend

Spring Boot backend for a shoe e-commerce system with authentication, product catalog, cart, order, inventory, shipping, and payment modules.

## Tech Stack

- Java 17
- Spring Boot 4
- Spring Security + JWT
- Spring Data JPA / Hibernate
- MySQL
- Redis
- Bean Validation
- MapStruct
- Cloudinary
- VNPAY payment integration
- GHN shipping integration

## Main Features

- Email/password authentication with access token and refresh token
- Role-based authorization for customer and admin APIs
- Product catalog with brands, categories, variants, images, price, and inventory
- Cart and checkout flow based on selected product variants
- Order management with payment status and order status
- VNPAY payment URL generation, return callback, and IPN handling
- GHN shipping fee and shipping order integration
- Centralized exception handling with a consistent `ApiResponse` wrapper
- Environment-based configuration for database, JWT, mail, Cloudinary, GHN, and VNPAY

## Project Structure

```text
src/main/java/com/ttthinh/shoe_shop_basic
|-- config
|-- controller
|-- dto
|   |-- request
|   `-- response
|-- entity
|-- enums
|-- exception
|-- mapper
|-- repository
|-- security
|-- service
`-- validation
```

## Environment Setup

Create a local `.env` or set environment variables based on `.env.example`.

Important variables:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`, `JWT_ACCESS_EXPIRATION`, `JWT_REFRESH_EXPIRATION`
- `ALLOWED_ORIGINS`
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`
- `GHN_TOKEN`, `GHN_SHOP_ID`
- `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_RETURN_URL`, `VNPAY_IPN_URL`

Do not commit real secrets. Use `.env.example` for placeholders only.

## Run Locally

Requirements:

- JDK 17
- MySQL
- Redis if refresh-token/session features are enabled

Build:

```bash
./mvnw -DskipTests package
```

Run:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

## API Modules

- `auth`: login, register, email verification, token refresh, logout
- `catalog`: brands, categories, products, product variants
- `cart`: add, update, remove, and view cart items
- `checkout`: shipping fee calculation and checkout preparation
- `order`: create, view, cancel, and update order status
- `payment`: VNPAY payment URL, callback, and IPN
- `inventory`: stock and product variant inventory management

## Engineering Notes

- Controllers should stay thin and delegate business logic to services.
- API responses should use the existing `ApiResponse<T>` format.
- Product variants are the sellable SKUs. Cart, order, and inventory logic should use variants, not only parent products.
- Order and payment flows should avoid trusting client-side price or payment status.
- Payment callbacks should be idempotent to avoid duplicated payment processing.
