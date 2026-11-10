"use client";

import { useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { chooseProfile, listProfiles } from "@/features/auth/api";
import { useSessionStore } from "@/features/auth/session";
import { useAuthFailure } from "@/features/auth/useAuthFailure";
import { AppHeader } from "@/features/auth/AppHeader";
import { ApiError, isApiError } from "@/shared/api/errors";
import type { ProfileSummary } from "@/shared/api/contracts";
import { Avatar } from "@/shared/ui/Avatar";
import { ErrorNotice } from "@/shared/ui/ErrorNotice";

interface PickerError {
  message: string;
  correlationId: string | null;
}

export function ProfilePicker() {
  const router = useRouter();
  const accountToken = useSessionStore((state) => state.accountToken);
  const account = useSessionStore((state) => state.account);
  const storedProfiles = useSessionStore((state) => state.profiles);
  const setProfiles = useSessionStore((state) => state.setProfiles);
  const selectProfile = useSessionStore((state) => state.selectProfile);
  const authFailure = useAuthFailure();

  const [pendingId, setPendingId] = useState<string | null>(null);
  const [error, setError] = useState<PickerError | null>(null);

  const profilesQuery = useQuery({
    queryKey: ["profiles", accountToken],
    queryFn: () => listProfiles(accountToken as string),
    enabled: Boolean(accountToken),
  });

  useEffect(() => {
    if (profilesQuery.data) {
      setProfiles(profilesQuery.data.profiles);
    }
  }, [profilesQuery.data, setProfiles]);

  useEffect(() => {
    if (profilesQuery.error) {
      authFailure(profilesQuery.error);
    }
  }, [profilesQuery.error, authFailure]);

  const profiles = profilesQuery.data?.profiles ?? storedProfiles;
  const queryError =
    profilesQuery.error &&
    !(profilesQuery.error instanceof ApiError && profilesQuery.error.status === 401)
      ? profilesQuery.error instanceof ApiError
        ? {
            message: profilesQuery.error.message,
            correlationId: profilesQuery.error.correlationId,
          }
        : {
            message: "No pudimos cargar los perfiles.",
            correlationId: null,
          }
      : null;

  const handleSelect = async (profile: ProfileSummary) => {
    if (!accountToken) {
      return;
    }
    setPendingId(profile.profile_id);
    setError(null);
    try {
      const response = await chooseProfile(accountToken, profile.profile_id);
      selectProfile(response.access_token, response.profile);
      router.push("/home");
    } catch (caught) {
      if (authFailure(caught)) {
        return;
      }
      if (isApiError(caught)) {
        setError({
          message: caught.message,
          correlationId: caught.correlationId,
        });
      } else {
        setError({
          message: "No pudimos abrir el perfil. Inténtalo de nuevo.",
          correlationId: null,
        });
      }
    } finally {
      setPendingId(null);
    }
  };

  return (
    <>
      <AppHeader />
      <main id="main" className="mx-auto w-full max-w-5xl px-6 py-16">
        <div className="max-w-xl animate-rise">
          <p className="text-sm text-paper-muted">
            {account ? `Cuenta de ${account.email}` : "Tu cuenta"}
          </p>
          <h1 className="mt-3 font-display text-5xl leading-[0.98] text-paper">
            ¿Quién está viendo?
          </h1>
          <p className="mt-4 text-paper-muted">
            Elige un perfil para cargar su historial, su progreso y sus
            recomendaciones.
          </p>
        </div>

        {error ? (
          <ErrorNotice
            className="mt-8 max-w-xl"
            message={error.message}
            correlationId={error.correlationId}
          />
        ) : null}

        {queryError ? (
          <ErrorNotice
            className="mt-8 max-w-xl"
            message={queryError.message}
            correlationId={queryError.correlationId}
            onRetry={() => {
              void profilesQuery.refetch();
            }}
          />
        ) : null}

        {profilesQuery.isLoading ? (
          <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {[0, 1, 2].map((item) => (
              <div
                key={item}
                className="h-24 animate-pulse rounded-card border border-line bg-ink-900"
              />
            ))}
          </div>
        ) : profiles.length === 0 ? (
          <p className="mt-10 max-w-md text-sm text-paper-muted">
            Esta cuenta todavía no tiene perfiles. Crea uno desde la cuenta
            principal para empezar a ver.
          </p>
        ) : (
          <ul className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {profiles.map((profile) => (
              <li key={profile.profile_id}>
                <button
                  type="button"
                  onClick={() => handleSelect(profile)}
                  disabled={pendingId !== null}
                  className="group flex w-full items-center gap-4 rounded-card border border-line bg-ink-900 p-4 text-left transition-colors hover:border-brass disabled:cursor-not-allowed disabled:opacity-60"
                  aria-label={`Entrar como ${profile.name}`}
                >
                  <Avatar
                    name={profile.name}
                    src={profile.avatar_url}
                    tone={profile.type === "KIDS" ? "kids" : "adult"}
                  />
                  <span className="flex min-w-0 flex-col gap-1">
                    <span className="font-display text-2xl leading-none break-words text-paper group-hover:text-brass">
                      {profile.name}
                    </span>
                    <span className="text-xs text-paper-muted">
                      {profile.type === "KIDS"
                        ? "Perfil infantil"
                        : "Perfil adulto"}
                    </span>
                  </span>
                  {pendingId === profile.profile_id ? (
                    <span className="ml-auto text-xs text-brass">Abriendo…</span>
                  ) : null}
                </button>
              </li>
            ))}
          </ul>
        )}

        <p className="mt-10 max-w-md text-xs text-paper-muted">
          Los perfiles se administran desde la cuenta principal. Cada perfil
          tiene su propio historial y sus recomendaciones.
        </p>
      </main>
    </>
  );
}
