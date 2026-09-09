import { api, unwrap } from "./client";
import type { ApiResponse, GHNMode, GHNSettingsResponse, OrderResponse, OrderStatus, OrderStatusHistoryResponse, PageResponse } from "../types";

export const adminApi = {
  orders: async () => {
    const { data } = await api.get<ApiResponse<OrderResponse[]> | OrderResponse[]>("/api/admin/orders");
    return unwrap<OrderResponse[]>(data);
  },
  ordersPage: async (page = 0, size = 20) => {
    const { data } = await api.get<ApiResponse<PageResponse<OrderResponse>> | PageResponse<OrderResponse>>("/api/admin/orders", { params: { page, size } });
    return unwrap<PageResponse<OrderResponse>>(data);
  },
  order: async (orderId: string) => {
    const { data } = await api.get<ApiResponse<OrderResponse> | OrderResponse>(`/api/admin/orders/${orderId}`);
    return unwrap<OrderResponse>(data);
  },
  orderHistory: async (orderId: string) => {
    const { data } = await api.get<ApiResponse<OrderStatusHistoryResponse[]> | OrderStatusHistoryResponse[]>(`/api/admin/orders/${orderId}/history`);
    return unwrap<OrderStatusHistoryResponse[]>(data);
  },
  updateStatus: async (orderId: string, status: OrderStatus) => {
    const { data } = await api.put<ApiResponse<OrderResponse> | OrderResponse>(`/api/admin/orders/${orderId}/status`, { status });
    return unwrap<OrderResponse>(data);
  },
  cancel: async (orderId: string) => {
    const { data } = await api.put<ApiResponse<OrderResponse> | OrderResponse>(`/api/admin/orders/${orderId}/cancel`);
    return unwrap<OrderResponse>(data);
  },
  createShipping: async (orderId: string) => {
    const { data } = await api.post<ApiResponse<OrderResponse> | OrderResponse>(`/api/admin/orders/${orderId}/shipping/ghn`);
    return unwrap<OrderResponse>(data);
  },
  ghnSettings: async () => {
    const { data } = await api.get<ApiResponse<GHNSettingsResponse> | GHNSettingsResponse>("/api/admin/shipping/ghn/settings");
    return unwrap<GHNSettingsResponse>(data);
  },
  updateGhnSettings: async (body: { mode?: GHNMode; mockFixedFee?: number }) => {
    const { data } = await api.put<ApiResponse<GHNSettingsResponse> | GHNSettingsResponse>("/api/admin/shipping/ghn/settings", body);
    return unwrap<GHNSettingsResponse>(data);
  }
};
