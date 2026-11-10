import { apiRequest } from "@/shared/api/client";
import type {
  LoginResponse,
  ProfilesResponse,
  SelectProfileResponse,
} from "@/shared/api/contracts";

export function login(email: string, password: string): Promise<LoginResponse> {
  return apiRequest<LoginResponse>("/auth/login", {
    method: "POST",
    body: { email, password },
  });
}

export function listProfiles(token: string): Promise<ProfilesResponse> {
  return apiRequest<ProfilesResponse>("/profiles", { token });
}

export function chooseProfile(
  token: string,
  profileId: string,
): Promise<SelectProfileResponse> {
  return apiRequest<SelectProfileResponse>("/profiles/select", {
    method: "POST",
    token,
    body: { profile_id: profileId },
  });
}
