import { api, unwrap } from "./client";
import type { ApiResponse } from "../types";

export const paymentApi = {
  createVnpay: async (orderId: string) => {
    const { data } = await api.post<ApiResponse<{ paymentUrl: string }>>("/api/payment/vnpay/create", null, {
      params: { orderId }
    });
    return unwrap<{ paymentUrl: string }>(data);
  }
};
