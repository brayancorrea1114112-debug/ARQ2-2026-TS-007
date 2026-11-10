"use client";

import { useEffect, useState } from "react";
import { BootSplash } from "@/shared/ui/BootSplash";

export function MockBoot({ children }: { children: React.ReactNode }) {
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let active = true;
    import("@/mocks/browser")
      .then(({ startMockWorker }) => startMockWorker())
      .then(() => {
        if (active) {
          setReady(true);
        }
      })
      .catch((error: unknown) => {
        console.error("No se pudo iniciar MSW; se usará la red real.", error);
        if (active) {
          setReady(true);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  if (!ready) {
    return <BootSplash label="Preparando la sala" />;
  }

  return <>{children}</>;
}
