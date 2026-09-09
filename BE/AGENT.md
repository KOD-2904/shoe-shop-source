# AGENT.md

## 1. Project Overview

This project is an e-commerce shoe store system.

The system sells fixed shoe SKUs such as sneakers, running shoes, sandals, leather shoes, boots, sport shoes, limited editions, and accessories.

Core business characteristics:

- Products are sold as fixed SKUs, not custom-made items.
- A product can have multiple variants by size and color.
- Each variant has its own SKU, price, inventory, images, and optional barcode.
- Customers cannot checkout anonymously.
- Email is used for login and OTP delivery.
- Phone number is an important customer identity field and must be unique when provided.
- Orders must keep snapshot data because product prices, discounts, shipping fees, and payment status can change later.
- Inventory must be handled carefully to avoid overselling, especially for popular sizes.
- Payment callback/IPN handling must be idempotent to avoid duplicate payment processing.
- Shipping uses GHN.
- Online payment uses VNPAY.

The backend should keep a clear authentication and authorization model:

- `user_account`
- `role`
- `permission`
- `user_role`
- `role_permission`
- `email_verify_token`
- `user_provider`

Do not remove or rewrite these authentication concepts unless explicitly asked.

---

## 2. Tech Stack

Use the following stack unless the user explicitly changes it:

### Backend

- Java 17
- Spring Boot 4.x or the current Spring Boot version already used by the project
- Spring Security
- Spring Data JPA / Hibernate
- Bean Validation
- MySQL or PostgreSQL
- Lombok
- JWT authentication
- OAuth2 Google Login if already available
- Email OTP using Java Mail Sender or external email provider
- VNPAY payment integration
- GHN shipping integration

### Frontend

- React.js or Next.js
- TailwindCSS
- Mobile-first UI
- API-based communication with backend

### Database

- Use relational design.
- Prefer clear foreign keys.
- Prefer transaction-safe design for inventory, payment, and order workflow.
- Do not merge unrelated modules into huge tables.

### Optional Infrastructure

- Redis for caching and temporary state:
  - OTP rate limiting
  - Refresh token/session tracking
  - Cart or checkout temporary data
  - Product cache
  - GHN province/district/ward cache

---

## 3. General Codex Rules

When modifying this repository:

1. Read the existing code structure before editing.
2. Do not rewrite the whole project unless explicitly requested.
3. Prefer small, focused changes.
4. Keep naming consistent with the existing codebase.
5. Do not introduce unnecessary frameworks.
6. Do not delete existing files unless explicitly requested.
7. Do not change public API response formats unless required.
8. Do not hardcode secrets, tokens, passwords, private keys, or credentials.
9. Do not add fake production credentials.
10. Do not silently ignore exceptions.
11. After changing backend code, check whether DTOs, services, repositories, controllers, and exception handlers still match.
12. If adding database entities, include proper relationships and constraints.
13. If adding business logic, place it in service classes, not controllers.
14. Controllers should be thin.
15. Avoid circular dependencies.
16. Keep code understandable for a student project report.
17. Keep external integrations behind dedicated service classes.

---

## 4. Backend Architecture Rules

Use layered architecture.

Recommended package structure:

```text
org.example.shoe_store
├── config
├── controller
├── dto
│   ├── request
│   └── response
├── entity
├── enums
├── exception
├── mapper
├── repository
├── security
├── service
│   └── impl
└── utils
```

Rules:

- `controller`: receive HTTP requests, validate input, return API response.
- `service`: contain business logic.
- `repository`: database access only.
- `entity`: JPA entities only.
- `dto.request`: request DTOs.
- `dto.response`: response DTOs.
- `mapper`: convert entity to DTO and DTO to entity.
- `exception`: centralized exception handling.
- `security`: JWT, OAuth2, filters, user details, security config.
- `config`: CORS, security, external provider, and environment configuration.

Controllers must not directly access repositories.

Bad:

```java
@RestController
public class ProductController {
    private final ProductRepository productRepository;
}
```

Good:

```java
@RestController
public class ProductController {
    private final ProductService productService;
}
```

---

## 5. API Response Rule

If the project already uses an `ApiResponse<T>` wrapper, continue using it.

Recommended success format:

```json
{
  "code": 200,
  "message": "Success",
  "result": {}
}
```

Recommended error format:

```json
{
  "code": 4001,
  "message": "Product variant is out of stock",
  "errors": []
}
```

