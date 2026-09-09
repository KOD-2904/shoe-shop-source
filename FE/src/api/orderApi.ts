import { api, unwrap } from "./client";
import type { ApiResponse, OrderResponse, OrderStatusHistoryResponse, PageResponse, PaymentMethod } from "../types";

export const orderApi = {
  create: async (body: { cartItemIds: string[]; addressId: string; shippingFeeSnapshotId: string; paymentMethod: PaymentMethod; voucherCode?: string; note?: string }) => {
    const { data } = await api.post<ApiResponse<OrderResponse> | OrderResponse>("/orders", body);
    return unwrap<OrderResponse>(data);
  },
  mine: async () => {
    const { data } = await api.get<ApiResponse<OrderResponse[]> | OrderResponse[]>("/orders/me");
    return unwrap<OrderResponse[]>(data);
  },
  minePage: async (page = 0, size = 20) => {
    const { data } = await api.get<ApiResponse<PageResponse<OrderResponse>> | PageResponse<OrderResponse>>("/orders/me", { params: { page, size } });
    return unwrap<PageResponse<OrderResponse>>(data);
  },
  byId: async (id: string) => {
    const { data } = await api.get<ApiResponse<OrderResponse> | OrderResponse>(`/orders/${id}`);
    return unwrap<OrderResponse>(data);
  },
  cancel: async (id: string) => {
    const { data } = await api.post<ApiResponse<OrderResponse> | OrderResponse>(`/orders/${id}/cancel`);
    return unwrap<OrderResponse>(data);
  },
  history: async (id: string) => {
    const { data } = await api.get<ApiResponse<OrderStatusHistoryResponse[]> | OrderStatusHistoryResponse[]>(`/orders/${id}/history`);
    return unwrap<OrderStatusHistoryResponse[]>(data);
  },
  buyNow: async (body: { variantSizeId: string; quantity: number; paymentMethod: PaymentMethod; addressId: string; shippingFeeSnapshotId: string; voucherCode?: string; note?: string }) => {
    const { data } = await api.post<ApiResponse<OrderResponse> | OrderResponse>("/orders/buy-now", body);
    return unwrap<OrderResponse>(data);
  }
};
