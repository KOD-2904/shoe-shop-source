# Prompt tao Frontend React Vite cho Shoe Shop

Ban la Codex dang lam viec trong thu muc `FE`. Hay tao mot frontend hoan chinh bang React Vite phu hop voi backend Spring Boot hien co trong thu muc `../BE`.

## Muc tieu

Xay dung ung dung shoe shop gom phan khach hang va phan admin, ket noi voi backend Spring Boot bang REST API. UI can dung duoc ngay de demo cac luong chinh: dang ky, dang nhap, xem thong tin tai khoan, quan ly san pham/brand/category/variant, gio hang, dia chi, checkout, dat hang, thanh toan VNPAY, xem don hang va admin cap nhat trang thai don.

## Stack bat buoc

- React + Vite.
- JavaScript hoac TypeScript deu duoc, uu tien TypeScript neu tao moi.
- React Router cho routing.
- Axios cho API client.
- TanStack Query nen dung de fetch/cache/mutation.
- CSS tuy y, uu tien Tailwind CSS neu setup nhanh gon. Khong tao landing page marketing; man hinh dau tien la storefront/app that su.
- Dung lucide-react cho icon neu co cai dat package.

## Cau hinh backend

- Base URL mac dinh: `http://localhost:8080`.
- Tao file `.env.example`:

```env
VITE_API_BASE_URL=http://localhost:8080
```

- Backend CORS mac dinh cho `http://localhost:3000`, nen chay Vite o port 3000 hoac cap nhat `ALLOWED_ORIGINS` ben BE neu dung port khac.
- Neu can ep Vite port 3000, cau hinh trong `vite.config.*`.

## Quy uoc response va error

Backend phan lon tra ve wrapper:

```ts
type ApiResponse<T> = {
  code: number;
  message?: string;
  result?: T;
};
```

Nhung mot so endpoint tra truc tiep object/array, khong boc `ApiResponse`, vi du:

- `GET /cart` tra `CartResponse`.
- `PUT /cart/items/{cartItemId}` tra `CartItemResponse`.
- `DELETE /cart/items/{cartItemId}` tra `CartItemResponse`.
- `GET /orders/me` tra `OrderResponse[]`.
- `GET /orders/{id}` tra `OrderResponse`.
- `POST /orders/buy-now` tra `OrderResponse`.
- Admin orders tra truc tiep `OrderResponse` hoac `OrderResponse[]`.

API client phai co helper unwrap linh hoat:

- Neu response co field `result` thi dung `data.result`.
- Neu khong co `result` thi dung `data`.
- Khi loi, hien `data.message` neu co, fallback HTTP status/message.

## Auth va security

Backend dung JWT Bearer token.

- Login: `POST /auth/login` body `{ identifier, password }`.
- Register: `POST /register` body `{ email, password, phone }`.
- Refresh token: `POST /auth/refreshToken` body `{ refreshToken }`.
- Logout: `POST /auth/log-out` body `{ token, logoutAllDevices }`.
- Verify email: `GET /auth/verify-email?token=...`.
- Google login API: `POST /auth/google` body `{ code, redirectUri }`.
- OAuth2 browser login co the dung link backend `/oauth2/authorization/google`; backend redirect ve `app.frontend-url` va set HttpOnly cookies, nhung JWT flow JSON van la luong uu tien cho FE.

`AuthResponse`:

```ts
type AuthResponse = {
  authenticated: boolean;
  accessToken: string;
  refreshToken: string;
  email: string;
  phone?: string;
  provider?: string;
};
```

`TokenResponse`:

```ts
type TokenResponse = {
  accessToken: string;
  refreshToken: string;
  deviceId?: string;
};
```

`UserResponse`:

```ts
type UserResponse = {
  id: string;
  email: string;
  phone?: string;
  provider?: string;
  emailVerified: boolean;
  status: string;
  roles: string[];
};
```

Yeu cau:

- Luu `accessToken`, `refreshToken`, va user summary trong localStorage.
- Axios interceptor tu dong them `Authorization: Bearer <accessToken>`.
- Neu gap 401, thu refresh token mot lan bang `/auth/refreshToken`, sau do retry request cu. Neu refresh fail thi logout.
- Tao route guard cho trang can login.
- Tao guard admin dua tren `roles` chua `ROLE_ADMIN`.
- Sau login goi `GET /myinfor` de lay roles/user hien tai.

Luu y quan trong: Trong `SecurityConfig`, tat ca endpoint khong public deu yeu cau authentication. Cac API catalog GET hien tai cung co `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`, nen storefront public se khong lay duoc product neu backend chua mo public endpoint. Hay xu ly UI bang mot trong hai cach:

1. Van tao storefront, nhung neu API tra 401/403 thi hien empty state "Can dang nhap hoac backend chua mo API public".
2. Admin dashboard la luong day du nhat cho catalog theo backend hien tai.

## Endpoints

### User/Auth

