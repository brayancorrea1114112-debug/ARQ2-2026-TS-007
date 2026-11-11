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
    !(
      profilesQuery.error instanceof ApiError &&
      profilesQuery.error.status === 401
    )
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
      <main id="main" className="papel flex-1">
        <div className="mx-auto w-full max-w-5xl px-6 py-14 lg:py-20">
          <div className="max-w-2xl animate-rise">
            <p className="text-sm font-medium text-azul">
              {account ? `Cuenta de ${account.email}` : "Tu cuenta"}
            </p>
            <h1 className="titular mt-3 text-6xl text-tinta sm:text-8xl">
              ¿Quién está viendo?
            </h1>
            <p className="mt-5 max-w-md text-base leading-relaxed text-tinta-suave">
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
                  className="h-28 animate-pulse rounded-hoja border border-regla bg-hoja-3"
                />
              ))}
            </div>
          ) : profiles.length === 0 ? (
            <p className="mt-10 max-w-md text-sm text-tinta-suave">
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
                    className="group relative flex w-full items-center gap-4 overflow-hidden rounded-hoja border border-regla bg-hoja-2 p-5 text-left transition-[border-color,transform] duration-150 hover:-translate-y-0.5 hover:border-azul active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0"
                    aria-label={`Entrar como ${profile.name}`}
                  >
                    <span
                      aria-hidden="true"
                      className={`absolute inset-y-0 left-0 w-1 ${
                        profile.type === "KIDS" ? "bg-menta" : "bg-azul"
                      }`}
                    />
                    <Avatar
                      name={profile.name}
                      src={profile.avatar_url}
                      tone={profile.type === "KIDS" ? "kids" : "adult"}
                      className="h-14 w-14 text-base"
                    />
                    <span className="flex min-w-0 flex-col gap-1">
                      <span className="titular text-3xl break-words text-tinta transition-colors duration-150 group-hover:text-azul">
                        {profile.name}
                      </span>
                      <span className="text-xs text-tinta-tenue">
                        {profile.type === "KIDS"
                          ? "Perfil infantil"
                          : "Perfil adulto"}
                      </span>
                    </span>
                    {pendingId === profile.profile_id ? (
                      <span className="ml-auto text-xs font-medium text-azul">
                        Abriendo…
                      </span>
                    ) : null}
                  </button>
                </li>
              ))}
            </ul>
          )}

          <p className="mt-10 max-w-md text-xs text-tinta-tenue">
            Los perfiles se administran desde la cuenta principal. Cada perfil
            tiene su propio historial y sus recomendaciones.
          </p>
        </div>
      </main>
    </>
  );
}
