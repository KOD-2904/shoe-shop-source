import { api, unwrap } from "./client";
import type { ApiResponse, InventoryListItemResponse, InventoryResponse } from "../types";

export const inventoryApi = {
  all: async () => {
    const { data } = await api.get<ApiResponse<InventoryListItemResponse[]> | InventoryListItemResponse[]>("/inventory");
    return unwrap<InventoryListItemResponse[]>(data);
  },
  lowStock: async (threshold = 5) => {
    const { data } = await api.get<ApiResponse<InventoryListItemResponse[]> | InventoryListItemResponse[]>("/inventory/low-stock", {
      params: { threshold }
    });
    return unwrap<InventoryListItemResponse[]>(data);
  },
  get: async (variantSizeId: string) => {
    const { data } = await api.get<ApiResponse<InventoryResponse> | InventoryResponse>(`/inventory/${variantSizeId}`);
    return unwrap<InventoryResponse>(data);
  },
  increase: async (variantSizeId: string, quantity: number) => {
    const { data } = await api.post<ApiResponse<InventoryResponse> | InventoryResponse>("/inventory/increase", null, {
      params: { variantSizeId, quantity }
    });
    return unwrap<InventoryResponse>(data);
  },
  decrease: async (variantSizeId: string, quantity: number) => {
    const { data } = await api.post<ApiResponse<InventoryResponse> | InventoryResponse>("/inventory/decrease", null, {
      params: { variantSizeId, quantity }
    });
    return unwrap<InventoryResponse>(data);
  },
  set: async (variantSizeId: string, quantity: number) => {
    const { data } = await api.put<ApiResponse<InventoryResponse> | InventoryResponse>("/inventory/set", null, {
      params: { variantSizeId, quantity }
    });
    return unwrap<InventoryResponse>(data);
  }
};