Validation error format should be easy for frontend to handle:

```json
{
  "code": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "Email must be valid"
    }
  ]
}
```

Rules:

1. Do not randomly return raw entities from controllers.
2. FE should receive a consistent response shape for success and error.
3. Do not expose stack traces to users.
4. Do not expose raw external provider error responses directly.
5. Use clear error codes for business errors.
6. API response and error design may reference reputable open-source Spring Boot projects, but do not copy code blindly.

---

## 6. Authentication & Authorization Rules

The auth model must be respected:

- `user_account.email` is unique and required.
- `user_account.phone` is unique when provided.
- `user_account.google_id` is unique when provided.
- `user_account.status` can be `ACTIVE`, `INACTIVE`, or `BANNED`.
- `role` and `permission` are many-to-many through `role_permission`.
- `user_account` and `role` are many-to-many through `user_role`.

Recommended roles:

- `CUSTOMER`
- `STAFF`
- `ADMIN`
- `WAREHOUSE_MANAGER`

Rules:

1. Do not store raw passwords.
2. Use `PasswordEncoder` for password hashing.
3. Do not expose password or password hash in response DTOs.
4. Do not expose refresh tokens unnecessarily.
5. Do not log access tokens or refresh tokens.
6. Do not allow banned users to authenticate.
7. Keep Google OAuth2 login compatible with the existing auth flow if available.
8. Use Spring Security authorization rules for protected APIs.
9. Admin-only APIs must require proper roles/permissions.
10. Customer APIs must only access the current authenticated user's data unless admin permission is present.
11. Do not trust client-provided `userId` when authenticated user can be read from `SecurityContext`.
12. Warehouse APIs should require warehouse/admin permissions.

---

## 7. JWT Rules

When editing JWT-related code:

1. Keep access token and refresh token responsibilities separate.
2. Access token should be short-lived.
3. Refresh token should be longer-lived and revocable.
4. Prefer storing refresh token session metadata in Redis or database.
5. Include stable user identity in token claims.
6. If `userId` is already used as subject/claim, keep it consistent.
7. Validate token signature and expiration.
8. Do not trust client-provided user IDs when authenticated user can be read from SecurityContext.

Recommended approach:

- Use `userId` internally for database lookup.
- Use email for login credential.
- Use phone as customer identity/business identity after verification.

---

## 8. OTP & Email Rules

The system uses email as the main OTP channel.

OTP purposes may include:

- Register account
- Verify email
- Forgot password
- Verify checkout phone/email ownership
- Delivery confirmation if implemented

Rules:

1. OTP must expire.
2. OTP must be single-use.
3. Do not store OTP in plain text if security is being improved.
4. Apply rate limiting:
   - Resend locked for 60 seconds.
   - Maximum 3 OTP requests per 15 minutes.
   - Temporary lock for 1 hour if exceeded.
5. Do not reveal whether an email exists in sensitive flows unless the project already allows it.
6. Email sending logic should be in a dedicated service.

---

## 9. Environment Variable Rules

Use `.env` or environment variables for sensitive and environment-specific configuration.

Never commit production `.env` values.

Provide `.env.example` with placeholder values.

Recommended variables:

```text
APP_NAME=shoe-store
APP_ENV=local

DB_URL=
DB_USERNAME=
DB_PASSWORD=

JWT_SECRET=
JWT_ACCESS_EXPIRATION=
JWT_REFRESH_EXPIRATION=

FRONTEND_URL=http://localhost:3000
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

MAIL_HOST=
MAIL_PORT=
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=

VNPAY_TMN_CODE=
VNPAY_HASH_SECRET=
VNPAY_PAY_URL=
VNPAY_RETURN_URL=
VNPAY_IPN_URL=

GHN_TOKEN=
GHN_SHOP_ID=
GHN_BASE_URL=
```

Rules:

1. `application.yml` or `application.properties` should read from environment variables.
2. Do not hardcode secrets in Java code.
3. Do not log secret values.
4. Tests should use test config, mocks, or safe fake values.
5. Keep `.env` in `.gitignore`.

---

## 10. CORS Rules

CORS must be configured explicitly for frontend integration.

Rules:

