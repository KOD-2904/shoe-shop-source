# Shoe Shop Production Audit

Audit date: 2026-09-08  
Scope: BE + FE implementation review. No code changes were made during this audit.

## Post-Audit Implementation Status

Updated: 2026-09-08

### PHASE 1 - Must Fix: DONE

- [DONE] Locked `DELETE /users/delete` behind `ROLE_ADMIN`.
- [DONE] Added GHN webhook shared-secret verification with `GHN_WEBHOOK_SECRET`.
- [DONE] Added pessimistic order/payment locking around VNPay IPN handling.
- [DONE] Added locked order lookup before GHN order creation to prevent duplicate handoff.
- [DONE] Rejected unsupported payment methods outside COD/VNPAY.
- [DONE] Allowed public storefront brand/category reads while keeping write/admin operations protected.
- [DONE] Registered refresh-token rate limit filter.

### PHASE 2 - Core E-commerce Completion: DONE

- [DONE] COD pending payment/order timeout now releases locked stock.
- [DONE] Voucher usage now uses locked updates plus `voucher_usages` ledger.
- [DONE] Cart, checkout, buy-now, and order creation reject inactive product/variant records.
- [DONE] Product/category/brand update and filtering methods are implemented with validation.
- [DONE] Product variant update is implemented with safe size upsert behavior.
- [DONE] Address delete and default-address row locking are implemented.

### PHASE 3 - Advanced Features: DONE

- [DONE] Permission entity and Role-Permission mapping added; authorities now include permission codes.
- [DONE] Backend OpenAI shop chatbot endpoint added with fallback mode when API key is absent/disabled.
- [DONE] Product/category/variant-size discount system added and wired into catalog, cart, checkout, and order pricing.
- [DONE] Admin product discount UI added.
- [DONE] Review images and order-item linked reviews added.
- [DONE] Delivered order detail now links each item to its order-item scoped review flow.
- [DONE] GHN polling reconciliation added behind `GHN_POLLING_ENABLED`.

### Remaining Production Debt

- [DONE] `CustomUserDetails` now blocks inactive, unverified, and banned accounts during JWT authentication.
- [DONE] JWT authentication entry point now returns 401 `UNAUTHENTICATED` instead of 403 authorization errors.
- [DONE] Added Flyway dependencies, opt-in local config, production Flyway config, and first MySQL migration for post-audit schema extensions.
- [DONE] Added production Docker stack for MySQL, Redis, backend, and frontend with Dockerfiles, healthchecks, and env example.
- [DONE] Production startup validation now blocks default JWT/admin/demo credentials and enabled init when `APP_ENV=prod|production` or a production profile is active.
- [DONE] Added paged order APIs plus eager order-item fetch and batch display-payment lookup for user/admin order lists.
- [DONE] Added shared external HTTP connect/read timeout configuration and wired it into active GHN, Google OAuth, OpenAI, and VNPay refund clients.
- [DONE] Added Spring integration test for concurrent buy-now checkout inventory locking.
- [DONE] Added Spring integration tests for GHN webhook secret verification and delivered COD status updates.
- [DONE] Added Spring integration test for duplicate VNPay IPN idempotency and locked-inventory deduction behavior.
- [DONE] Added Spring integration tests for voucher ledger idempotency/usage limit and delivered order-item review ownership/update behavior.
- [DONE] Removed legacy `user_id + product_id` product-review uniqueness so separate delivered order items can each be reviewed.

## 1. Project Health Summary

| Area | Score | Notes |
| --- | ---: | --- |
| Architecture | 6.5/10 | Module split is usable, but public/admin API boundaries and state machines need cleanup. |
| Security | 5/10 | JWT/Redis flow exists, but destructive endpoint, webhook verification, default secrets, and token invalidation gaps are high risk. |
| Database | 5.5/10 | Entities are functional, but migration strategy, indexes, FK consistency, and cascade policy are not production-ready. |
| Business logic | 6/10 | Checkout/order/payment/shipping flow exists, but race conditions and edge states remain. |
| Error handling | 5.5/10 | `AppException` exists, but several paths still return null, throw generic exceptions, or map errors incorrectly. |
| Production readiness | 4.5/10 | Docker, migrations, tests, timeout/retry, observability, and secret validation are incomplete. |

