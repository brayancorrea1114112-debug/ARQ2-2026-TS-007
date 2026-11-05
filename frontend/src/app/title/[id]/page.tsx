import type { Metadata } from "next";
import { RequireProfile } from "@/features/auth/RequireProfile";
import { TitleScreen } from "@/features/catalog/TitleScreen";

export const metadata: Metadata = {
  title: "Título",
};

export default function TitlePage() {
  return (
    <RequireProfile>
      <TitleScreen />
    </RequireProfile>
  );
}