1. Configure CORS in Spring Security or a central config class.
2. Read allowed origins from environment variables.
3. Do not use `allowedOrigins("*")` together with `allowCredentials(true)`.
4. Allow needed methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.
5. Allow needed headers: `Authorization`, `Content-Type`.
6. Expose headers only when frontend needs them.
7. Keep local development origins separate from production origins.
8. Do not disable security only to fix CORS.

---

## 11. Product, Variant, SKU Rules

A parent product is not the same as a sellable SKU.

Use this concept:

- `product`: parent shoe model.
- `product_variant`: sellable SKU by size and color.

Example:

- Product: "Nike Air Force 1 White"
- Variants:
  - SKU `AF1-WHITE-EU40`
  - SKU `AF1-WHITE-EU41`
  - SKU `AF1-BLACK-EU40`

Recommended catalog tables:

- `brand`
- `category`
- `collection`
- `product`
- `product_variant`
- `product_image`
- `product_review`

Variant fields may include:

- `sku`
- `size`
- `size_system` such as `EU`, `US`, `UK`
- `color`
- `price`
- `sale_price`
- `barcode`
- `status`
- `weight`

Rules:

1. Customer must select a variant before adding to cart.
2. Inventory must be checked by variant, not only by product.
3. Product images can belong to product or variant.
4. Color-specific images should be associated with variants when possible.
5. SKU must be unique.
6. Product slug should be unique.
7. Product detail should show available sizes, colors, price, sale price, stock status, material, brand, and return policy.
8. Product review should only be allowed for users who purchased the product if review feature is implemented.

---

## 12. Pricing & Promotion Rules

Shoe pricing may include fixed price, sale price, voucher discount, and shipping fee.

Rules:

1. Cart price is not permanently frozen.
2. Product price must be recalculated before checkout.
3. Order item price must be frozen after order creation.
4. Voucher discount must be frozen in order snapshot.
5. Shipping fee from GHN must be frozen in order snapshot.
6. Discount must not make final price negative.
7. Do not calculate final accounting data only from current product price because product price can change after order creation.

---

## 13. Inventory Rules

Inventory is critical.

Rules:

1. Inventory must be tracked by `product_variant` and `warehouse` or `store`.
2. Avoid overselling.
3. Use transaction-safe logic when locking or deducting inventory.
4. Consider pessimistic locking with `@Lock(PESSIMISTIC_WRITE)` for checkout inventory rows.
5. Optimistic locking with `@Version` may be used where suitable.
6. Adding to cart must not deduct inventory.
7. Creating order may temporarily lock inventory.
8. Successful payment deducts inventory permanently.
9. Failed, cancelled, or expired payment releases locked inventory.
10. Return can increase inventory depending on business rule.
11. Every inventory change should be recorded in `inventory_transaction`.
12. Inventory adjustment must require staff/admin/warehouse permission.

Inventory transaction types:

- `IMPORT`
- `LOCK`
- `RELEASE`
- `DEDUCT`
- `RETURN`
- `ADJUST`

Recommended inventory fields:

- `quantity_on_hand`
- `quantity_locked`
- `quantity_available`

`quantity_available` should be derived carefully:

```text
quantity_available = quantity_on_hand - quantity_locked
```

---

## 14. Cart Rules

Rules:

1. One user should have one active cart.
2. Cart item references `product_variant`.
3. Cart does not freeze price.
4. Cart total must be recalculated when opening cart or checkout.
5. If variant is out of stock, checkout must be blocked.
6. Quantity must be validated against available inventory.
7. User must select size and color before adding to cart.
8. Cart item should not reference only `product`.

---

## 15. Order Rules

Order state machine is important.

Possible order statuses:

- `PENDING_PAYMENT`
- `PAYMENT_FAILED`
- `PAID`
- `CONFIRMED`
- `PACKING`
- `READY_TO_SHIP`
- `SHIPPING`
- `DELIVERED`
- `COMPLETED`
- `CANCELLED`
- `EXPIRED`
- `RETURN_REQUESTED`
- `RETURNED`
- `REFUNDED`

Rules:

1. Do not update order status randomly.
2. Add status history when order status changes.
3. Use service methods for status transitions.
4. Keep order snapshot data immutable after creation unless there is a clear business process.
5. Do not delete orders in normal business flow.
6. Use cancellation or expiration statuses instead.
7. Customer can only access their own orders.
8. Admin/staff can access orders according to permission.
9. Order item must store product and variant snapshots.
10. Order must store shipping address snapshot.
11. Order must store shipping fee snapshot.
12. Order must store voucher/discount snapshot if used.

