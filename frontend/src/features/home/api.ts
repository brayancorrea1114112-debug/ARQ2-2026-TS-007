import { apiRequest } from "@/shared/api/client";
import { normalizeHome, type HomeView } from "@/shared/api/contracts";

export async function getHome(token: string): Promise<HomeView> {
  const raw = await apiRequest<unknown>("/home", { token });
  return normalizeHome(raw);
}
