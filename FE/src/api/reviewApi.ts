import { api, unwrap } from "./client";
import type { ApiResponse, ProductReviewResponse, ProductReviewSummaryResponse } from "../types";

export const reviewApi = {
  byProduct: async (productId: string) => {
    const { data } = await api.get<ApiResponse<ProductReviewSummaryResponse> | ProductReviewSummaryResponse>(`/products/${productId}/reviews`);
    return unwrap<ProductReviewSummaryResponse>(data);
  },
  saveMine: async (productId: string, body: { rating: number; comment?: string; orderItemId?: string; imageUrls?: string[] }) => {
    const { data } = await api.put<ApiResponse<ProductReviewResponse> | ProductReviewResponse>(`/products/${productId}/reviews/me`, body);
    return unwrap<ProductReviewResponse>(data);
  },
  deleteMine: async (productId: string, orderItemId?: string) => {
    const { data } = await api.delete<ApiResponse<void> | void>(`/products/${productId}/reviews/me`, { params: orderItemId ? { orderItemId } : undefined });
    return unwrap<void>(data);
  }
};