Order item snapshot fields should include:

- `product_id`
- `variant_id`
- `sku`
- `product_name`
- `brand_name`
- `size`
- `size_system`
- `color`
- `unit_price`
- `discount_amount`
- `quantity`
- `line_total`

---

## 16. VNPAY Payment Rules

Payment methods may include:

- `VNPAY`
- `COD`

Payment statuses:

- `PENDING`
- `SUCCESS`
- `FAILED`
- `REFUNDED`

Rules:

1. Backend creates VNPAY payment URL.
2. Backend must validate VNPAY callback/IPN signature.
3. Do not trust payment status sent directly from frontend.
4. `vnp_TxnRef` or an internal `idempotency_key` must be unique.
5. Callback/IPN must not process the same transaction twice.
6. Store provider transaction ID.
7. Store raw callback/IPN payload in `payment_webhook_log` if needed for debugging.
8. Do not store raw card data, CVV, or sensitive payment information.
9. Payment success should update order status through a service method.
10. Payment failure should not corrupt inventory state.
11. Payment failed, cancelled, or expired should release locked inventory when applicable.
12. Duplicate VNPAY callback must return a safe response without duplicate order/payment mutation.

Recommended tables:

- `payment`
- `payment_webhook_log`

---

## 17. GHN Shipping Rules

Shipping provider:

- GHN

Recommended shipping tables:

- `shipment`
- `shipment_status_history`
- `shipping_provider_log`

Rules:

1. GHN integration must be isolated in `GhnService` or a shipping provider adapter.
2. Controllers must not directly call GHN APIs.
3. Shipping fee should be calculated before order creation or checkout confirmation.
4. Shipping fee must be frozen in the order snapshot.
5. GHN order code must be stored when shipment is created.
6. GHN status changes should update shipment status through service methods.
7. If GHN API fails, return a controlled business error for frontend.
8. Do not expose raw GHN errors directly to customer-facing APIs.
9. Province, district, and ward data may be cached.
10. Store raw GHN request/response only if useful for debugging and do not store secrets.

Recommended shipment statuses:

- `PENDING`
- `CREATED`
- `PICKING`
- `SHIPPING`
- `DELIVERED`
- `RETURNING`
- `RETURNED`
- `CANCELLED`
- `FAILED`

---

## 18. Voucher & Promotion Rules

Rules:

1. Validate voucher before applying.
2. Check voucher active status.
3. Check start and end time.
4. Check minimum order amount.
5. Check usage limit.
6. Check applicable categories/brands/products if configured.
7. Record voucher usage after order is successfully created or paid depending on business rule.
8. Avoid applying the same voucher multiple times incorrectly.
9. Discount must not make order total negative.

---

## 19. Return / Exchange Rules

Shoe stores often need return or exchange because of size issues.

Rules:

1. Return/exchange request should reference the original order item.
2. Customer can request return/exchange only for their own completed or delivered orders.
3. Admin/staff approves or rejects request.
4. Returning item can increase inventory only after inspection or approval.
5. Refund should be linked to payment and return request.
6. Loyalty points or voucher usage may need adjustment after refund.

Possible return reasons:

- `WRONG_SIZE`
- `DEFECTIVE_PRODUCT`
- `WRONG_ITEM`
- `CUSTOMER_CHANGED_MIND`
- `OTHER`

---

## 20. Validation Rules

Use Bean Validation where appropriate.

Examples:

```java
@NotBlank
private String email;

@Email
private String email;

@NotNull
private BigDecimal price;

@Min(1)
private Integer quantity;
```

Rules:

1. Validate request DTOs.
2. Do not validate only in frontend.
3. Return consistent error responses.
4. Keep validation messages understandable.
5. Validate IDs, quantity, shipping address, phone, and voucher code.
6. Validate size/color selection before cart or checkout.

---

## 21. Exception Handling Rules

Use centralized exception handling.

Recommended classes:

- `AppException`
- `ErrorCode`
- `GlobalExceptionHandler`
- `FieldErrorResponse`

Rules:

1. Do not throw generic `RuntimeException` for expected business errors.
2. Use meaningful error codes.
3. Do not expose stack traces to users.
4. Log server-side errors.
5. Keep API error response consistent.
6. Convert provider errors from GHN/VNPAY to internal error responses.
7. Handle validation errors consistently.
8. Handle authentication and authorization errors consistently.