## 2. Completed Features

- Authentication: register, password hash, email verification, login, Google login, JWT access token, refresh token, Redis token storage, refresh rotation, logout, logout all devices.
- Authorization: role-based access with `ROLE_USER`, `ROLE_ADMIN`, `ROLE_STAFF`.
- Catalog: product, brand, category, product variant, variant size, product image, variant image basics.
- Inventory: stock, locked quantity, optimistic version, pessimistic lock during reservation/deduct/release.
- Cart: get cart, add item, update quantity, remove item, clear cart.
- Checkout: checkout preview, shipping fee snapshot, cart signature validation, voucher integration.
- Order: cart order, buy-now order, order history, COD flow, VNPay flow.
- Payment: VNPay URL generation, callback inspect, IPN processing, expiry handling, basic refund service.
- Shipping: GHN fee, GHN order creation, GHN mock/real mode, webhook handling.
- Address: add, list, update, set default.
- Promotion: voucher fixed/percent, min order, max discount, usage limit, active date window.
- Review: product reviews with purchased-and-delivered validation.
- FE: storefront, PDP, cart, checkout, address, orders, wishlist, account, admin pages, local rule-based chatbot.

## 3. Partially Completed Features

### Finding P1

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/service/impl/ProductServiceImpl.java`  
CLASS: `ProductServiceImpl`  
METHOD: `updateProduct`, `getProductsByCategory`, `getProductsByBrand`  
ISSUE: Methods are present but return `null` or empty lists.  
SEVERITY: HIGH  
WHY: API consumers can receive broken responses or believe filtering exists when it does not.  
SUGGESTED FIX: Implement the methods fully with validation, active filtering, pagination, and mapper-safe responses.

### Finding P2

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/service/impl/ProductVariantServiceImpl.java`  
CLASS: `ProductVariantServiceImpl`  
METHOD: `updateProductVariant`  
ISSUE: Method returns `null`.  
SEVERITY: HIGH  
WHY: Any endpoint using this method will produce broken API behavior.  
SUGGESTED FIX: Implement variant update with size reconciliation, SKU uniqueness validation, inventory handling, and transaction boundary.

### Finding P3

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/controller/catalog/ProductController.java`  
CLASS: `ProductController`  
METHOD: `addProductImage`  
ISSUE: Builds a result but returns `null`.  
SEVERITY: HIGH  
WHY: Upload can succeed while client receives an invalid response.  
SUGGESTED FIX: Return `ApiResponse<ProductResponse>` with the mapped result.

### Finding P4

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/config/OpenAiConfig.java`, `FE/src/components/ShopChatbot.tsx`  
CLASS: `OpenAiConfig`, `ShopChatbot`  
METHOD: N/A  
ISSUE: OpenAI properties exist, but chatbot is only client-side rule/ranking based.  
SEVERITY: MEDIUM  
WHY: The feature does not use the configured OpenAI API key and cannot reason beyond local catalog heuristics.  
SUGGESTED FIX: Add backend chatbot API with OpenAI integration, catalog context, user-scoped cart/order context, rate limiting, and safe response shaping.

### Finding P5

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/controller/catalog/BrandController.java`, `CategoryController.java`, `FE/src/api/catalogApi.ts`  
CLASS: `BrandController`, `CategoryController`, `catalogApi`  
METHOD: `getBrands`, `getCategorys`, `brands`, `categories`  
ISSUE: FE storefront calls brand/category list endpoints that BE protects with `ROLE_ADMIN`.  
SEVERITY: HIGH  
WHY: Public storefront can fail to load brand/category merchandising for anonymous or normal users.  
SUGGESTED FIX: Split public catalog endpoints from admin management endpoints.

### Finding P6

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/service/impl/ProductReviewServiceImpl.java`  
CLASS: `ProductReviewServiceImpl`  
METHOD: `createReview`  
ISSUE: Review is unique by user/product, not by delivered order item.  
SEVERITY: MEDIUM  
WHY: Requirement says one order item should be reviewed once. Current logic does not model order-item ownership.  
SUGGESTED FIX: Add `order_item_id` to review and unique constraint on that field.

