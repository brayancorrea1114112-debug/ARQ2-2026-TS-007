import type { Metadata } from "next";
import { RequireProfile } from "@/features/auth/RequireProfile";
import { HomeScreen } from "@/features/home/HomeScreen";

export const metadata: Metadata = {
  title: "Inicio",
};

export default function HomePage() {
  return (
    <RequireProfile>
      <HomeScreen />
    </RequireProfile>
  );
}
