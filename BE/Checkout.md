# Checkout Flow For FE

## Core Rules

- User must have at least one shipping address before checkout.
- If `addressId` is omitted in checkout preview, backend uses the user's default address.
- FE must call checkout preview before creating an order.
- Checkout preview returns a short-lived `shippingFeeSnapshotId`; send it when creating the order.
- VNPay redirect callback is not the source of truth. FE should fetch order detail after redirect. Final payment update is handled by VNPay IPN.

Default TTL:

```text
checkout.shipping-fee-snapshot-ttl-seconds=300
```

## Address APIs

### Add Address

```http
POST /api/addresses
Authorization: Bearer <token>
Content-Type: application/json
```

Body:

```json
{
  "isDefault": true,
  "provinceId": 202,
  "districtId": 1524,
  "wardCode": "40101",
  "provinceName": "HCM",
  "districtName": "Quan 1",
  "wardName": "Ben Nghe",
  "detailAddress": "123 Nguyen Hue"
}
```

Legacy endpoint still exists:

```http
POST /address/add
```

### List Addresses

```http
GET /api/addresses
Authorization: Bearer <token>
```

Legacy endpoint still exists:

```http
GET /users/addresses
```

### Change Default Address

```http
PUT /api/addresses/{addressId}/default
Authorization: Bearer <token>
```

### Update Address

```http
PUT /api/addresses/{addressId}
Authorization: Bearer <token>
Content-Type: application/json
```

Body:

```json
{
  "isDefault": true,
  "receiverName": "Nguyen Van A",
  "phoneNumber": "0900000000",
  "provinceId": 202,
  "districtId": 1524,
  "wardCode": "40101",
  "provinceName": "HCM",
  "districtName": "Quan 1",
  "wardName": "Ben Nghe",
  "detailAddress": "123 Nguyen Hue"
}
```

## Checkout Preview

Use this before `POST /orders`.

```http
POST /api/checkout/preview
Authorization: Bearer <token>
Content-Type: application/json
```

Body:

```json
{
  "addressId": "optional-address-id",
  "cartItemIds": ["cart-item-id-1", "cart-item-id-2"]
}
```

If `addressId` is missing, backend uses default address. If no address exists, backend returns `ADDRESS_NOT_FOUND`.

Response:

```json
{
  "code": 200,
  "message": "Checkout preview created",
  "result": {
    "shippingFeeSnapshotId": "snapshot-id",
    "addressId": "address-id-used",
    "productTotal": 1200000,
    "shippingFee": 30000,
    "totalAmount": 1230000,
    "expiresAt": "2026-06-24T10:05:00"
  }
}
```

Compatibility endpoint:

```http
POST /api/checkout/calculate-shipping
```

It now behaves like `/api/checkout/preview`.

## Create Order From Cart

```http
POST /orders
Authorization: Bearer <token>
Content-Type: application/json
```

Body:

```json
{
  "cartItemIds": ["cart-item-id-1", "cart-item-id-2"],
  "addressId": "address-id-used",
  "shippingFeeSnapshotId": "snapshot-id",
  "paymentMethod": "VNPAY",
  "note": "Call before delivery"
}
```

Payment methods currently accepted by enum:

```text
COD, VNPAY, MOMO, STRIPE
```

Implemented flow is reliable for:

```text
COD, VNPAY
```

Response for VNPay includes `paymentUrl`:

```json
{
  "code": 200,
  "message": "Order created",
  "result": {
    "id": "order-id",
    "status": "PENDING",
    "shippingStatus": "NOT_CREATED",
    "totalPrice": 1200000,
    "shippingPrice": 30000,
    "paymentMethod": "VNPAY",
    "paymentStatus": "UNPAID",
    "paymentId": "payment-id",
    "paymentUrl": "https://sandbox.vnpayment.vn/..."
  }
}
```

Frontend should redirect user to `paymentUrl`.

## Create VNPay URL Again

Use when order was created but FE lost the URL.

```http
POST /api/payment/vnpay/create?orderId={orderId}
Authorization: Bearer <token>
```

Rules:

- Order must belong to current user.
- Payment method must be `VNPAY`.
- Payment status must still be `UNPAID`.

Response:

```json
{
  "code": 200,
  "message": "Payment created successfully",
  "result": {
    "paymentUrl": "https://sandbox.vnpayment.vn/...",
    "paymentId": "payment-id"
  }
}
```