## 4. Missing Features

- Permission model: NOT IMPLEMENTED. No `Permission` entity, no Role-Permission mapping, no fine-grained `hasAuthority` beyond role strings.
- Phone OTP: NOT IMPLEMENTED.
- Staff management flow: PARTIALLY IMPLEMENTED. `ROLE_STAFF` exists, but staff lifecycle and endpoint permissions are not complete.
- Soft delete lifecycle for product, variant, brand, category: PARTIALLY IMPLEMENTED.
- Product, variant, category, and campaign-level discount: NOT IMPLEMENTED. Voucher exists only at order level.
- Review image, review moderation, helpful votes, rating aggregate cache: NOT IMPLEMENTED.
- GHN polling or reconciliation job: NOT IMPLEMENTED.
- Flyway/Liquibase: NOT IMPLEMENTED. SQL scripts are stored in `BE/docs`.
- Full Docker production stack: NOT IMPLEMENTED. Current compose only starts Redis.
- Production monitoring, metrics, structured request logging, external API circuit breaker: NOT IMPLEMENTED.

## 5. Critical Problems

### Finding C1

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/controller/auth/UserController.java`, `BE/src/main/java/com/ttthinh/shoe_shop_basic/service/impl/auth/UserServiceImpl.java`  
CLASS: `UserController`, `UserServiceImpl`  
METHOD: `deleteAllUser`, `deleteAllUsers`  
ISSUE: Any authenticated user can call `DELETE /users/delete` and delete all users.  
SEVERITY: CRITICAL  
WHY: Controller method and service method have no `@PreAuthorize`. `SecurityConfig` only requires authentication for non-public endpoints.  
SUGGESTED FIX: Remove this endpoint or restrict it with `@PreAuthorize("hasRole('ADMIN')")`; add integration test proving normal user receives 403.

### Finding C2

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/controller/shipping/GHNWebhookController.java`  
CLASS: `GHNWebhookController`  
METHOD: `handle`  
ISSUE: GHN webhook is public and unauthenticated.  
SEVERITY: CRITICAL  
WHY: Anyone can POST fake shipping status payloads and potentially mark orders delivered, failed, returned, or cancelled.  
SUGGESTED FIX: Verify GHN webhook signature/shared secret, reject unsigned payloads, and make webhook idempotent.

### Finding C3

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/service/impl/payment/PaymentApplicationServiceImpl.java`  
CLASS: `PaymentApplicationServiceImpl`  
METHOD: `handleVNPayIpn`  
ISSUE: VNPay duplicate IPN handling is not protected against parallel requests.  
SEVERITY: HIGH  
WHY: Two IPNs can read `UNPAID` before either commits. Inventory/order/payment updates can be applied inconsistently.  
SUGGESTED FIX: Lock payment/order row with `PESSIMISTIC_WRITE`, add unique transaction constraint, and process status transitions idempotently.

### Finding C4

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/service/shipping/ShippingOrderService.java`  
CLASS: `ShippingOrderService`  
METHOD: `createGHNOrder`  
ISSUE: Concurrent admin actions can create more than one GHN order for the same internal order.  
SEVERITY: HIGH  
WHY: Method checks state then calls GHN without locking the order row.  
SUGGESTED FIX: Add locked repository read or conditional update before external call; store idempotency marker.