- `POST /register`
- `POST /auth/login`
- `POST /auth/google`
- `POST /auth/refreshToken`
- `POST /auth/log-out`
- `GET /auth/verify-email?token=...`
- `GET /myinfor`
- `GET /users` admin/secured
- `GET /inforbyid/{id}`
- `DELETE /users/delete`
- `POST /address/add`

`AddAddressRequest`:

```ts
type AddAddressRequest = {
  isDefault?: boolean;
  provinceId?: number;
  districtId?: number;
  wardCode?: string;
  provinceName?: string;
  districtName?: string;
  wardName?: string;
  detailAddress?: string;
  fullAddress?: string;
};
```

### Catalog

Product:

- `GET /products/getProducts`
- `GET /products/getProduct?productId=...`
- `POST /products/` JSON body `ProductRequest`, admin.
- `POST /products/with-images` multipart form-data:
  - part `product`: JSON product request.
  - part `images`: file list optional.
  - query/form param `primaryIndex` optional.
- `POST /products/addProductImage` multipart has known backend issue: method builds result but returns `null`; avoid relying on it unless fixed.

Types:

```ts
type ProductRequest = {
  name: string;
  slug?: string;
  description?: string;
  brandId?: string;
  categoryId?: string;
  basePrice: number;
};

type ProductResponse = ProductRequest & {
  id: string;
  status?: "DRAFT" | "ACTIVE" | "INACTIVE" | string;
};
```

Brand:

- `GET /brand/getBrands`
- `GET /brand/getBrand?brandId=...`
- `POST /brand/add`
- `POST /brand/addBrandImage` multipart: `image`, `brandId`.
- `POST /brand/addBrandWithImage` multipart: `image`, `brand`.

```ts
type BrandRequest = { name: string; logoUrl?: string };
type BrandResponse = { id: string; name: string; logoUrl?: string };
```

Category:

- `GET /category/getCategorys`
- `GET /category/getCategory?id=...`
- `POST /category/add`

```ts
type CategoryRequest = { name: string; parentId?: string | null };
type CategoryResponse = { id: string; name: string; parentId?: string | null };
```

Variant:

- `POST /variants/addVariant?initQuantity=...`
- `POST /variants/addVariants?init=...`
- `POST /variants/variants/{variantId}/images` multipart `images`, optional `primaryIndex`.

```ts
type ProductVariantRequest = {
  productId: string;
  sku: string;
  size: string;
  color: string;
  price: number;
  active?: boolean;
};

type ProductVariantResponse = ProductVariantRequest & {
  id: string;
};
```

### Inventory

- `GET /inventory/{variantId}`
- `POST /inventory/increase?variantId=...&quantity=...`
- `POST /inventory/decrease?variantId=...&quantity=...`
- `PUT /inventory/set?variantId=...&quantity=...`

```ts
type InventoryResponse = {
  variantId: string;
  quantity: number;
};
```

### Cart

- `GET /cart`
- `POST /cart/items` body `{ variantId, quantity }`, returns no body.
- `PUT /cart/items/{cartItemId}?quantity=...`
- `DELETE /cart/items/{cartItemId}`
- `DELETE /cart`

Types:

```ts
type AddCartItemRequest = {
  variantId: string;
  quantity: number;
};

type CartItemResponse = {
  cartItemId: string;
  variantId: string;
  productName: string;
  brand?: string;
  category?: string;
  size?: string;
  color?: string;
  price: number;
  quantity: number;
  lineTotal: number;
};

type CartResponse = {
  cartId: string;
  items: CartItemResponse[];
  subtotal: number;
};
```

### Checkout

- `POST /api/checkout/calculate-shipping`

Request:

```ts
type CheckoutRequest = {
  addressId?: string;
  cartItemIds: string[];
  totalProductPrice: number;
  shippingFeeRequest: {
    toDistrictId: number;
    toWardCode: string;
    weight: number;
    length?: number;
    width?: number;
    height?: number;
    insuranceValue?: number;
  };
};
```

Response `result`:

```ts
type ShippingCalculation = {
  shipping_fee: number;
  product_total: number;
  total_amount: number;
};
```

### Orders

- `POST /orders`
- `GET /orders/me`
- `GET /orders/{id}`
- `POST /orders/buy-now`

Enums:

```ts
type PaymentMethod = "COD" | "VNPAY" | "MOMO" | "STRIPE";
type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "SHIPPED"
  | "DELIVERED"
  | "COMPLETED"
  | "CANCELED";
```

Requests:

```ts
type CreateOrderRequest = {
  cartItemIds: string[];
  addressId: string;
  paymentMethod: PaymentMethod;
  note?: string;
};

type BuyNowRequest = {
  variantId: string;
  quantity: number;
  paymentMethod: PaymentMethod;
  addressId: string;
  note?: string;
};
```

Responses:

```ts
type OrderItemResponse = {
  id: string;
  productId: string;
  productName: string;
  variantId: string;
  variantName?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
};

type OrderResponse = {
  id: string;
  userId: string;
  status: OrderStatus | string;
  totalPrice: number;
  shippingPrice?: number;
  discountPrice?: number;
  items: OrderItemResponse[];
  paymentId?: string;
  paymentUrl?: string;
  createdAt?: string;
};
```

