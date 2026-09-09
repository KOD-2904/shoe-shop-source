import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { refreshTokens } from "../api/client";
import { authApi } from "../api/authApi";
import { cacheTimes, queryKeys } from "../lib/queryCache";
import { tokenStorage } from "../lib/storage";
import type { UserResponse } from "../types";

type AuthContextValue = {
  user: UserResponse | null;
  initializing: boolean;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (identifier: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: (options?: { force?: boolean }) => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [user, setUser] = useState<UserResponse | null>(() => tokenStorage.getUser());
  const [initializing, setInitializing] = useState(true);

  const refreshUser = useCallback(async (options?: { force?: boolean }) => {
    if (options?.force) {
      queryClient.removeQueries({ queryKey: queryKeys.profile });
    }
    const nextUser = await queryClient.fetchQuery({
      queryKey: queryKeys.profile,
      queryFn: authApi.me,
      ...cacheTimes.userProfile
    });
    tokenStorage.setUser(nextUser);
    setUser(nextUser);
  }, [queryClient]);

  const clearSession = useCallback(() => {
    tokenStorage.clear();
    queryClient.removeQueries({ queryKey: queryKeys.profile });
    setUser(null);
  }, [queryClient]);

  useEffect(() => {
    // OAuth2SuccessPage owns the callback flow. For every other entry point,
    // restore the session once when the provider mounts, not on route changes.
    if (window.location.pathname === "/oauth2/success") {
      setInitializing(false);
      return;
    }

    const boot = async () => {
      try {
        await refreshTokens();
        await refreshUser();
      } catch {
        clearSession();
      } finally {
        setInitializing(false);
      }
    };
    void boot();
  }, [clearSession, refreshUser]);

  useEffect(() => {
    window.addEventListener("auth:logout", clearSession);
    return () => window.removeEventListener("auth:logout", clearSession);
  }, [clearSession]);

  const login = useCallback(
    async (identifier: string, password: string) => {
      const auth = await authApi.login({ identifier, password });
      tokenStorage.setAccessToken(auth.accessToken);
      await refreshUser({ force: true });
    },
    [refreshUser]
  );

  const logout = useCallback(async () => {
    try {
      await authApi.logout({ logoutAllDevices: false });
    } finally {
      clearSession();
    }
  }, [clearSession]);

  const value = useMemo(
    () => ({
      user,
      initializing,
      isAuthenticated: Boolean(user && tokenStorage.getAccessToken()),
      isAdmin: Boolean(user?.roles?.some((role) => role === "ROLE_ADMIN" || role === "ADMIN")),
      login,
      logout,
      refreshUser
    }),
    [initializing, login, logout, refreshUser, user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
