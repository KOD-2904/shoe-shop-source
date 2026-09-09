import { api, unwrap } from "./client";
import type { ApiResponse, ProductDiscountResponse, PromotionTargetType, VoucherType } from "../types";

export type ProductDiscountRequest = {
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

export const productDiscountApi = {
  adminList: async () => {
    const { data } = await api.get<ApiResponse<ProductDiscountResponse[]> | ProductDiscountResponse[]>("/api/admin/product-discounts");
    return unwrap<ProductDiscountResponse[]>(data);
  },
  adminCreate: async (body: ProductDiscountRequest) => {
    const { data } = await api.post<ApiResponse<ProductDiscountResponse> | ProductDiscountResponse>("/api/admin/product-discounts", body);
    return unwrap<ProductDiscountResponse>(data);
  },
  setActive: async (id: string, active: boolean) => {
    const { data } = await api.patch<ApiResponse<ProductDiscountResponse> | ProductDiscountResponse>(`/api/admin/product-discounts/${id}/active`, null, { params: { active } });
    return unwrap<ProductDiscountResponse>(data);
  }
};
