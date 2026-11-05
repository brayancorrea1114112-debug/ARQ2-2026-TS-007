"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useSessionStore } from "@/features/auth/session";
import { ScreenLoader } from "@/shared/ui/ScreenLoader";

export function RequireProfile({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const hydrated = useSessionStore((state) => state.hydrated);
  const accountToken = useSessionStore((state) => state.accountToken);
  const profileToken = useSessionStore((state) => state.profileToken);

  useEffect(() => {
    if (!hydrated) {
      return;
    }
    if (!accountToken) {
      router.replace("/login");
      return;
    }
    if (!profileToken) {
      router.replace("/profiles");
    }
  }, [hydrated, accountToken, profileToken, router]);

  if (!hydrated || !accountToken || !profileToken) {
    return <ScreenLoader label="Cargando tu perfil" />;
  }

  return <>{children}</>;
}
