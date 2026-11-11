"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useSessionStore } from "@/features/auth/session";
import { ScreenLoader } from "@/shared/ui/ScreenLoader";

export function RequireAccount({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const hydrated = useSessionStore((state) => state.hydrated);
  const accountToken = useSessionStore((state) => state.accountToken);

  useEffect(() => {
    if (hydrated && !accountToken) {
      router.replace("/login");
    }
  }, [hydrated, accountToken, router]);

  if (!hydrated || !accountToken) {
    return <ScreenLoader label="Comprobando tu cuenta" />;
  }

  return <>{children}</>;
}
