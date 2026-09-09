import { api, unwrap } from "./client";
import type { ApiResponse, WishlistItemResponse } from "../types";

export const wishlistApi = {
  mine: async () => {
    const { data } = await api.get<ApiResponse<WishlistItemResponse[]> | WishlistItemResponse[]>("/wishlist");
    return unwrap<WishlistItemResponse[]>(data);
  },
  add: async (productId: string) => {
    const { data } = await api.post<ApiResponse<WishlistItemResponse> | WishlistItemResponse>(`/wishlist/${productId}`);
    return unwrap<WishlistItemResponse>(data);
  },
  remove: async (productId: string) => {
    const { data } = await api.delete<ApiResponse<void> | void>(`/wishlist/${productId}`);
    return unwrap<void>(data);
  },
  exists: async (productId: string) => {
    const { data } = await api.get<ApiResponse<{ wishlisted: boolean }> | { wishlisted: boolean }>(`/wishlist/${productId}/exists`);
    return unwrap<{ wishlisted: boolean }>(data);
  }
};