Recommended business errors:

- `USER_NOT_FOUND`
- `EMAIL_ALREADY_EXISTS`
- `PHONE_ALREADY_EXISTS`
- `INVALID_CREDENTIALS`
- `ACCOUNT_BANNED`
- `PRODUCT_NOT_FOUND`
- `VARIANT_NOT_FOUND`
- `OUT_OF_STOCK`
- `INSUFFICIENT_STOCK`
- `ORDER_NOT_FOUND`
- `INVALID_ORDER_STATUS`
- `PAYMENT_NOT_FOUND`
- `PAYMENT_ALREADY_PROCESSED`
- `INVALID_PAYMENT_SIGNATURE`
- `SHIPPING_PROVIDER_ERROR`
- `VOUCHER_INVALID`
- `ACCESS_DENIED`

---

## 22. Entity Rules

When creating JPA entities:

1. Use `@Entity`.
2. Use `@Table`.
3. Use `@Id`.
4. Use clear column names.
5. Use enum fields carefully.
6. Prefer `@Enumerated(EnumType.STRING)` for enums.
7. Avoid exposing entities directly in API responses.
8. Avoid lazy loading issues in response mapping.
9. Be careful with bidirectional relationships.
10. Do not use Lombok `@Data` blindly on JPA entities if it causes recursive `toString`, `equals`, or `hashCode`.

