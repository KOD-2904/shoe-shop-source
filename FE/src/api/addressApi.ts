import { api, unwrap } from "./client";
import type { AddressRequest, AddressResponse, ApiResponse, GHNDistrict, GHNProvince, GHNWard } from "../types";

export const addressApi = {
  mine: async () => {
    const { data } = await api.get<ApiResponse<AddressResponse[]> | AddressResponse[]>("/api/addresses");
    return unwrap<AddressResponse[]>(data);
  },
  add: async (body: AddressRequest) => {
    const { data } = await api.post<ApiResponse<AddressResponse> | AddressResponse>("/api/addresses", body);
    return unwrap<AddressResponse>(data);
  },
  setDefault: async (addressId: string) => {
    const { data } = await api.put<ApiResponse<AddressResponse> | AddressResponse>(`/api/addresses/${addressId}/default`);
    return unwrap<AddressResponse>(data);
  },
  update: async (addressId: string, body: AddressRequest) => {
    const { data } = await api.put<ApiResponse<AddressResponse> | AddressResponse>(`/api/addresses/${addressId}`, body);
    return unwrap<AddressResponse>(data);
  },
  remove: async (addressId: string) => {
    const { data } = await api.delete<ApiResponse<AddressResponse> | AddressResponse>(`/api/addresses/${addressId}`);
    return unwrap<AddressResponse>(data);
  },
  provinces: async () => {
    const { data } = await api.get<ApiResponse<GHNProvince[]> | GHNProvince[]>("/api/ghn/provinces");
    return unwrap<GHNProvince[]>(data);
  },
  districts: async (provinceId: number) => {
    const { data } = await api.get<ApiResponse<GHNDistrict[]> | GHNDistrict[]>("/api/ghn/districts", { params: { provinceId } });
    return unwrap<GHNDistrict[]>(data);
  },
  wards: async (districtId: number) => {
    const { data } = await api.get<ApiResponse<GHNWard[]> | GHNWard[]>("/api/ghn/wards", { params: { districtId } });
    return unwrap<GHNWard[]>(data);
  }
};
