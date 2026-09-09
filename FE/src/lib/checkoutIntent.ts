import type { PaymentMethod } from "../types";

const BUY_NOW_KEY = "shoe_shop_buy_now_intent";

export type BuyNowIntent = {
  variantSizeId: string;
  quantity: number;
  productName: string;
  variantLabel?: string;
  imageUrl?: string;
  unitPrice: number;
};

export function saveBuyNowIntent(intent: BuyNowIntent) {
  sessionStorage.setItem(BUY_NOW_KEY, JSON.stringify(intent));
}

export function getBuyNowIntent(): BuyNowIntent | null {
  const raw = sessionStorage.getItem(BUY_NOW_KEY);
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as BuyNowIntent;
    return parsed.variantSizeId && parsed.quantity > 0 ? parsed : null;
  } catch {
    return null;
  }
}

export function clearBuyNowIntent() {
  sessionStorage.removeItem(BUY_NOW_KEY);
}

export type CheckoutMode = "cart" | "buy-now";

export type BuyNowPreviewRequest = {
  variantSizeId: string;
  quantity: number;
  paymentMethod: PaymentMethod;
  addressId: string;
  voucherCode?: string;
};
