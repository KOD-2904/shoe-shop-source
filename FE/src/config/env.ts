const rawApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();

if (!rawApiBaseUrl) {
  throw new Error("Missing VITE_API_BASE_URL in FE environment");
}

export const API_BASE_URL = rawApiBaseUrl.replace(/\/+$/, "");
