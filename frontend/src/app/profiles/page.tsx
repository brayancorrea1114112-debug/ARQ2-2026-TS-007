import type { Metadata } from "next";
import { ProfilePicker } from "@/features/auth/ProfilePicker";
import { RequireAccount } from "@/features/auth/RequireAccount";

export const metadata: Metadata = {
  title: "Perfiles",
};

export default function ProfilesPage() {
  return (
    <RequireAccount>
      <ProfilePicker />
    </RequireAccount>
  );
}
