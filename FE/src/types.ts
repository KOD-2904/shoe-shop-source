export type ApiResponse<T> = {
  code: number;
  message?: string;
  result?: T;
};

export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type AuthResponse = {
  authenticated: boolean;
  accessToken: string;
  refreshToken?: string;
  email: string;
  phone?: string;
  providers?: string[];
};

export type TokenResponse = {
  accessToken: string;
  refreshToken?: string;
  deviceId?: string;
};

export type UserResponse = {
  id: string;
  email: string;
  phone?: string;
  providers?: string[];
  emailVerified: boolean;
  status: string;
  roles: string[];
};

export type BrandResponse = { id: string; name: string; logoUrl?: string };
export type CategoryResponse = { id: string; name: string; parentId?: string | null };

export type ProductRequest = {
  name: string;
  slug?: string;
  description?: string;
  brandId?: string;
  categoryId?: string;
  basePrice: number;
};

export type ProductResponse = ProductRequest & {
  id: string;
  brandName?: string;
  categoryName?: string;
  originalPrice?: number;
  salePrice?: number;
  discountPercent?: number;
  onSale?: boolean;
  status?: "DRAFT" | "ACTIVE" | "INACTIVE" | string;
  primaryImageUrl?: string;
  imageUrls?: string[];
};

export type WishlistItemResponse = {
  id: string;
  productId: string;
  createdAt?: string;
  product: ProductResponse;
};

export type ProductReviewResponse = {
  id: string;
  productId: string;
  userId: string;
  reviewer: string;
  rating: number;
  comment?: string;
  orderItemId?: string;
  imageUrls?: string[];
  createdAt?: string;
  updatedAt?: string;
};

export type ProductReviewSummaryResponse = {
  productId: string;
  averageRating: number;
  reviewCount: number;
  reviews: ProductReviewResponse[];
};

export type ProductVariantRequest = {
  productId: string;
  color: string;
  active?: boolean;
  sizes: VariantSizeRequest[];
};

export type VariantSizeRequest = {
  size: string;
  sku: string;
  price: number;
  initQuantity?: number;
};

export type VariantSizeResponse = VariantSizeRequest & {
  id: string;
  variantId: string;
  variantColor?: string;
  productId?: string;
  productName?: string;
  originalPrice?: number;
  salePrice?: number;
  discountPercent?: number;
  onSale?: boolean;
  quantity?: number;
};

export type ProductVariantResponse = {
  id: string;
  productId: string;
  productName?: string;
  color: string;
  active?: boolean;
  primaryImageUrl?: string;
  imageUrls?: string[];
  sizes: VariantSizeResponse[];
};

export type VariantFormOptions = {
  products: ProductResponse[];
  variants?: ProductVariantResponse[];
};

export type InventoryResponse = {
  variantId?: string;
  variantSizeId?: string;
  quantity: number;
};

export type InventoryListItemResponse = {
  inventoryId: string;
  productId: string;
  productName: string;
  variantId: string;
  color?: string;
  variantSizeId: string;
  size: string;
  sku: string;
  price: number;
  originalPrice?: number;
  discountPrice?: number;
  quantity: number;
  quantityLocked: number;
  availableQuantity: number;
  lowStock: boolean;
  outOfStock: boolean;
};

export type CartItemResponse = {
  cartItemId: string;
  variantId?: string;
  variantSizeId?: string;
  productName: string;
  imageUrl?: string;
  brand?: string;
  category?: string;
  size?: string;
  color?: string;
  price: number;
  quantity: number;
  lineTotal: number;
};

export type CartResponse = {
  cartId: string;
  items: CartItemResponse[];
  subtotal: number;
};

export type PaymentMethod = "COD" | "VNPAY" | "MOMO" | "STRIPE";
export type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "PACKING"
  | "READY_TO_SHIP"
  | "SHIPPING"
  | "DELIVERED"
  | "CANCELLED"
  | "FAILED"
  | "RETURNED"
  | string;
export type PaymentStatus = "UNPAID" | "PAID" | "FAILED" | "REFUNDED" | string;
export type ShippingStatus =
  | "NOT_CREATED"
  | "CREATED"
  | "PICKING"
  | "PICKED"
  | "DELIVERING"
  | "DELIVERED"
  | "DELIVERY_FAILED"
  | "RETURNING"
  | "RETURNED"
  | "CANCELLED"
  | string;

export type GHNMode = "REAL" | "MOCK_TEST";

export type OrderItemResponse = {
  id: string;
  productId: string;
  productName: string;
  variantId?: string;
  variantSizeId?: string;
  variantName?: string;
  size?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  reviewed?: boolean;
};

export type OrderResponse = {
  id: string;
  userId: string;
  status: OrderStatus;
  paymentMethod?: PaymentMethod | string;
  paymentStatus?: PaymentStatus;
  shippingStatus?: ShippingStatus;
  shippingProvider?: string;
  shippingOrderCode?: string;
  totalPrice: number;
  finalTotalPrice?: number;
  shippingPrice?: number;
  discountPrice?: number;
  voucherCode?: string;
  items: OrderItemResponse[];
  paymentId?: string;
  paymentUrl?: string;
  createdAt?: string;
};

export type OrderStatusHistoryResponse = {
  id: string;
  orderId: string;
  actorId?: string;
  actorEmail?: string;
  oldOrderStatus?: OrderStatus;
  newOrderStatus?: OrderStatus;
  oldPaymentStatus?: PaymentStatus;
  newPaymentStatus?: PaymentStatus;
  oldShippingStatus?: ShippingStatus;
  newShippingStatus?: ShippingStatus;
  source: string;
  note?: string;
  createdAt?: string;
};

export type AddressRequest = {
  isDefault?: boolean;
  receiverName?: string;
  phoneNumber?: string;
  provinceId?: number;
  districtId?: number;
  wardCode?: string;
  provinceName?: string;
  districtName?: string;
  wardName?: string;
  detailAddress?: string;
  fullAddress?: string;
};

export type AddressResponse = AddressRequest & {
  id: string;
};

export type GHNProvince = {
  ProvinceID: number;
  ProvinceName: string;
};

export type GHNDistrict = {
  DistrictID: number;
  DistrictName: string;
  ProvinceID?: number;
};

export type GHNWard = {
  WardCode: string;
  WardName: string;
  DistrictID?: number;
};

export type GHNSettingsResponse = {
  mode: GHNMode;
  mockFixedFee: number;
  mockActive: boolean;
  realConfigured: boolean;
  apiUrl?: string;
};

export type CheckoutPreviewResponse = {
  shippingFeeSnapshotId: string;
  addressId: string;
  productTotal: number;
  shippingFee: number;
  discountAmount?: number;
  voucherCode?: string;
  totalAmount: number;
  expiresAt: string;
};

export type VoucherType = "FIXED" | "PERCENT";
export type PromotionTargetType = "PRODUCT" | "CATEGORY" | "VARIANT_SIZE";

export type VoucherResponse = {
  id: string;
  code: string;
  name: string;
  type: VoucherType;
  value: number;
  minOrderAmount: number;
  maxDiscountAmount?: number;
  usageLimit: number;
  usedCount: number;
  active: boolean;
  startsAt?: string;
  endsAt?: string;
};

export type ProductDiscountResponse = {
  id: string;
  name: string;
  targetType: PromotionTargetType;
  targetId: string;
  type: VoucherType;
  value: number;
  maxDiscountAmount?: number;
  active: boolean;
  startsAt?: string;
  endsAt?: string;
};
