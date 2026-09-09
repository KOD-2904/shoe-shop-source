import { api, unwrap } from "./client";
import type { ApiResponse, VoucherResponse } from "../types";

export const voucherApi = {
  adminList: async () => {
    const { data } = await api.get<ApiResponse<VoucherResponse[]> | VoucherResponse[]>("/api/admin/vouchers");
    return unwrap<VoucherResponse[]>(data);
  },
  adminCreate: async (body: {
    code: string;
    name: string;
    type: "FIXED" | "PERCENT";
    value: number;
    minOrderAmount: number;
    maxDiscountAmount?: number;
    usageLimit: number;
    active: boolean;
    startsAt?: string;
    endsAt?: string;
  }) => {
    const { data } = await api.post<ApiResponse<VoucherResponse> | VoucherResponse>("/api/admin/vouchers", body);
    return unwrap<VoucherResponse>(data);
  },
  setActive: async (id: string, active: boolean) => {
    const { data } = await api.patch<ApiResponse<VoucherResponse> | VoucherResponse>(`/api/admin/vouchers/${id}/active`, null, { params: { active } });
    return unwrap<VoucherResponse>(data);
  }
};