## VNPay Return Handling

VNPay redirects browser to:

```http
GET /api/payment/vnpay-callback
```

This endpoint only reports redirect data:

```json
{
  "code": 200,
  "message": "Return from VNPAY. Use order detail API for final status.",
  "result": {
    "code": "00",
    "transactionStatus": "00",
    "orderId": "order-id",
    "paymentId": "payment-id",
    "validSignature": true
  }
}
```

FE should then call:

```http
GET /orders/{orderId}
Authorization: Bearer <token>
```

Do not mark the order paid only from callback. VNPay IPN updates the database.

## VNPay IPN

Public endpoint for VNPay server:

```http
GET /api/payment/vnpay-ipn
```

Backend behavior:

```text
Valid signature + amount match + responseCode=00 + transactionStatus=00
  -> PaymentStatus.PAID
  -> OrderStatus.CONFIRMED
  -> locked stock is deducted

Payment failed
  -> PaymentStatus.FAILED
  -> OrderStatus.CANCELLED
  -> locked stock is released
```

If VNPay never calls IPN, scheduled timeout scans expired `UNPAID` payments and cancels the order.

## User Order APIs

```http
GET /orders/me
GET /orders/{orderId}
POST /orders/{orderId}/cancel
```

User can cancel only when:

```text
OrderStatus.PENDING
```

Cancel behavior:

```text
release locked stock
PaymentStatus.FAILED if payment is still UNPAID
OrderStatus.CANCELLED
```

## Admin Order APIs

```http
GET /api/admin/orders
GET /api/admin/orders/{orderId}
PUT /api/admin/orders/{orderId}/status
PUT /api/admin/orders/{orderId}/cancel
POST /api/admin/orders/{orderId}/shipping/ghn
```

Update status body:

```json
{
  "status": "PACKING"
}
```

Allowed order transitions:

```text
PENDING -> CONFIRMED | CANCELLED
CONFIRMED -> PACKING | CANCELLED
PACKING -> READY_TO_SHIP
READY_TO_SHIP -> SHIPPING
SHIPPING -> DELIVERED | FAILED
FAILED -> SHIPPING | RETURNED
DELIVERED/CANCELLED/RETURNED -> final
```

Confirm behavior:

```text
COD:
  PENDING -> CONFIRMED
  locked stock is deducted
  payment remains UNPAID until delivery

VNPAY:
  confirmation happens from successful IPN
```

## Create GHN Shipping Order

Admin creates GHN order when:

```text
OrderStatus.READY_TO_SHIP
ShippingStatus.NOT_CREATED
```

Endpoint:

```http
POST /api/admin/orders/{orderId}/shipping/ghn
Authorization: Bearer <admin-token>
```

Backend behavior:

```text
call GHN /shipping-order/create
save shippingProvider = GHN
save shippingOrderCode
ShippingStatus.CREATED
OrderStatus.SHIPPING
```

If `ghn.mock-enabled=true`, backend returns a mock order code and does not call GHN.

## GHN Webhook

Public endpoint:

```http
POST /api/webhooks/ghn
Content-Type: application/json
```

Example:

```json
{
  "order_code": "GHN123456",
  "status": "delivering"
}
```

Status mapping:

```text
ready_to_pick, created -> ShippingStatus.CREATED
picking -> ShippingStatus.PICKING
picked -> ShippingStatus.PICKED
delivering, transporting, sorting -> ShippingStatus.DELIVERING
delivered -> ShippingStatus.DELIVERED
delivery_fail, delivery_failed, waiting_to_return -> ShippingStatus.DELIVERY_FAILED
return, returning -> ShippingStatus.RETURNING
returned -> ShippingStatus.RETURNED
cancel, cancelled, canceled -> ShippingStatus.CANCELLED
```

Order mapping:

```text
PICKED/DELIVERING -> OrderStatus.SHIPPING
DELIVERED -> OrderStatus.DELIVERED
DELIVERY_FAILED -> OrderStatus.FAILED
RETURNED -> OrderStatus.RETURNED
CANCELLED -> OrderStatus.CANCELLED
```

For COD:

```text
ShippingStatus.DELIVERED
  -> PaymentStatus.PAID
  -> paidAt = now
```
