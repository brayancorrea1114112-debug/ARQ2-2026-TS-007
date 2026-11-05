"use client";

import dynamic from "next/dynamic";
import { USE_MOCKS } from "@/shared/api/config";
import { BootSplash } from "@/shared/ui/BootSplash";

const MockBoot = dynamic(
  () => import("@/mocks/MockBoot").then((module) => module.MockBoot),
  {
    ssr: false,
    loading: () => <BootSplash label="Preparando la sala" />,
  },
);

export function MockProvider({ children }: { children: React.ReactNode }) {
  if (!USE_MOCKS) {
    return <>{children}</>;
  }

  return <MockBoot>{children}</MockBoot>;
}
