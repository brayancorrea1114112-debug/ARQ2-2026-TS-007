"use client";

import { useQuery } from "@tanstack/react-query";
import { useEffect } from "react";
import { AppHeader } from "@/features/auth/AppHeader";
import { useSessionStore } from "@/features/auth/session";
import { useAuthFailure } from "@/features/auth/useAuthFailure";
import { ClasesError } from "@/features/clases/api";
import { ClaseCard } from "@/features/clases/ClaseCard";
import { resumeByClase } from "@/features/clases/ClasesRail";
import { SemesterRuler } from "@/features/clases/SemesterRuler";
import { useClases } from "@/features/clases/useClases";
import { getHome } from "@/features/home/api";
import { ErrorNotice } from "@/shared/ui/ErrorNotice";
import { ScreenLoader } from "@/shared/ui/ScreenLoader";

export function ClasesScreen() {
  const token = useSessionStore((state) => state.profileToken);
  const authFailure = useAuthFailure();
  const clasesQuery = useClases();

  // El progreso guardado viene de Home; si falla, las clases siguen siendo visibles.
  const homeQuery = useQuery({
    queryKey: ["home", token],
    queryFn: () => getHome(token as string),
    enabled: Boolean(token),
  });

  useEffect(() => {
    if (homeQuery.error) {
      authFailure(homeQuery.error);
    }
  }, [homeQuery.error, authFailure]);

  const clases = clasesQuery.data ?? [];
  const resume = resumeByClase(homeQuery.data);
  const error =
    clasesQuery.error instanceof ClasesError ? clasesQuery.error : null;

  return (
    <>
      <AppHeader />
      <main id="main" className="flex-1 pb-16">
        <section className="papel border-b border-regla">
          <div className="mx-auto w-full max-w-6xl px-6 py-10 lg:py-14">
            <div className="animate-rise max-w-2xl">
              <p className="text-sm font-medium text-azul">
                Arquitectura de Software II
              </p>
              <h1 className="titular mt-3 text-6xl text-tinta sm:text-8xl">
                Clases
              </h1>
              <p className="mt-5 max-w-lg text-base leading-relaxed text-tinta-suave">
                Las grabaciones del semestre, en el orden en que ocurrieron.
                Retoma cualquier clase donde la dejaste.
              </p>
            </div>
          </div>
        </section>

        <div className="mx-auto w-full max-w-6xl px-6 pt-8">
          {clasesQuery.isLoading ? (
            <ScreenLoader label="Buscando las clases" />
          ) : error ? (
            <ErrorNotice
              className="max-w-xl"
              message={error.message}
              correlationId={error.correlationId}
              onRetry={() => {
                void clasesQuery.refetch();
              }}
            />
          ) : clases.length === 0 ? (
            <div className="max-w-md">
              <h2 className="titular text-3xl text-tinta">
                Aún no hay clases
              </h2>
              <p className="mt-3 text-sm text-tinta-suave">
                Copia las grabaciones (.mp4) a la carpeta de clases y recarga
                esta página.
              </p>
            </div>
          ) : (
            <>
              <SemesterRuler clases={clases} />
              <ul className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
                {clases.map((clase) => (
                  <li key={clase.id}>
                    <ClaseCard
                      clase={clase}
                      resumeSeconds={resume[clase.id]}
                      className="h-full"
                    />
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      </main>
    </>
  );
}
