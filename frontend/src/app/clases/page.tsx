import type { Metadata } from "next";
import { RequireProfile } from "@/features/auth/RequireProfile";
import { ClasesScreen } from "@/features/clases/ClasesScreen";

export const metadata: Metadata = {
  title: "Clases",
};

export default function ClasesPage() {
  return (
    <RequireProfile>
      <ClasesScreen />
    </RequireProfile>
  );
}
