"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useSessionStore } from "@/features/auth/session";
import { Avatar } from "@/shared/ui/Avatar";
import { Wordmark } from "@/shared/ui/Wordmark";

export function AppHeader() {
  const router = useRouter();
  const account = useSessionStore((state) => state.account);
  const profile = useSessionStore((state) => state.profile);
  const clearProfile = useSessionStore((state) => state.clearProfile);
  const logout = useSessionStore((state) => state.logout);

  const signOut = () => {
    logout();
    router.replace("/login");
  };

  return (
    <header className="border-b border-line">
      <div className="mx-auto flex w-full max-w-6xl items-center justify-between gap-4 px-6 py-4">
        <Wordmark className="text-2xl" href={profile ? "/home" : "/profiles"} />

        <div className="flex items-center gap-3">
          {profile ? (
            <Link
              href="/profiles"
              onClick={() => clearProfile()}
              className="flex items-center gap-3 rounded-card border border-line bg-ink-900 py-1 pr-3 pl-1 transition-colors hover:border-brass"
              aria-label={`Perfil ${profile.name}, cambiar de perfil`}
            >
              <Avatar
                name={profile.name}
                src={profile.avatar_url}
                tone={profile.type === "KIDS" ? "kids" : "adult"}
                className="h-9 w-9 text-xs"
              />
              <span className="text-sm text-paper">{profile.name}</span>
              <span className="text-xs text-paper-muted">Cambiar</span>
            </Link>
          ) : account ? (
            <span className="hidden text-sm text-paper-muted sm:inline">
              {account.email}
            </span>
          ) : null}

          <button
            type="button"
            onClick={signOut}
            className="min-h-11 rounded-card px-3 text-sm text-paper-muted transition-colors hover:text-paper"
          >
            Salir
          </button>
        </div>
      </div>
    </header>
  );
}
