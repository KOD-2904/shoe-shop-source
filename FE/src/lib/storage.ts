import type { UserResponse } from "../types";

let accessToken: string | null = null;
let user: UserResponse | null = null;

const LEGACY_KEYS = ["shoe_shop_access_token", "shoe_shop_refresh_token", "shoe_shop_user"];

LEGACY_KEYS.forEach((key) => localStorage.removeItem(key));

function decodeJwtPayload(token: string): { exp?: number } | null {
  try {
    const payload = token.split(".")[1];
    if (!payload) return null;
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(normalized));
  } catch {
    return null;
  }
}

export const tokenStorage = {
  getAccessToken: () => accessToken,
  isAccessTokenExpiring: (skewSeconds = 30) => {
    const token = accessToken;
    if (!token) return true;
    const payload = decodeJwtPayload(token);
    if (!payload?.exp) return true;
    return payload.exp * 1000 <= Date.now() + skewSeconds * 1000;
  },
  setAccessToken: (nextAccessToken: string) => {
    accessToken = nextAccessToken;
  },
  getUser: () => user,
  setUser: (nextUser: UserResponse) => {
    user = nextUser;
  },
  clear: () => {
    accessToken = null;
    user = null;
    LEGACY_KEYS.forEach((key) => localStorage.removeItem(key));
  }
};
