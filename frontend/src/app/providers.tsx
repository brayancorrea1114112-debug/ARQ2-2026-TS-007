"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { useSessionStore } from "@/features/auth/session";
import { MockProvider } from "@/mocks/MockProvider";

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 30_000,
            gcTime: 5 * 60_000,
            retry: 1,
            refetchOnWindowFocus: false,
          },
        },
      }),
  );

  useEffect(() => {
    useSessionStore.getState().markHydrated();
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      <MockProvider>{children}</MockProvider>
    </QueryClientProvider>
  );
}
