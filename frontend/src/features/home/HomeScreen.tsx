"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useEffect } from "react";
import { AppHeader } from "@/features/auth/AppHeader";
import { useSessionStore } from "@/features/auth/session";
import { useAuthFailure } from "@/features/auth/useAuthFailure";
import { ClasesRail } from "@/features/clases/ClasesRail";
import { ContinueRail } from "@/features/home/ContinueRail";
import { TitleRail } from "@/features/home/TitleRail";
import { getHome } from "@/features/home/api";
import { featuredSelection } from "@/features/home/lookup";
import type { HomeView } from "@/shared/api/contracts";
import { ApiError } from "@/shared/api/errors";
import { buttonStyles } from "@/shared/ui/Button";
import { ErrorNotice } from "@/shared/ui/ErrorNotice";
import {
  formatDateTime,
  formatDuration,
  formatTime,
  hueFromId,
  progressPercent,
} from "@/shared/lib/format";

export function HomeScreen() {
  const token = useSessionStore((state) => state.profileToken);
  const profile = useSessionStore((state) => state.profile);
  const authFailure = useAuthFailure();

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

  if (homeQuery.isLoading) {
    return <HomeSkeleton />;
  }

  if (homeQuery.error) {
    const apiError =
      homeQuery.error instanceof ApiError ? homeQuery.error : null;
    return (
      <>
        <AppHeader />
        <main id="main" className="mx-auto w-full max-w-3xl px-6 py-16">
          <h1 className="titular text-5xl text-tinta">
            No pudimos abrir tu cartelera
          </h1>
          <ErrorNotice
            className="mt-6"
            message={apiError?.message ?? "Ocurrió un error inesperado."}
            correlationId={apiError?.correlationId}
            onRetry={() => {
              void homeQuery.refetch();
            }}
          />
        </main>
      </>
    );
  }

  const home = homeQuery.data;
  if (!home) {
    return null;
  }

  const featured = featuredSelection(home);
  const hasContent =
    featured !== null ||
    home.recommended.length > 0 ||
    home.categories.some((category) => category.titles.length > 0);

  return (
    <>
      <AppHeader />
      <main id="main" className="flex-1 pb-12">
        {featured ? (
          <HomeHero home={home} featured={featured} />
        ) : (
          <section className="papel border-b border-regla px-6 py-16">
            <div className="mx-auto w-full max-w-6xl">
              <h1 className="titular text-6xl text-tinta sm:text-7xl">
                Buenas noches{profile ? `, ${profile.name}` : ""}.
              </h1>
              <p className="mt-4 max-w-lg text-tinta-suave">
                Todavía no hay títulos en tu cartelera. Vuelve más tarde o
                explora otro perfil.
              </p>
            </div>
          </section>
        )}

        {!hasContent ? (
          <div className="mx-auto w-full max-w-6xl px-6 py-12">
            <p className="max-w-md text-sm text-tinta-suave">
              Cuando veas o califiques títulos, aparecerán aquí.
            </p>
            <Link
              href="/profiles"
              className={buttonStyles({ variant: "secondary", className: "mt-4" })}
            >
              Cambiar de perfil
            </Link>
          </div>
        ) : (
          <>
            <ContinueRail home={home} />
            <ClasesRail home={home} />
            <TitleRail
              heading="Recomendado para ti"
              titles={home.recommended}
              emptyMessage="Califica algunos títulos para empezar a recibir recomendaciones."
            />
            {home.categories.map((category) => (
              <TitleRail
                key={category.name}
                heading={category.name}
                titles={category.titles}
              />
            ))}
          </>
        )}

        {!hasContent ? <ClasesRail home={home} /> : null}

        <HomeFooter home={home} />
      </main>
    </>
  );
}

