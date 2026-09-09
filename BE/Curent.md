# Current Project Overview

Ngày rà soát: 2026-08-28

## 1. Tổng quan

Project hiện tại là backend Spring Boot cho hệ thống bán giày online. Backend đã có các module chính của một e-commerce flow: xác thực người dùng, quản lý catalog sản phẩm, giỏ hàng, checkout, đơn hàng, tồn kho, thanh toán VNPAY, vận chuyển GHN, upload ảnh Cloudinary và cấu hình Redis.

Package chính:

```text
com.ttthinh.shoe_shop_basic
```

Ứng dụng đang dùng kiến trúc phổ biến:

- `controller`: expose REST API.
- `service`: xử lý nghiệp vụ.
- `repository.jpa`: truy cập database qua Spring Data JPA.
- `entity`: model database.
- `dto.request` / `dto.response`: input/output API.
- `mapper`: MapStruct mapper giữa entity và DTO.
- `security`: JWT, user details, OAuth2, auth filters.
- `config`: cấu hình Spring Security, CORS, Redis, Cloudinary, GHN, VNPAY, async, init data.
- `exception`: exception tập trung và error code.

## 2. Tech stack hiện có

- Java 17.
- Spring Boot 4.0.0.
- Spring Web / WebMVC.
- Spring Security.
- JWT bằng `jjwt`.
- OAuth2 client, hiện có Google login.
- Spring Data JPA / Hibernate.
- MySQL cho runtime chính.
- H2 cho test scope.
- Redis cho refresh token, cache và rate limit.
- MapStruct.
- Lombok.
- Bean Validation.
- Spring Mail.
- Cloudinary SDK.
- VNPAY sandbox integration.
- GHN shipping API integration.
- Dotenv để load biến môi trường local.
- Maven wrapper.

## 3. Cấu hình và môi trường

Project có:

- `application.properties`: cấu hình app, database, JWT, CORS, Google OAuth, mail, Cloudinary, multipart upload, GHN, Redis, VNPAY.
- `.env.example`: mẫu biến môi trường.
- `.env`: file local có tồn tại trong workspace, không nên commit secret thật.
- `docker-compose.yml`: hiện chỉ khai báo service MySQL cơ bản.
- `application-test.properties`: cấu hình riêng cho test.

Các nhóm config quan trọng:

