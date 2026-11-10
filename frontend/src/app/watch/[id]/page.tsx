import type { Metadata } from "next";
import { Suspense } from "react";
import { RequireProfile } from "@/features/auth/RequireProfile";
import { WatchScreen } from "@/features/playback/WatchScreen";
import { ScreenLoader } from "@/shared/ui/ScreenLoader";

export const metadata: Metadata = {
  title: "Reproductor",
};

export default function WatchPage() {
  return (
    <RequireProfile>
      <Suspense fallback={<ScreenLoader label="Preparando el reproductor" />}>
        <WatchScreen />
      </Suspense>
    </RequireProfile>
  );
}
