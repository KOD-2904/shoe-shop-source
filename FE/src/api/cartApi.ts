import { api, unwrap } from "./client";
import type { ApiResponse, CartItemResponse, CartResponse } from "../types";

export const cartApi = {
  get: async () => {
    const { data } = await api.get<ApiResponse<CartResponse> | CartResponse>("/cart");
    return unwrap<CartResponse>(data);
  },
  add: async (body: { variantSizeId: string; quantity: number }) => {
    await api.post("/cart/items", body);
  },
  update: async (cartItemId: string, quantity: number) => {
    const { data } = await api.put<ApiResponse<CartItemResponse> | CartItemResponse>(`/cart/items/${cartItemId}`, null, {
      params: { quantity }
    });
    return unwrap<CartItemResponse>(data);
  },
  remove: async (cartItemId: string) => {
    const { data } = await api.delete<ApiResponse<CartItemResponse> | CartItemResponse>(`/cart/items/${cartItemId}`);
    return unwrap<CartItemResponse>(data);
  },
  clear: async () => {
    await api.delete("/cart");
  }
};
