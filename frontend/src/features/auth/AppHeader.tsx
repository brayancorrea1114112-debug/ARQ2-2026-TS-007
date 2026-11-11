"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useSessionStore } from "@/features/auth/session";
import { cn } from "@/shared/lib/cn";
import { Avatar } from "@/shared/ui/Avatar";
import { Wordmark } from "@/shared/ui/Wordmark";

const NAV = [
  { href: "/home", label: "Cartelera" },
  { href: "/clases", label: "Clases" },
];

export function AppHeader() {
  const router = useRouter();
  const pathname = usePathname();
  const account = useSessionStore((state) => state.account);
  const profile = useSessionStore((state) => state.profile);
  const clearProfile = useSessionStore((state) => state.clearProfile);
  const logout = useSessionStore((state) => state.logout);

  const signOut = () => {
    logout();
    router.replace("/login");
  };

  return (
    <header className="sticky top-0 z-30 border-b border-regla bg-hoja/90 backdrop-blur">
      <div className="mx-auto flex w-full max-w-6xl flex-wrap items-center gap-x-6 px-4 py-2 sm:gap-x-8 sm:px-6 sm:py-3">
        <Wordmark
          className="text-3xl"
          href={profile ? "/home" : "/profiles"}
        />

        {profile ? (
          <nav
            aria-label="Principal"
            className="order-last -ml-3 flex w-full items-center sm:order-none sm:ml-0 sm:w-auto"
          >
            {NAV.map((item) => {
              const active = pathname === item.href;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  aria-current={active ? "page" : undefined}
                  className={cn(
                    "relative flex min-h-11 items-center px-3 text-sm font-medium transition-colors duration-150",
                    active ? "text-tinta" : "text-tinta-suave hover:text-tinta",
                  )}
                >
                  {item.label}
                  {active ? (
                    <span
                      aria-hidden="true"
                      className="absolute inset-x-3 bottom-1 h-[3px] bg-marcador"
                    />
                  ) : null}
                </Link>
              );
            })}
          </nav>
        ) : null}

        <div className="ml-auto flex items-center gap-2">
          {profile ? (
            <Link
              href="/profiles"
              onClick={() => clearProfile()}
              className="flex min-h-11 items-center gap-3 rounded-full border border-regla bg-hoja-2 py-1 pr-4 pl-1 transition-colors duration-150 hover:border-azul"
              aria-label={`Perfil ${profile.name}, cambiar de perfil`}
            >
              <Avatar
                name={profile.name}
                src={profile.avatar_url}
                tone={profile.type === "KIDS" ? "kids" : "adult"}
                className="h-9 w-9 text-xs"
              />
              <span className="hidden text-sm font-medium text-tinta sm:inline">
                {profile.name}
              </span>
              <span className="hidden text-xs text-tinta-tenue sm:inline">
                Cambiar
              </span>
            </Link>
          ) : account ? (
            <span className="hidden text-sm text-tinta-suave sm:inline">
              {account.email}
            </span>
          ) : null}

          <button
            type="button"
            onClick={signOut}
            className="min-h-11 rounded-hoja px-3 text-sm text-tinta-suave transition-colors duration-150 hover:text-tinta"
          >
            Salir
          </button>
        </div>
      </div>
    </header>
  );
}