- Database: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JPA_DDL_AUTO`.
- JWT: `JWT_SECRET`, access token expiration, refresh token expiration.
- CORS: `ALLOWED_ORIGINS`.
- Google OAuth: client id, client secret, token URI, user info URI.
- Mail: SMTP host, port, username, password.
- Cloudinary: cloud name, API key, API secret, kích thước ảnh.
- GHN: token, shop id, địa chỉ kho gửi, mock mode, API URL.
- Redis: host, port, cache type.
- VNPAY: merchant code, hash secret, payment URL, return URL, IPN URL, refund URL.

## 4. Module authentication và user

Đã có:

- Đăng ký user bằng email/password.
- Đăng nhập local bằng identifier/password.
- Google login qua authorization code.
- Email verification token.
- JWT access token và refresh token.
- Refresh token lưu Redis.
- Revoke refresh token khi logout.
- Logout một thiết bị hoặc toàn bộ thiết bị.
- Kiểm tra provider để tránh login sai kiểu tài khoản.
- Trạng thái user: `ACTIVE`, `INACTIVE`, `BANNED`.
- Role-based authorization qua Spring Security và method security.
- Seed role/admin/demo user qua `ApplicationInitConfig`.
- Rate limit cho refresh token đã có class riêng.

Các entity liên quan:

- `UserAccount`
- `Role`
- `EmailVerifyToken`
- `RedisToken`

Các API chính:

- `POST /auth/login`
- `POST /auth/google`
- `POST /auth/log-out`
- `POST /auth/refreshToken`
- `GET /auth/verify-email`
- `POST /register`
- `GET /users`
- `GET /myinfor`
- `GET /inforbyid/{id}`
- `DELETE /users/delete`

## 5. Module địa chỉ khách hàng

Đã có quản lý địa chỉ giao hàng:

- Thêm địa chỉ.
- Lấy danh sách địa chỉ của user.
- Lấy API địa chỉ chuẩn hơn dưới `/api/addresses`.
- Thêm địa chỉ qua `/api/addresses`.
- Cập nhật địa chỉ.
- Set địa chỉ mặc định.
- Validate địa chỉ trong checkout: phải đúng user, có district id, ward code, receiver name, phone number.

Entity chính:

- `Address`

## 6. Module catalog

Đã có các phần chính để quản lý sản phẩm:

- Brand.
- Category, có parent-child category.
- Product.
- Product image.
- Product variant.
- Variant image.
- Variant size.
- Product status: `DRAFT`, `ACTIVE`, `INACTIVE`.

Product variant đang là SKU bán được. Cart, order và inventory đều bám vào `VariantSize`, không chỉ bám vào product cha.

Các API chính:

- Brand:
  - `GET /brand`
  - `POST /brand/add`
  - `POST /brand/addBrandImage`
  - `POST /brand/addBrandWithImage`
  - `GET /brand/getBrands`
  - `GET /brand/getBrand`
- Category:
  - `GET /category`
  - `GET /category/form-options`
  - `POST /category/add`
  - `GET /category/getCategorys`
  - `GET /category/getCategory`
- Product:
  - `GET /products`
  - `GET /products/form-options`
  - `POST /products/with-images`
  - `POST /products/`
  - `POST /products/addProductImage`
  - `GET /products/getProducts`
  - `GET /products/getProduct`
- Variant:
  - `GET /variants/form-options`
  - `GET /variants`
  - `GET /variants/product/{productId}`
  - `POST /variants/addVariant`
  - `POST /variants/addVariants`

## 7. Module ảnh và Cloudinary

Đã có:

- `CloudinaryConfig`.
- `CloudinaryService`.
- Upload ảnh sản phẩm.
- Upload ảnh variant.
- Upload logo brand.
- Queue upload ảnh sau khi transaction commit.
- Async executor riêng cho image upload.

Điểm thiết kế tốt: ảnh được snapshot từ `MultipartFile`, rồi upload sau commit để tránh upload khi transaction database rollback.

## 8. Module cart

Đã có:

- Cart theo user.
- Cart item gắn với `VariantSize`.
- Add item vào cart.
- Update quantity.
- Delete một item.
- Clear cart.
- Trả về cart response gồm danh sách item.

Entity chính:

- `Cart`
- `CartItem`

API chính:

- `GET /cart`
- `POST /cart/items`
- `PUT /cart/items/{cartItemId}`
- `DELETE /cart/items/{cartItemId}`
- `DELETE /cart`

## 9. Module checkout

Đã có:

- Preview checkout từ các cart item được chọn.
- Buy-now preview.
- Tính product total.
- Tính shipping fee qua GHN.
- Fallback shipping fee nếu GHN lỗi.
- Tạo `ShippingFeeSnapshot` để chống việc client sửa tiền ship/tổng tiền sau preview.
- Snapshot có TTL, mặc định 300 giây.
- Khi tạo order từ cart, hệ thống consume snapshot và kiểm tra:
  - Snapshot thuộc đúng user.
  - Snapshot chưa dùng.
  - Snapshot chưa hết hạn.
  - Address khớp.
  - Cart signature khớp.
  - Product total khớp.

Entity chính:

- `ShippingFeeSnapshot`

API chính:

- `POST /api/checkout/calculate-shipping`
- `POST /api/checkout/preview`
- `POST /api/checkout/buy-now-preview`

## 10. Module order

Đã có:

- Tạo order từ cart.
- Buy now.
- Lấy danh sách order của user.
- Lấy chi tiết order của user.
- User cancel order.
- Admin xem tất cả order.
- Admin xem chi tiết order.
- Admin cập nhật trạng thái order.
- Admin cancel order.
- Admin tạo vận đơn GHN.
- Order có thông tin người nhận, địa chỉ snapshot, phí ship, tổng hàng, giảm giá, tổng cuối.
- Khi order tạo từ cart, selected cart items bị xóa khỏi cart.

Order status hiện có:

- `PENDING`
- `CONFIRMED`
- `PACKING`
- `READY_TO_SHIP`
- `SHIPPING`
- `DELIVERED`
- `CANCELLED`
- `FAILED`
- `RETURNED`

Order status có method kiểm soát transition, user cancel và admin cancel.

Entity chính:

- `Order`
- `OrderItem`

API chính:

- User:
  - `POST /orders`
  - `POST /orders/buy-now`
  - `GET /orders/me`
  - `GET /orders/{id}`
  - `POST /orders/{orderId}/cancel`
- Admin:
  - `GET /api/admin/orders`
  - `GET /api/admin/orders/{orderId}`
  - `PUT /api/admin/orders/{orderId}/status`
  - `PUT /api/admin/orders/{orderId}/cancel`
  - `POST /api/admin/orders/{orderId}/shipping/ghn`

## 11. Module inventory

Đã có:

- Inventory theo `VariantSize`.
- Tăng tồn kho.
- Giảm tồn kho.
- Set tồn kho.
- Lock inventory khi checkout/order.
- Deduct locked quantity khi order được confirm hoặc VNPAY trả paid.
- Release locked quantity khi order pending bị hủy hoặc thanh toán fail.
- Restore deducted quantity khi order đã trừ kho nhưng cần hoàn kho.
- Repository có query lock inventory theo variant size để giảm rủi ro race condition.

Entity chính:

- `Inventory`

API chính:

- `GET /inventory/{variantSizeId}`
- `POST /inventory/increase`
- `POST /inventory/decrease`
- `PUT /inventory/set`

## 12. Module payment

Đã có:

- Payment entity gắn với order.
- Payment method: `COD`, `VNPAY`, `MOMO`, `STRIPE`.
- Payment status: `UNPAID`, `PAID`, `FAILED`, `REFUNDED`.
- Tạo payment record khi tạo order.
- Tạo URL thanh toán VNPAY.
- VNPAY return endpoint để FE đọc kết quả redirect.
- VNPAY IPN endpoint để xử lý kết quả chính thức.
- Validate chữ ký VNPAY.
- Validate amount trả về từ VNPAY.
- Idempotency cơ bản: nếu payment đã xử lý hoặc đã có transaction id thì không xử lý lại.
- Khi VNPAY thành công:
  - Payment chuyển `PAID`.
  - Order chuyển `CONFIRMED`.
  - Inventory locked được deduct.
- Khi VNPAY fail:
  - Payment chuyển `FAILED`.
  - Order chuyển `CANCELLED`.
  - Inventory locked được release nếu order còn pending.
- Có `VNPayRefundService` và job retry refund cho order `RETURNED` đã paid bằng VNPAY.

Entity chính:

- `Payment`

API chính:

- `POST /api/payment/vnpay/create`
- `GET /api/payment/vnpay-callback`
- `GET /api/payment/vnpay-ipn`
- `POST /api/payment/vnpay-ipn`

## 13. Module shipping GHN

Đã có:

- Tính phí vận chuyển qua GHN.
- Mock mode cho GHN.
- Cache phí ship.
- API lấy tỉnh/thành, quận/huyện, phường/xã từ GHN.
- Tạo vận đơn GHN khi order ở `READY_TO_SHIP`.
- Webhook GHN để cập nhật shipping status và order status.
- Map một số status GHN sang enum nội bộ.
- Khi giao COD thành công, payment COD được chuyển sang `PAID`.

Shipping status hiện có:

- `NOT_CREATED`
- `CREATED`
- `PICKING`
- `PICKED`
- `DELIVERING`
- `DELIVERED`
- `DELIVERY_FAILED`
- `RETURNING`
- `RETURNED`
- `CANCELLED`

API chính:

- `GET /api/ghn/provinces`
- `GET /api/ghn/districts`
- `GET /api/ghn/wards`
- `POST /api/webhooks/ghn`

## 14. Exception và API response

Đã có:

- `AppException`.
- `ErrorCode`.
- `GlobalExceptionHandler`.
- `ApiResponse<T>` wrapper.

Thiết kế này giúp API response nhất quán và service có thể throw lỗi nghiệp vụ rõ hơn.

## 15. Tài liệu và script hiện có

Các file tài liệu/script đáng chú ý:

- `README.md`: mô tả tổng quan backend, stack, module và cách chạy.
- `Checkout.md`: ghi chú về checkout/payment/shipping.
- `StatusFlow.txt`: thiết kế order/payment/shipping status flow, nhưng file đang bị lỗi encoding tiếng Việt.
- `AGENT.md`: hướng dẫn/ghi chú nội bộ khá dài cho agent.
- `docs/auth-migration.sql`
- `docs/ensure-admin-role.sql`
- `docs/fix-cart-item-variant-size-unique.sql`
- `docs/fix-order-status-columns.sql`
- `docs/fix-payment-enum-columns.sql`
- `docs/fix-payment-url-column.sql`
- `docs/drop-variant-size-dimensions.sql`

## 16. Test hiện tại

Hiện mới thấy test skeleton:

- `ShoeShopBasicApplicationTests.java`

Test coverage thực tế còn rất mỏng so với độ phức tạp của hệ thống, đặc biệt ở các flow nhiều tiền và tồn kho như checkout, payment IPN, cancel/refund, GHN webhook.

## 17. Những điểm đã làm tốt

- Module nghiệp vụ đã chia tương đối rõ: auth, catalog, cart, checkout, order, payment, shipping, inventory.
- Đã có entity/DTO/mapper/repository/service/controller đầy đủ cho nhiều domain chính.
- Checkout không tin total/phí ship từ client mà dùng shipping fee snapshot.
- Inventory có cơ chế lock/deduct/release/restore rõ ràng.
- Payment VNPAY có validate signature, amount và idempotency cơ bản.
- Order, payment và shipping status được tách riêng, đúng hướng cho e-commerce.
- Upload ảnh được xử lý sau commit, tránh tạo side effect khi transaction rollback.
- Config dùng environment variable, phù hợp deploy nhiều môi trường.
- Có mock GHN để dev/test khi không muốn gọi API thật.

## 18. Những điểm nên bổ sung

### Ưu tiên cao

1. Bổ sung test cho các flow quan trọng.
   - Unit/integration test cho checkout preview và consume snapshot.
   - Test tạo order từ cart và buy now.
   - Test inventory lock/deduct/release/restore.
   - Test VNPAY IPN success/fail/invalid signature/invalid amount/duplicate callback.
   - Test user/admin cancel order và refund path.
   - Test GHN webhook map status.

2. Thêm bảng lịch sử trạng thái đơn hàng.
   - `order_status_history`: order id, old status, new status, changed by, changed at, note.
   - Dùng cho timeline đơn hàng, audit, debug khi khiếu nại.
   - File `StatusFlow.txt` cũng đã đề xuất phần này.

3. Chuẩn hóa migration database.
   - Hiện có nhiều SQL fix thủ công trong `docs`.
   - Nên đưa vào Flyway hoặc Liquibase.
   - Không nên phụ thuộc lâu dài vào `spring.jpa.hibernate.ddl-auto=update` cho môi trường staging/production.

4. Siết quyền admin API.
   - Security hiện mới thấy rule global authenticated cho đa số endpoint.
   - Nên kiểm tra và bổ sung `@PreAuthorize("hasRole('ADMIN')")` cho API admin, inventory mutation, catalog mutation nếu chưa đủ.
   - Public GET product/category/brand thì giữ public, mutation phải rõ quyền.

5. Bảo vệ webhook GHN.
   - Endpoint webhook đang public.
   - Nên validate chữ ký/token/IP whitelist nếu GHN hỗ trợ.
   - Nên log raw event và xử lý idempotent theo event/order code/status.

6. Hoàn thiện refund/payment consistency.
   - Hiện có refund VNPAY nhưng cần test và audit kỹ.
   - Nên có trạng thái refund riêng hoặc bảng `refunds` nếu sau này hoàn tiền từng phần.
   - Nên lưu request/response từ cổng thanh toán để đối soát.

### Ưu tiên trung bình

7. Chuẩn hóa URL API.
   - Hiện đang trộn `/auth`, `/register`, `/cart`, `/orders`, `/products`, `/api/payment`, `/api/checkout`, `/api/admin/orders`.
   - Nên thống nhất version prefix, ví dụ `/api/v1/...`.

8. Thêm pagination/filter/search cho admin.
   - Product list, order list, user list nên có pagination rõ.
   - Order admin nên filter theo status, payment status, shipping status, date range, keyword.
   - Product nên search theo name, brand, category, status, price range.

9. Thêm audit log cho thao tác admin.
   - Ai đổi trạng thái đơn.
   - Ai sửa tồn kho.
   - Ai tạo/sửa/xóa product, variant, price.

10. Thêm coupon/discount module.
    - Hiện `discountPrice` có trong order nhưng chưa thấy module voucher/coupon hoàn chỉnh.
    - Nên có coupon code, rule, usage limit, min order amount, expiry, per-user usage.

11. Thêm review/rating sản phẩm.
    - User chỉ review sản phẩm đã mua và delivered.
    - Có moderation cho admin.

12. Thêm wishlist/favorite.
    - Hữu ích cho FE shop giày.

13. Thêm return/exchange flow.
    - Shop giày thường cần đổi size.
    - Nên có return request, exchange request, reason, ảnh bằng chứng, admin approval.

14. Tách rõ shipping dimensions.
    - Hiện weight đang hardcode 200g/item và dimensions nhiều chỗ bằng 0 hoặc default 20x20x10.
    - Nên lưu weight/length/width/height theo product hoặc variant size để phí ship chính xác.

15. Thêm notification.
    - Email khi đăng ký, verify, order created, payment success, shipping update, cancel/refund.
    - Có thể mở rộng sang realtime/websocket hoặc push notification sau.

### Ưu tiên thấp nhưng nên làm khi chuẩn bị production

16. Thêm OpenAPI/Swagger.
    - Giúp FE dễ tích hợp và giảm lệch contract.

17. Thêm logging/observability.
    - Structured log cho payment, shipping, order transition.
    - Actuator health/info đã có dependency, nên cấu hình endpoint phù hợp.

18. Thêm CI pipeline.
    - Build, test, check formatting, verify migration.

19. Thêm seed/demo data chuẩn.
    - Brand, category, product mẫu, variant size, inventory mẫu.
    - Tách rõ local/dev seed với production.

20. Chuẩn hóa validation DTO.
    - Rà lại tất cả request DTO có `@NotNull`, `@NotBlank`, `@Min`, `@Positive`, size limit.
    - Đặc biệt các request checkout, order, inventory, catalog.

21. Chuẩn hóa naming.
    - Một số endpoint/DTO đang sai chính tả hoặc chưa nhất quán, ví dụ `getCategorys`, `myinfor`, `inforbyid`, `ProductCreatRequest`.
    - Nên giữ backward compatibility nếu FE đang dùng, nhưng thêm endpoint/DTO mới đúng tên.

22. Cải thiện tài liệu tiếng Việt.
    - Sửa encoding cho `StatusFlow.txt`.
    - Tách tài liệu API, flow nghiệp vụ, setup môi trường, payment/shipping notes.

## 19. Roadmap đề xuất

### Giai đoạn 1: Làm chắc core bán hàng

- Viết test cho checkout, order, inventory, VNPAY IPN.
- Thêm `order_status_history`.
- Siết quyền admin/mutation API.
- Chuẩn hóa migration bằng Flyway hoặc Liquibase.
- Bảo vệ webhook GHN.

### Giai đoạn 2: Hoàn thiện admin vận hành

- Pagination/filter/search order.
- Pagination/filter/search product.
- Audit log admin.
- Dashboard đơn hàng cơ bản.
- Quản lý refund/return rõ ràng.

### Giai đoạn 3: Tăng tính e-commerce

- Coupon/discount.
- Review/rating.
- Wishlist.
- Return/exchange size.
- Notification theo trạng thái đơn.

### Giai đoạn 4: Chuẩn bị deploy production

- OpenAPI/Swagger.
- CI pipeline.
- Observability.
- Docker compose đầy đủ cho MySQL + Redis + app.
- Profile `local`, `test`, `staging`, `prod`.
- Secrets management thay vì phụ thuộc `.env` local.

## 20. Nhận định tổng kết

Backend hiện tại đã vượt mức CRUD cơ bản. Core domain e-commerce đã có nhiều phần quan trọng: variant-size SKU, inventory lock, checkout snapshot, VNPAY IPN, GHN shipping, OAuth2/JWT/Redis token. Việc nên tập trung tiếp theo không phải là thêm thật nhiều feature ngay, mà là làm chắc các flow có rủi ro cao: tiền, tồn kho, trạng thái đơn hàng, vận chuyển và quyền admin.

Đề xuất ưu tiên trước mắt:

1. Test các flow payment/order/inventory.
2. Thêm order status history.
3. Chuyển SQL fix sang migration chuẩn.
4. Siết authorization cho admin và mutation endpoints.
5. Bảo vệ và log webhook/payment callback.
