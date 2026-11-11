"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useSessionStore } from "@/features/auth/session";
import { BootSplash } from "@/shared/ui/BootSplash";

export default function IndexPage() {
  const router = useRouter();
  const hydrated = useSessionStore((state) => state.hydrated);
  const accountToken = useSessionStore((state) => state.accountToken);
  const profileToken = useSessionStore((state) => state.profileToken);

  useEffect(() => {
    if (!hydrated) {
      return;
    }
    if (profileToken) {
      router.replace("/home");
      return;
    }
    if (accountToken) {
      router.replace("/profiles");
      return;
    }
    router.replace("/login");
  }, [hydrated, accountToken, profileToken, router]);

  return <BootSplash label="Abriendo StreamHub" />;
}
