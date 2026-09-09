import { api, unwrap } from "./client";
import type { ApiResponse, CheckoutPreviewResponse } from "../types";
import type { BuyNowPreviewRequest } from "../lib/checkoutIntent";

export const checkoutApi = {
  preview: async (body: {
    addressId?: string;
    cartItemIds: string[];
    totalProductPrice?: number;
    shippingFeeRequest?: {
      toDistrictId: number;
      toWardCode: string;
      weight: number;
      length?: number;
      width?: number;
      height?: number;
      insuranceValue?: number;
    };
    voucherCode?: string;
  }) => {
    const { data } = await api.post<ApiResponse<CheckoutPreviewResponse>>("/api/checkout/preview", body);
    return unwrap<CheckoutPreviewResponse>(data);
  },
  buyNowPreview: async (body: BuyNowPreviewRequest) => {
    const { data } = await api.post<ApiResponse<CheckoutPreviewResponse>>("/api/checkout/buy-now-preview", body);
    return unwrap<CheckoutPreviewResponse>(data);
  }
};