Preferred Lombok pattern for entities:

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "example")
public class Example {
}
```

Be careful with:

```java
@Data
@Entity
public class Example {
}
```

---

## 23. DTO & Mapper Rules

Rules:

1. Request DTOs should not contain fields controlled by server such as `id`, `createdAt`, `updatedAt`, unless needed.
2. Response DTOs should not expose password, token secrets, internal provider secrets, or sensitive fields.
3. Use mapper classes or MapStruct if the project already uses it.
4. Do not map complex business logic inside controllers.
5. Product response should include variants in a frontend-friendly structure.
6. Cart/order response should include enough snapshot data so FE does not need extra calls for basic display.

---

## 24. Testing Rules

When asked to write tests:

1. Prefer service-level unit tests for business logic.
2. Prefer controller tests for API behavior.
3. For inventory, include concurrency-related test scenarios when possible.
4. For payment callback/IPN, test duplicate callback/idempotency.
5. For order state machine, test invalid transitions.
6. For voucher, test expired voucher, minimum order, usage limit, and invalid category.
7. For GHN integration, mock external API calls.
8. For VNPAY integration, mock signature and callback payloads.
9. Do not create meaningless tests that only check object construction.

Required mock test scenarios:

- Register with duplicate email should fail.
- Login with wrong password should fail.
- Banned user cannot authenticate.
- Add to cart without selecting variant should fail.
- Add quantity greater than available stock should fail.
- Create order successfully locks inventory.
- Create order out of stock should fail.
- Two concurrent checkout requests for the same low-stock variant should not oversell.
- Payment success deducts locked inventory.
- Payment failure releases locked inventory.
- Duplicate VNPAY callback is processed only once.
- Invalid VNPAY signature is rejected.
- GHN shipping fee failure returns controlled error.
- Validation error returns field-level error response.
- Customer cannot access another customer's order.
- Admin/staff access requires proper role or permission.

---

## 25. Security Rules

1. Never commit secrets.
2. Never log JWT tokens.
3. Never log OTP values in production-level code.
4. Never expose password hash in API response.
5. Enforce authorization on admin endpoints.
6. Validate ownership for customer resources.
7. Use HTTPS in deployment assumptions.
8. Do not store raw payment card data.
9. Sanitize file uploads if product image upload is added.
10. Restrict CORS properly.
11. Validate and sanitize callback data from external providers.
12. Do not trust frontend for price, discount, shipping fee, payment status, or user identity.

---

## 26. Frontend Rules

If editing frontend code:

1. Use component-based structure.
2. Use TailwindCSS.
3. Keep mobile-first layout.
4. Product listing should support filters.
5. Product detail must require size and color selection before add to cart.
6. Cart must refresh price before checkout.
7. Checkout must require login.
8. If user has no phone number, require phone update/verification before payment.
9. Admin UI should not be mixed with customer UI without route protection.
10. Keep API error messages user-friendly.
11. FE should not calculate trusted final price.
12. FE should use backend order/payment/shipping responses as source of truth.

Recommended filters:

- brand
- category
- size
- color
- price range
- gender
- material
- sale
- availability

---

## 27. Naming Rules

Use English names in code.

Recommended naming examples:

- `UserAccount`
- `Product`
- `ProductVariant`
- `Brand`
- `Category`
- `Inventory`
- `InventoryTransaction`
- `Cart`
- `CartItem`
- `Order`
- `OrderItem`
- `Payment`
- `PaymentWebhookLog`
- `Shipment`
- `Voucher`
- `ReturnRequest`

Database table names can use snake_case:

- `user_account`
- `product_variant`
- `inventory_transaction`
- `order_item`
- `payment_webhook_log`
- `shipment_status_history`

Enums should use uppercase snake case:

```java
ACTIVE
INACTIVE
BANNED
PENDING_PAYMENT
READY_TO_SHIP
PAYMENT_FAILED
WRONG_SIZE
```

---

## 28. Things Codex Must Not Do

Do not:

1. Rewrite the whole codebase without instruction.
2. Replace Spring Security with another auth framework.
3. Remove existing JWT/OAuth2 flow without asking.
4. Return JPA entities directly from controllers.
5. Store passwords in plain text.
6. Store card/CVV data.
7. Skip inventory locking in checkout.
8. Process VNPAY callback without idempotency.
9. Add guest checkout.
10. Assume product price is stable forever.
11. Remove order snapshot fields.
12. Merge product and variant into one concept if variants are required.
13. Ignore phone uniqueness.
14. Ignore email uniqueness.
15. Add excessive abstractions that make the student project hard to explain.
16. Add dependencies without a clear reason.
17. Change package names globally unless requested.
18. Break existing tests or API contracts silently.
19. Hardcode GHN or VNPAY secrets.
20. Disable CORS/security globally just to make frontend calls work.

---

## 29. Preferred Workflow For Codex

For every task:

1. Inspect relevant files first.
2. Identify existing naming and style.
3. Make the smallest safe change.
4. Update related DTOs/services/repositories/controllers if needed.
5. Add or update tests if the task affects logic.
6. Run available tests or at least mention which tests should be run.
7. Summarize:
   - What changed
   - Files changed
   - Tests run
   - Any risk or follow-up needed

---

## 30. Suggested ERD Modules

The database should be modular and expandable.

Important modules:

### Auth

- `user_account`
- `role`
- `permission`
- `user_role`
- `role_permission`
- `email_verify_token`
- `user_provider`

### Customer

- `customer_profile`
- `customer_address`

### Catalog

- `brand`
- `category`
- `collection`
- `product`
- `product_variant`
- `product_image`
- `product_review`

### Inventory

- `warehouse`
- `store`
- `inventory`
- `inventory_transaction`

### Cart & Wishlist

- `cart`
- `cart_item`
- `wishlist`

### Voucher

- `voucher`
- `voucher_category`
- `voucher_brand`
- `voucher_usage`

### Order

- `orders`
- `order_item`
- `order_status_history`

### Payment

- `payment`
- `payment_webhook_log`

### Shipping

- `shipment`
- `shipment_status_history`
- `shipping_provider_log`

### Return / Refund

- `return_request`
- `return_request_item`
- `refund`

### Admin / Audit

- `notification_log`
- `audit_log`

---

## 31. Audit Log Rules

Admin actions that should be audited:

- Product price update
- Product variant update
- Inventory adjustment
- Refund approval
- Return/exchange approval
- Role or permission update
- Order manual status update
- Voucher configuration update

Rules:

1. Store actor user ID.
2. Store action name.
3. Store target table and target ID.
4. Store old value and new value when useful.
5. Do not store sensitive secrets in audit log.

---

## 32. Current Project Notes

The project is expected to evolve module by module.

Recommended implementation order:

1. Authentication and authorization
2. Product catalog
3. Product variants by size/color
4. Inventory by warehouse/store
5. Cart
6. Order
7. VNPAY payment
8. GHN shipping
9. Voucher
10. Return/exchange/refund
11. Audit log

Future modules should integrate with the auth model instead of replacing it.

---

## 33. Language Preference

The user usually discusses requirements in Vietnamese.

When explaining changes to the user, use Vietnamese unless the user asks otherwise.

Code, class names, method names, database names, and comments may remain in English for consistency.
