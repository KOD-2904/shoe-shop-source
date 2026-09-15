import axios, { AxiosError, AxiosRequestConfig } from "axios";
import { API_BASE_URL } from "../config/env";
import { tokenStorage } from "../lib/storage";
import type { ApiResponse, TokenResponse } from "../types";

type RetryConfig = AxiosRequestConfig & { _retry?: boolean };
let refreshPromise: Promise<TokenResponse> | null = null;

export const api = axios.create({ baseURL: API_BASE_URL, withCredentials: true });

export function unwrap<T>(data: ApiResponse<T> | T): T {
  if (data && typeof data === "object" && "result" in data) {
    return (data as ApiResponse<T>).result as T;
  }
  return data as T;
}

export function getApiError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | undefined;
    return data?.message || error.response?.statusText || error.message;
  }
  return error instanceof Error ? error.message : "Co loi xay ra";
}

export async function refreshTokens() {
  if (!refreshPromise) {
    refreshPromise = axios
      .post<ApiResponse<TokenResponse> | TokenResponse>(`${API_BASE_URL}/auth/refreshToken`, null, { withCredentials: true })
      .then((response) => {
        const tokens = unwrap<TokenResponse>(response.data);
        tokenStorage.setAccessToken(tokens.accessToken);
        return tokens;
      })
      .catch((error) => {
        tokenStorage.clear();
        window.dispatchEvent(new Event("auth:logout"));
        throw error;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}

api.interceptors.request.use((config) => {
  const token = tokenStorage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetryConfig | undefined;
    if (error.response?.status !== 401 || !original || original._retry) {
      return Promise.reject(error);
    }

    original._retry = true;
    try {
      const tokens = await refreshTokens();
      original.headers = {
        ...original.headers,
        Authorization: `Bearer ${tokens.accessToken}`
      };
      return api(original);
    } catch (refreshError) {
      return Promise.reject(refreshError);
    }
  }
);
