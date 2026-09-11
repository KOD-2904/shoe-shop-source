import { api, unwrap } from "./client";
import type { AddressRequest, ApiResponse, AuthResponse, UserResponse } from "../types";

export const authApi = {
  login: async (body: { identifier: string; password: string }) => {
    const { data } = await api.post<ApiResponse<AuthResponse>>("/auth/login", body);
    return unwrap<AuthResponse>(data);
  },
  register: async (body: { email: string; password: string; phone: string }) => {
    const { data } = await api.post<ApiResponse<unknown>>("/register", body);
    return data;
  },
  verifyEmail: async (token: string) => {
    const { data } = await api.get<ApiResponse<unknown>>("/auth/verify-email", { params: { token } });
    return data;
  },
  me: async () => {
    const { data } = await api.get<ApiResponse<UserResponse> | UserResponse>("/myinfor");
    return unwrap<UserResponse>(data);
  },
  logout: async (body?: { logoutAllDevices: boolean }) => {
    const { data } = await api.post<ApiResponse<unknown>>("/auth/log-out", body ?? {});
    return data;
  },
  changePassword: async (body: { currentPassword: string; newPassword: string }) => {
    const { data } = await api.post<ApiResponse<unknown>>("/auth/change-password", body);
    return data;
  },
  forgotPassword: async (body: { email: string }) => {
    const { data } = await api.post<ApiResponse<unknown>>("/auth/forgot-password", body);
    return data;
  },
  resetPassword: async (body: { token: string; newPassword: string }) => {
    const { data } = await api.post<ApiResponse<unknown>>("/auth/reset-password", body);
    return data;
  },
  addAddress: async (body: AddressRequest) => {
    const { data } = await api.post<ApiResponse<unknown>>("/address/add", body);
    return unwrap<unknown>(data);
  },
  users: async () => {
    const { data } = await api.get<ApiResponse<UserResponse[]> | UserResponse[]>("/users");
    return unwrap<UserResponse[]>(data);
  }
};
