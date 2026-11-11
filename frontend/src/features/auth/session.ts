import { create } from "zustand";
import { createJSONStorage, persist, type StateStorage } from "zustand/middleware";
import type {
  AccountSummary,
  ProfileSummary,
} from "@/shared/api/contracts";

interface SessionState {
  accountToken: string | null;
  account: AccountSummary | null;
  profiles: ProfileSummary[];
  profileToken: string | null;
  profile: ProfileSummary | null;
  hydrated: boolean;
  setAccount: (token: string, account: AccountSummary) => void;
  setProfiles: (profiles: ProfileSummary[]) => void;
  selectProfile: (token: string, profile: ProfileSummary) => void;
  clearProfile: () => void;
  logout: () => void;
  markHydrated: () => void;
}

const noopStorage: StateStorage = {
  getItem: () => null,
  setItem: () => undefined,
  removeItem: () => undefined,
};

export const useSessionStore = create<SessionState>()(
  persist(
    (set) => ({
      accountToken: null,
      account: null,
      profiles: [],
      profileToken: null,
      profile: null,
      hydrated: false,
      setAccount: (token, account) =>
        set({
          accountToken: token,
          account,
          profiles: [],
          profileToken: null,
          profile: null,
        }),
      setProfiles: (profiles) => set({ profiles }),
      selectProfile: (token, profile) =>
        set({ profileToken: token, profile }),
      clearProfile: () => set({ profileToken: null, profile: null }),
      logout: () =>
        set({
          accountToken: null,
          account: null,
          profiles: [],
          profileToken: null,
          profile: null,
        }),
      markHydrated: () => set({ hydrated: true }),
    }),
    {
      name: "streamhub.session",
      storage: createJSONStorage(() =>
        typeof window === "undefined" ? noopStorage : window.sessionStorage,
      ),
      partialize: (state) => ({
        accountToken: state.accountToken,
        account: state.account,
        profiles: state.profiles,
        profileToken: state.profileToken,
        profile: state.profile,
      }),
      onRehydrateStorage: () => (state) => {
        state?.markHydrated();
      },
    },
  ),
);