Neu `POST /orders` voi `paymentMethod: "VNPAY"` tra `paymentUrl`, FE phai redirect browser den `paymentUrl`.

### Admin Orders

Can role admin.

- `GET /api/admin/orders`
- `GET /api/admin/orders/{orderId}`
- `PUT /api/admin/orders/{orderId}/status` body `{ status }`
- `PUT /api/admin/orders/{orderId}/cancel`

### Payment VNPAY

Public theo security config.

- `POST /api/payment/vnpay/create?orderId=...` tra `{ paymentUrl }` trong `result`.
- `GET /api/payment/vnpay-callback?...` tra `ApiResponse` result `{ code, paymentId }`.
- `GET /api/payment/vnpay-ipn?...` cho VNPAY server, FE khong can goi.

Tao route FE `/payment-result` hoac `/payment/callback` de hien ket qua. Neu backend VNPAY return URL dang la `http://localhost:8080/api/payment/vnpay-callback`, nguoi dung se thay JSON backend. Hay ghi chu trong UI/docs rang nen cau hinh `VNPAY_RETURN_URL` ve route frontend neu muon UX dep, hoac sau khi redirect ve backend thi FE co the co nut quay lai trang don hang.

## Man hinh can tao

### Public/auth

- `/login`: form identifier/password, link register, login submit, hien loi tu backend.
- `/register`: email/password/phone, thong bao can verify email neu register thanh cong.
- `/verify-email`: doc query `token`, goi `/auth/verify-email`.
- Layout chinh co header, search box, cart icon, user menu.

### Customer

- `/`: storefront/product list. Neu catalog API bi 403, hien empty state co huong dan dang nhap/admin.
- `/products/:id`: product detail. Neu chua co endpoint list variant theo product, tao UI fallback cho admin hoac nhap variantId thu cong trong demo.
- `/cart`: hien gio hang, update quantity, remove item, clear cart.
- `/checkout`: chon cart item, nhap/sua dia chi, tinh shipping, chon payment method COD/VNPAY, tao order. Neu VNPAY co `paymentUrl`, redirect.
- `/orders`: danh sach don cua toi.
- `/orders/:id`: chi tiet don.
- `/account`: thong tin user, form them dia chi.

### Admin

- `/admin`: dashboard ngan gon.
- `/admin/products`: list products, tao product, upload product with images.
- `/admin/brands`: list/create brand, upload logo.
- `/admin/categories`: list/create category.
- `/admin/variants`: tao variant, tao nhieu variant, set inventory.
- `/admin/orders`: list/detail/cap nhat status/cancel order.
- `/admin/users`: list users.

## UX va implementation yeu cau

- Thiet ke giong ecommerce/admin tool hien dai, gon, de scan thong tin.
- Khong dung text giai thich dai trong app. Dung empty state ngan gon khi API chua co data hoac bi 403.
- Tat ca form co loading state, disabled khi submit, toast/snackbar success/error.
- Money format theo VND: `Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" })`.
- Date format theo `vi-VN`.
- Dung table cho admin list, card/grid cho storefront product.
- Tao components dung lai: `Button`, `Input`, `Select`, `Modal/Dialog`, `DataTable` neu can, `StatusBadge`, `Price`, `PageHeader`.
- Tao API modules rieng theo domain: `authApi`, `catalogApi`, `cartApi`, `checkoutApi`, `orderApi`, `adminApi`, `inventoryApi`, `paymentApi`.
- Tao auth store/context rieng, khong goi localStorage lung tung trong component.
- Multipart request phai dung `FormData`; voi JSON part co the append Blob:

```ts
formData.append(
  "product",
  new Blob([JSON.stringify(product)], { type: "application/json" })
);
```

- Sau mutation thanh cong, invalidate query lien quan.
- Bao ve route admin neu user khong co `ROLE_ADMIN`, redirect hoac hien forbidden.

## Viec can lam khi bat dau trong thu muc FE

1. Neu thu muc FE rong, khoi tao Vite React app tai chinh thu muc hien tai.
2. Cai packages can thiet.
3. Tao `.env.example`, config port 3000.
4. Implement API client va auth flow truoc.
5. Implement routes/layout.
6. Implement cac man hinh theo muc tren.
7. Chay build/test/lint neu co.
8. Chay dev server va bao URL.

## Luu y backend hien tai

- Nhieu endpoint catalog dang chi admin moi goi duoc. Neu muon customer browse public, can sua backend sau: public GET `/products/getProducts`, `/products/getProduct`, `/brand/getBrands`, `/category/getCategorys`, va them endpoint lay variants theo product.
- `ProductController.addProductImage` hien return `null`; FE nen tranh dung API nay.
- `CartController.removeItem` va `clearCart` dung `@AuthenticationPrincipal UserAccount user` trong khi cac method khac dung expression `"user"`; neu gap loi auth khi delete cart item, can sua backend.
- Backend `app.frontend-url` mac dinh `http://localhost:3000`, hop voi Vite port 3000.
