import type { Metadata } from "next";
import { LoginScreen } from "@/features/auth/LoginScreen";

export const metadata: Metadata = {
  title: "Entrar",
};

export default function LoginPage() {
  return <LoginScreen />;
}
