"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useEffect } from "react";
import { AppHeader } from "@/features/auth/AppHeader";
import { useSessionStore } from "@/features/auth/session";
import { useAuthFailure } from "@/features/auth/useAuthFailure";
import { ContinueRail } from "@/features/home/ContinueRail";
import { TitleRail } from "@/features/home/TitleRail";
import { getHome } from "@/features/home/api";
import { featuredSelection } from "@/features/home/lookup";
import type { HomeView } from "@/shared/api/contracts";
import { ApiError } from "@/shared/api/errors";
import { buttonStyles } from "@/shared/ui/Button";
import { ErrorNotice } from "@/shared/ui/ErrorNotice";
import { Poster } from "@/shared/ui/Poster";
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
          <h1 className="font-display text-4xl text-paper">
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
          <section className="screen-grain border-b border-line px-6 py-16">
            <div className="mx-auto w-full max-w-6xl">
              <h1 className="font-display text-5xl text-paper">
                Buenas noches{profile ? `, ${profile.name}` : ""}.
              </h1>
              <p className="mt-4 max-w-lg text-paper-muted">
                Todavía no hay títulos en tu cartelera. Vuelve más tarde o
                explora otro perfil.
              </p>
            </div>
          </section>
        )}

        {!hasContent ? (
          <div className="mx-auto w-full max-w-6xl px-6 py-12">
            <p className="max-w-md text-sm text-paper-muted">
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
    <section className="relative overflow-hidden border-b border-line">
      <div
        aria-hidden="true"
        className="absolute inset-0"
        style={{
          background: `radial-gradient(120% 150% at 82% 8%, hsla(${hue}, 46%, 34%, 0.55), transparent 62%), linear-gradient(180deg, transparent 42%, var(--ink-950) 96%)`,
        }}
      />
      <span
        aria-hidden="true"
        className="pointer-events-none absolute -top-10 right-6 font-display text-[16rem] leading-none text-paper/[0.04]"
      >
        {title.name.charAt(0)}
      </span>

      <div className="relative mx-auto grid w-full max-w-6xl gap-10 px-6 py-14 lg:grid-cols-[1.32fr_0.68fr] lg:items-end lg:py-20">
        <div className="animate-rise">
          <p className="text-sm text-brass">
            {progress ? "Continúa donde lo dejaste" : "Tu cartelera de esta noche"}
          </p>
          <h1 className="mt-3 max-w-2xl font-display text-5xl leading-[0.94] text-paper sm:text-6xl">
            {title.name}
          </h1>

          {title.synopsis ? (
            <p className="mt-5 max-w-xl text-base leading-relaxed text-paper-muted">
              {title.synopsis}
            </p>
          ) : null}

          <div className="mt-5 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-paper-muted">
            {title.age_rating ? <span>{title.age_rating}</span> : null}
            {duration ? <span>{duration}</span> : null}
            {title.genres?.length ? (
              <span>{title.genres.join(" · ")}</span>
            ) : null}
            <span>
              {home.continue_watching.length} en curso ·{" "}
              {home.recommended.length} recomendados
            </span>
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

          {progress ? (
            <div className="mt-7 max-w-sm">
              <div className="flex items-center justify-between text-xs text-paper-muted tabular-nums">
                <span>{formatTime(progress.progress_seconds)} vistos</span>
                {duration ? <span>{duration}</span> : null}
              </div>
              <div
                className="mt-2 h-1 w-full bg-ink-800"
                role="progressbar"
                aria-label="Progreso del título destacado"
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={Math.round(percent ?? 0)}
              >
                <div
                  className="h-full bg-brass"
                  style={{ width: `${percent ?? 0}%` }}
                />
              </div>
            </div>
          ) : null}
        </div>

        <div className="hidden lg:block">
          <div className="mx-auto w-56 rotate-2">
            <Poster
              contentId={title.content_id}
              name={title.name}
              poster={title.poster}
            />
            <p className="mt-3 text-center text-xs text-paper-muted">
              {progress ? "En curso" : "Selección de la casa"}
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}

function HomeFooter({ home }: { home: HomeView }) {
  return (
    <footer className="mx-auto mt-6 w-full max-w-6xl px-6">
      <div className="border-t border-line pt-4 text-xs text-paper-muted">
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
        <div className="h-[380px] border-b border-line bg-ink-900/40" />
        <div className="mx-auto w-full max-w-6xl space-y-12 px-6 py-10">
          {[0, 1].map((row) => (
            <div key={row}>
              <div className="h-6 w-56 rounded-card bg-ink-800" />
              <div className="mt-5 flex gap-4">
                {[0, 1, 2, 3, 4].map((item) => (
                  <div
                    key={item}
                    className="aspect-[2/3] w-[148px] shrink-0 rounded-card border border-line bg-ink-900 sm:w-[168px]"
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