### Finding C5

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/security/auth/RefreshTokenRateLimitFilter.java`, `BE/src/main/java/com/ttthinh/shoe_shop_basic/config/SecurityConfig.java`  
CLASS: `RefreshTokenRateLimitFilter`, `SecurityConfig`  
METHOD: `securityFilterChain`  
ISSUE: Refresh rate-limit filter exists but is not registered.  
SEVERITY: HIGH  
WHY: Refresh endpoint can be abused without the intended Redis rate limit.  
SUGGESTED FIX: Add the filter to the Spring Security chain before refresh token processing.

## 6. Security Problems

### Finding S1

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/security/user/CustomUserDetails.java`  
CLASS: `CustomUserDetails`  
METHOD: `isEnabled`  
ISSUE: Always returns `true`.  
SEVERITY: HIGH  
WHY: Deactivated or unverified accounts can continue using existing access tokens until expiration.  
SUGGESTED FIX: Return status/email verification flags and re-check account state in JWT authentication.

### Finding S2

FILE: `BE/src/main/resources/application.properties`  
CLASS: N/A  
METHOD: N/A  
ISSUE: Default JWT secret, default admin/demo credentials, and `app.init.enabled=true` are production-dangerous.  
SEVERITY: HIGH  
WHY: A production boot with missing env vars can create predictable accounts and weak token signing.  
SUGGESTED FIX: Add production startup validation and set production init disabled by default.

### Finding S3

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/service/impl/auth/AuthServiceImpl.java`  
CLASS: `AuthServiceImpl`  
METHOD: `createAuthResponse`, `refreshToken`  
ISSUE: Device binding uses request IP as `deviceId`.  
SEVERITY: MEDIUM  
WHY: IP changes can break sessions; NAT/proxy users can share the same identifier.  
SUGGESTED FIX: Use a client-generated device ID or server session ID stored in secure cookie.

### Finding S4

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/service/impl/auth/UserServiceImpl.java`  
CLASS: `UserServiceImpl`  
METHOD: `register`  
ISSUE: Email verification URL is hardcoded to localhost.  
SEVERITY: MEDIUM  
WHY: Production email verification links will be wrong unless code is changed.  
SUGGESTED FIX: Use `app.public-domain` or configured frontend verification URL.

