"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect } from "react";
import { AppHeader } from "@/features/auth/AppHeader";
import { useSessionStore } from "@/features/auth/session";
import { useAuthFailure } from "@/features/auth/useAuthFailure";
import { getHome } from "@/features/home/api";
import {
  continueWatchingFor,
  titleOrStub,
} from "@/features/home/lookup";
import { RatingButtons } from "@/features/interactions/RatingButtons";
import { ApiError } from "@/shared/api/errors";
import { buttonStyles } from "@/shared/ui/Button";
import { ErrorNotice } from "@/shared/ui/ErrorNotice";
import { Poster } from "@/shared/ui/Poster";
import { ScreenLoader } from "@/shared/ui/ScreenLoader";
import {
  formatDuration,
  formatTime,
  progressPercent,
} from "@/shared/lib/format";

export function TitleScreen() {
  const params = useParams<{ id: string | string[] }>();
  const contentId = Array.isArray(params.id) ? params.id[0] : params.id;
  const token = useSessionStore((state) => state.profileToken);
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
    return (
      <>
        <AppHeader />
        <ScreenLoader label="Buscando el título" />
      </>
    );
  }

  if (homeQuery.error) {
    const apiError =
      homeQuery.error instanceof ApiError ? homeQuery.error : null;
    return (
      <>
        <AppHeader />
        <main id="main" className="mx-auto w-full max-w-3xl px-6 py-16">
          <h1 className="font-display text-4xl text-paper">
            No pudimos cargar este título
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
  if (!home || !contentId) {
    return (
      <>
        <AppHeader />
        <main id="main" className="mx-auto w-full max-w-3xl px-6 py-16">
          <h1 className="font-display text-4xl text-paper">
            No encontramos este título
          </h1>
          <Link
            href="/home"
            className={buttonStyles({ variant: "secondary", className: "mt-6" })}
          >
            Volver al inicio
          </Link>
        </main>
      </>
    );
  }

  const title = titleOrStub(home, contentId);
  const progress = continueWatchingFor(home, contentId);
  const duration = formatDuration(title.duration_seconds);
  const percent =
    progress && title.duration_seconds
      ? progressPercent(progress.progress_seconds, title.duration_seconds)
      : null;
  const hasMetadata = Boolean(
    title.age_rating || duration || title.genres?.length,
  );
  const watchHref = progress
    ? `/watch/${title.content_id}?t=${progress.progress_seconds}`
    : `/watch/${title.content_id}`;

  return (
    <>
      <AppHeader />
      <main id="main" className="mx-auto w-full max-w-5xl px-6 py-10">
        <Link
          href="/home"
          className="text-sm text-paper-muted transition-colors hover:text-paper"
        >
          ← Volver al inicio
        </Link>

        <div className="mt-8 grid gap-10 md:grid-cols-[220px_1fr] md:items-start">
          <div className="mx-auto w-40 sm:w-52 md:mx-0 md:w-full">
            <Poster
              contentId={title.content_id}
              name={title.name}
              poster={title.poster}
              progress={percent}
            />
          </div>

          <div className="animate-rise">
            <h1 className="font-display text-5xl leading-[0.96] text-paper">
              {title.name}
            </h1>

            {hasMetadata ? (
              <div className="mt-4 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-paper-muted tabular-nums">
                {title.age_rating ? <span>{title.age_rating}</span> : null}
                {duration ? <span>{duration}</span> : null}
                {title.genres?.length ? (
                  <span>{title.genres.join(" · ")}</span>
                ) : null}
              </div>
            ) : null}

            <p className="mt-6 max-w-2xl text-base leading-relaxed text-paper-muted">
              {title.synopsis ??
                "La sinopsis de este título todavía no está en el catálogo."}
            </p>

            {progress ? (
              <div className="mt-6 max-w-md">
                <div className="flex items-center justify-between text-xs text-paper-muted tabular-nums">
                  <span>{formatTime(progress.progress_seconds)} vistos</span>
                  {duration ? <span>{duration}</span> : null}
                </div>
                <div
                  className="mt-2 h-1 w-full bg-ink-800"
                  role="progressbar"
                  aria-label="Progreso guardado"
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

            <div className="mt-8 flex flex-wrap items-center gap-3">
              <Link href={watchHref} className={buttonStyles()}>
                {progress ? "Reanudar" : "Reproducir"}
              </Link>
              <RatingButtons contentId={title.content_id} />
            </div>
          </div>
        </div>
      </main>
    </>
  );
}