function HomeHero({
  home,
  featured,
}: {
  home: HomeView;
  featured: NonNullable<ReturnType<typeof featuredSelection>>;
}) {
  const { title, progress } = featured;
  const hue = hueFromId(title.content_id);
  const duration = formatDuration(title.duration_seconds);
  const percent =
    progress && title.duration_seconds
      ? progressPercent(progress.progress_seconds, title.duration_seconds)
      : null;
  const watchHref = progress
    ? `/watch/${title.content_id}?t=${progress.progress_seconds}`
    : `/watch/${title.content_id}`;

  return (
    <section className="papel border-b border-regla">
      <div className="mx-auto grid w-full max-w-6xl gap-10 px-6 py-10 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.1fr)] lg:items-center lg:py-14">
        <div className="animate-rise">
          <p className="text-sm font-medium text-azul">
            {progress ? "Continúa donde lo dejaste" : "Tu cartelera de esta noche"}
          </p>
          <h1 className="titular mt-3 text-6xl text-tinta sm:text-8xl">
            {title.name}
          </h1>

          {title.synopsis ? (
            <p className="mt-5 max-w-md text-base leading-relaxed text-tinta-suave">
              {title.synopsis}
            </p>
          ) : null}

          <div className="mt-5 flex flex-wrap items-center gap-x-5 gap-y-2 text-sm text-tinta-suave tabular-nums">
            {title.age_rating ? (
              <span className="rounded-[3px] border border-regla-fuerte px-1.5 py-0.5 text-xs font-medium text-tinta">
                {title.age_rating}
              </span>
            ) : null}
            {duration ? <span>{duration}</span> : null}
            {title.genres?.length ? <span>{title.genres.join(", ")}</span> : null}
          </div>

          <div className="mt-7 flex flex-wrap gap-3">
            <Link href={watchHref} className={buttonStyles()}>
              {progress ? "Reanudar" : "Reproducir"}
            </Link>
            <Link
              href={`/title/${title.content_id}`}
              className={buttonStyles({ variant: "secondary" })}
            >
              Ver detalles
            </Link>
          </div>

          <dl className="mt-8 flex gap-8 text-sm">
            <div>
              <dt className="text-xs text-tinta-tenue">En curso</dt>
              <dd className="titular mt-1 text-4xl text-tinta tabular-nums">
                {home.continue_watching.length}
              </dd>
            </div>
            <div>
              <dt className="text-xs text-tinta-tenue">Recomendados</dt>
              <dd className="titular mt-1 text-4xl text-tinta tabular-nums">
                {home.recommended.length}
              </dd>
            </div>
          </dl>
        </div>

        <Link
          href={watchHref}
          tabIndex={-1}
          aria-hidden="true"
          className="pizarra-zona group relative block aspect-video overflow-hidden rounded-hoja bg-pizarra text-tiza"
          style={{
            backgroundImage: `radial-gradient(110% 140% at 85% 0%, hsla(${hue}, 72%, 52%, 0.6), transparent 62%), linear-gradient(rgba(241, 243, 255, 0.05) 1px, transparent 1px), linear-gradient(90deg, rgba(241, 243, 255, 0.05) 1px, transparent 1px)`,
            backgroundSize: "auto, 32px 32px, 32px 32px",
          }}
        >
          <span className="titular absolute -bottom-6 left-4 text-[14rem] leading-none text-tiza/15">
            {title.name.charAt(0)}
          </span>

          <span className="absolute top-1/2 left-1/2 flex h-20 w-20 -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded-full bg-tiza text-pizarra transition-transform duration-150 group-hover:scale-105">
            <svg viewBox="0 0 24 24" width="30" height="30">
              <path d="M8 5.5v13l11-6.5-11-6.5Z" fill="currentColor" />
            </svg>
          </span>

          {progress ? (
            <span className="absolute inset-x-5 bottom-4 block">
              <span className="flex justify-between text-xs text-tiza/80 tabular-nums">
                <span>{formatTime(progress.progress_seconds)}</span>
                {duration ? <span>{duration}</span> : null}
              </span>
              <span className="relative mt-2 block h-[3px] bg-tiza/25">
                <span
                  className="absolute inset-y-0 left-0 bg-tiza/60"
                  style={{ width: `${percent ?? 0}%` }}
                />
                <span
                  className="absolute -top-1.5 h-[15px] w-[3px] bg-marcador"
                  style={{ left: `${percent ?? 0}%` }}
                />
              </span>
            </span>
          ) : null}
        </Link>
      </div>
    </section>
  );
}

function HomeFooter({ home }: { home: HomeView }) {
  return (
    <footer className="mx-auto mt-6 w-full max-w-6xl px-6">
      <div className="border-t border-regla pt-4 text-xs text-tinta-tenue">
        <p>
          Cartelera generada el{" "}
          <time dateTime={home.generated_at} className="tabular-nums">
            {formatDateTime(home.generated_at)} UTC
          </time>
          . Servida desde{" "}
          {home.source === "cache" ? "caché" : "el respaldo del catálogo"}.
        </p>
      </div>
    </footer>
  );
}

function HomeSkeleton() {
  return (
    <>
      <AppHeader />
      <main id="main" className="flex-1 animate-pulse">
        <div className="papel h-[380px] border-b border-regla" />
        <div className="mx-auto w-full max-w-6xl space-y-12 px-6 py-10">
          {[0, 1].map((row) => (
            <div key={row}>
              <div className="h-8 w-56 rounded-hoja bg-hoja-3" />
              <div className="mt-5 flex gap-4">
                {[0, 1, 2, 3, 4].map((item) => (
                  <div
                    key={item}
                    className="aspect-[2/3] w-[148px] shrink-0 rounded-hoja bg-hoja-3 sm:w-[168px]"
                  />
                ))}
              </div>
            </div>
          ))}
        </div>
      </main>
    </>
  );
}