### Finding S5

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/config/JwtAuthenticationEntryPoint.java`  
CLASS: `JwtAuthenticationEntryPoint`  
METHOD: `commence`  
ISSUE: Unauthenticated errors are mapped through an authorization-style error code.  
SEVERITY: MEDIUM  
WHY: FE and API clients may not handle 401/403 semantics correctly.  
SUGGESTED FIX: Return a consistent 401 `UNAUTHENTICATED` response.

## 7. Database Problems

### Finding D1

FILE: `BE/src/main/resources/application.properties`, `BE/docs/*.sql`  
CLASS: N/A  
METHOD: N/A  
ISSUE: Hibernate `ddl-auto=update` is default and SQL migrations are manual docs.  
SEVERITY: HIGH  
WHY: Schema drift and unreviewed production mutations are likely.  
SUGGESTED FIX: Adopt Flyway or Liquibase and use `ddl-auto=validate` in production.

### Finding D2

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/entity/customer/Address.java`  
CLASS: `Address`  
METHOD: N/A  
ISSUE: Address stores `userId` as plain string instead of FK relationship.  
SEVERITY: MEDIUM  
WHY: Database cannot enforce address ownership integrity.  
SUGGESTED FIX: Use `@ManyToOne UserAccount` or add explicit FK constraint.

### Finding D3

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/entity/order/Order.java`, `OrderRepository.java`  
CLASS: `Order`, `OrderRepository`  
METHOD: `findByShippingOrderCode`  
ISSUE: `shippingOrderCode` is queried but not indexed or unique.  
SEVERITY: MEDIUM  
WHY: Webhook lookup can become slow and ambiguous.  
SUGGESTED FIX: Add unique index on `shipping_order_code` where applicable.

### Finding D4

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/entity/catalog/Product.java`  
CLASS: `Product`  
METHOD: N/A  
ISSUE: `CascadeType.ALL` and `orphanRemoval=true` on product variants/images can be dangerous.  
SEVERITY: HIGH  
WHY: Hard delete or orphan operations can conflict with historical order items and inventory.  
SUGGESTED FIX: Prefer soft delete/status transitions for sellable catalog records.

### Finding D5

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/entity/catalog/Product.java`  
CLASS: `Product`  
METHOD: N/A  
ISSUE: Builder defaults are missing for some initialized fields/collections.  
SEVERITY: MEDIUM  
WHY: Lombok builder can produce null collections despite field initializers.  
SUGGESTED FIX: Add `@Builder.Default` for initialized collections and default enum fields.

## 8. Business Logic Problems

### Finding B1

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/service/impl/CartServiceImpl.java`  
CLASS: `CartServiceImpl`  
METHOD: `addItem`, `updateItem`  
ISSUE: Cart validates inventory but not product/variant active status.  
SEVERITY: HIGH  
WHY: User can buy inactive products if they know a variant size ID.  
SUGGESTED FIX: Reject inactive product, inactive variant, deleted catalog, and unavailable size in cart and checkout.

### Finding B2

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/enums/PaymentMethod.java`, order creation services  
CLASS: Order/payment services  
METHOD: create order methods  
ISSUE: Enum includes methods such as `MOMO` and `STRIPE`, but only COD/VNPay are implemented.  
SEVERITY: HIGH  
WHY: Unsupported payment method can create an order/payment with no completion path.  
SUGGESTED FIX: Validate allowed methods or implement the missing providers.

### Finding B3

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/service/impl/OrderServiceImpl.java`  
CLASS: `OrderServiceImpl`  
METHOD: COD create/confirm/cancel flow  
ISSUE: COD `PENDING` orders lock inventory indefinitely.  
SEVERITY: HIGH  
WHY: Stock can remain unavailable forever if the user never pays or admin never cancels/confirms.  
SUGGESTED FIX: Add pending-order expiry scheduler and release locked stock.

### Finding B4

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/service/impl/VoucherServiceImpl.java`  
CLASS: `VoucherServiceImpl`  
METHOD: `markUsed`  
ISSUE: Voucher usage limit update is not concurrency-safe.  
SEVERITY: HIGH  
WHY: Parallel checkouts can exceed `usageLimit`.  
SUGGESTED FIX: Use pessimistic lock or atomic conditional update and add a voucher usage ledger.

### Finding B5

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/service/AddressService.java`  
CLASS: `AddressService`  
METHOD: `setDefaultAddress`, `changeDefaultAddress`  
ISSUE: Concurrent default address changes can leave more than one default.  
SEVERITY: MEDIUM  
WHY: Code clears then sets without user-level lock or database uniqueness.  
SUGGESTED FIX: Lock user address rows during default update or enforce one default through database strategy.

### Finding B6

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/service/impl/payment/PaymentServiceImpl.java`  
CLASS: `PaymentServiceImpl`  
METHOD: `updatePaymentStatus`  
ISSUE: Invalid transition throws `IllegalStateException`.  
SEVERITY: MEDIUM  
WHY: Global handler turns it into generic 500 instead of business error.  
SUGGESTED FIX: Throw `AppException` with a dedicated payment transition error.

### Finding B7

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/service/impl/OrderServiceImpl.java`, `PaymentRepository.java`  
CLASS: `OrderServiceImpl`, `PaymentRepository`  
METHOD: `getAllOrders`, `getMyOrders`, display payment lookup  
ISSUE: N+1 payment queries and no pagination.  
SEVERITY: HIGH  
WHY: Admin/user order pages will degrade quickly with real data.  
SUGGESTED FIX: Add pageable endpoints and fetch payment summaries in batch/projection.

### Finding B8

FILE: `BE/src/main/java/com/ttthinh/shoe_shop_basic/service/shipping/GHNShippingService.java`  
CLASS: `GHNShippingService`  
METHOD: GHN external calls  
ISSUE: External API calls use `RestTemplate` without explicit timeout/retry policy.  
SEVERITY: HIGH  
WHY: GHN latency can block request threads and degrade checkout/admin operations.  
SUGGESTED FIX: Configure connect/read timeouts, retries for safe calls, and clear fallback behavior.

## 9. Refactor Recommendations

- Split public storefront APIs from admin APIs: `/api/products` versus `/api/admin/products`.
- Normalize REST naming: replace `/getProducts`, `/getCategorys`, `/add` style endpoints over time.
- Add explicit order/payment/shipping state machine service.
- Add repository lock methods for order, payment, voucher, and possibly address default operations.
- Standardize `ApiResponse`, `ErrorCode`, and HTTP status mapping.
- Replace manual SQL docs with versioned migrations.
- Add DTO validation consistently and ensure controllers use `@Valid`.
- Add integration tests for security, checkout, inventory concurrency, VNPay duplicate IPN, and GHN webhook.
- Add query projections/pageable responses for admin tables.
- Keep FE admin route checks, but never rely on FE for authorization.

## 10. Recommended Development Roadmap

### PHASE 1 - Must Fix

- [CRITICAL] Remove or lock down `DELETE /users/delete`. Dependency: none.
- [CRITICAL] Add GHN webhook authentication/signature verification. Dependency: GHN webhook secret/config.
- [HIGH] Add pessimistic lock/idempotency for VNPay IPN. Dependency: repository lock methods.
- [HIGH] Add lock/idempotency for GHN order creation. Dependency: order lock method.
- [HIGH] Reject unsupported payment methods. Dependency: payment method policy.
- [HIGH] Split public/admin catalog endpoints so FE storefront can load brand/category safely. Dependency: SecurityConfig/controller updates.

### PHASE 2 - Core E-commerce Completion

- [HIGH] Add COD pending timeout and stock release scheduler. Dependency: order expiry policy.
- [HIGH] Make voucher usage concurrency-safe and add voucher usage ledger. Dependency: voucher schema migration.
- [HIGH] Enforce product/variant active validation in cart, checkout, and buy-now. Dependency: catalog status rules.
- [MEDIUM] Implement product/category/brand filtering and update methods fully. Dependency: repository queries and DTO validation.
- [MEDIUM] Add address delete and one-default-address enforcement. Dependency: schema/index strategy.

### PHASE 3 - Advanced Features

- [MEDIUM] Implement Permission entity and Role-Permission mapping. Dependency: authorization model decision.
- [MEDIUM] Implement OpenAI backend chatbot endpoint with catalog/order/cart context. Dependency: OpenAI API key and rate limit config.
- [MEDIUM] Add product, variant, category, and campaign promotions. Dependency: pricing calculation architecture.
- [MEDIUM] Add review images and order-item based review. Dependency: review schema migration.
- [LOW] Add GHN polling reconciliation. Dependency: scheduled job infrastructure.

### PHASE 4 - Production Ready

- [HIGH] Add Flyway/Liquibase and set prod `ddl-auto=validate`. Dependency: migration baseline.
- [HIGH] Build full Docker stack for BE, FE, MySQL, Redis, and env profiles. Dependency: production config.
- [HIGH] Add startup validation for required secrets. Dependency: profile detection.
- [MEDIUM] Add indexes, pagination, and fetch optimization for catalog/order/admin pages. Dependency: query audit.
- [MEDIUM] Add timeout/retry/circuit breaker for GHN, VNPay, Cloudinary, mail, and OpenAI. Dependency: HTTP client abstraction.
- [MEDIUM] Add structured logs, health checks, and production smoke tests. Dependency: deployment target.

## Business Logic Issues Intentionally Not Modified

- Destructive user delete endpoint is still present.
- GHN webhook is still public.
- VNPay IPN and GHN create order still need row-level idempotency protection.
- Voucher usage still has race condition.
- COD pending orders can still lock stock indefinitely.
- Public storefront still calls admin-protected brand/category endpoints.
- OpenAI chatbot configuration exists, but chatbot integration is not implemented server-side.
