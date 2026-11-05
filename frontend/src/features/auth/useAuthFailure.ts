"use client";

import { useRouter } from "next/navigation";
import { useCallback } from "react";
import { isApiError } from "@/shared/api/errors";
import { useSessionStore } from "@/features/auth/session";

export function useAuthFailure(): (error: unknown) => boolean {
  const router = useRouter();
  const logout = useSessionStore((state) => state.logout);
  const clearProfile = useSessionStore((state) => state.clearProfile);

  return useCallback(
    (error: unknown) => {
      if (!isApiError(error)) {
        return false;
      }
      if (error.status === 401) {
        logout();
        router.replace("/login");
        return true;
      }
      if (error.status === 403 && error.code === "PROFILE_NOT_SELECTED") {
        clearProfile();
        router.replace("/profiles");
        return true;
      }
      return false;
    },
    [logout, clearProfile, router],
  );
}
