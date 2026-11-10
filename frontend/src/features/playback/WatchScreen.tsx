"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useParams, useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { useSessionStore } from "@/features/auth/session";
import { useAuthFailure } from "@/features/auth/useAuthFailure";
import { getHome } from "@/features/home/api";
import {
  continueWatchingFor,
  titleOrStub,
} from "@/features/home/lookup";
import { RatingButtons } from "@/features/interactions/RatingButtons";
import {
  usePlaybackTelemetry,
  type TelemetryStatus,
} from "@/features/telemetry/usePlaybackTelemetry";
import type {
  ContinueWatchingItem,
  HomeTitle,
} from "@/shared/api/contracts";
import { ApiError } from "@/shared/api/errors";
import { buttonStyles } from "@/shared/ui/Button";
import { ErrorNotice } from "@/shared/ui/ErrorNotice";
import { ScreenLoader } from "@/shared/ui/ScreenLoader";
import { Wordmark } from "@/shared/ui/Wordmark";
import {
  formatDuration,
  formatTime,
  hueFromId,
} from "@/shared/lib/format";

const FALLBACK_DURATION_SECONDS = 5400;

export function WatchScreen() {
  const params = useParams<{ id: string | string[] }>();
  const searchParams = useSearchParams();
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
        <PlayerTopBar contentId={contentId} />
        <ScreenLoader label="Preparando el reproductor" />
      </>
    );
  }

  if (homeQuery.error) {
    const apiError =
      homeQuery.error instanceof ApiError ? homeQuery.error : null;
    return (
      <>
        <PlayerTopBar contentId={contentId} />
        <main id="main" className="mx-auto w-full max-w-3xl px-6 py-16">
          <h1 className="font-display text-4xl text-paper">
            No pudimos abrir el reproductor
          </h1>
          <ErrorNotice
            className="mt-6"
            message={apiError?.message ?? "Ocurrió un error inesperado."}
            correlationId={apiError?.correlationId}
            onRetry={() => {
              void homeQuery.refetch();
            }}
          />
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

  const home = homeQuery.data;
  if (!home || !contentId) {
    return (
      <>
        <PlayerTopBar contentId={contentId} />
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
  const resume = continueWatchingFor(home, contentId);
  const fromQuery = Number(searchParams.get("t"));
  const initialProgress =
    Number.isFinite(fromQuery) && fromQuery > 0
      ? fromQuery
      : (resume?.progress_seconds ?? 0);

  return (
    <Player
      contentId={contentId}
      token={token as string}
      title={title}
      resume={resume}
      initialProgress={initialProgress}
    />
  );
}

function Player({
  contentId,
  token,
  title,
  resume,
  initialProgress,
}: {
  contentId: string;
  token: string;
  title: HomeTitle;
  resume: ContinueWatchingItem | null;
  initialProgress: number;
}) {
  const duration = title.duration_seconds ?? FALLBACK_DURATION_SECONDS;
  const [progress, setProgress] = useState(() =>
    Math.min(Math.max(0, initialProgress), duration),
  );
  const [playing, setPlaying] = useState(false);
  const progressRef = useRef(progress);

  useEffect(() => {
    if (!playing) {
      return;
    }
    const timer = setInterval(() => {
      const next = Math.min(duration, progressRef.current + 1);
      progressRef.current = next;
      setProgress(next);
      if (next >= duration) {
        setPlaying(false);
      }
    }, 1000);
    return () => clearInterval(timer);
  }, [playing, duration]);

  const telemetry = usePlaybackTelemetry({
    token,
    contentId,
    episodeId: resume?.episode_id,
    playing,
    progressSeconds: progress,
  });

  const hue = hueFromId(contentId);
  const total = formatTime(duration);
  const durationLabel = formatDuration(title.duration_seconds);
  const hasMetadata = Boolean(
    title.age_rating || durationLabel || resume?.episode_id,
  );

  const seek = (value: number) => {
    const next = Math.min(duration, Math.max(0, value));
    progressRef.current = next;
    setProgress(next);
  };

  const togglePlayback = () => {
    if (!playing && progressRef.current >= duration) {
      progressRef.current = 0;
      setProgress(0);
    }
    setPlaying(!playing);
  };

  return (
    <main id="main" className="flex flex-1 flex-col">
      <PlayerTopBar contentId={contentId} />

      <div className="mx-auto w-full max-w-5xl px-6 py-8">
        <div
          className="relative aspect-video overflow-hidden rounded-card border border-line"
          style={{
            background: `radial-gradient(110% 140% at 78% 18%, hsla(${hue}, 44%, 32%, 0.75), transparent 62%), linear-gradient(160deg, #111821, #0b0f14 70%)`,
          }}
        >
          <span
            aria-hidden="true"
            className="pointer-events-none absolute -top-6 left-6 font-display text-[12rem] leading-none text-paper/[0.05]"
          >
            {title.name.charAt(0)}
          </span>

          <button
            type="button"
            onClick={togglePlayback}
            className="absolute inset-0 flex items-center justify-center"
            aria-label={playing ? "Pausar" : "Reproducir"}
          >
            <span className="flex h-20 w-20 items-center justify-center rounded-full border border-brass/60 bg-ink-950/70 text-brass backdrop-blur transition-colors hover:bg-ink-950">
              {playing ? <PauseIcon /> : <PlayIcon />}
            </span>
          </button>

          {!playing ? (
            <p className="absolute bottom-4 left-4 text-xs text-paper-muted">
              Pulsa para {progress > 0 ? "continuar" : "empezar"}
            </p>
          ) : null}
        </div>

        <div className="mt-6 flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="font-display text-4xl leading-none text-paper">
              {title.name}
            </h1>
            {hasMetadata ? (
              <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-paper-muted tabular-nums">
                {title.age_rating ? <span>{title.age_rating}</span> : null}
                {durationLabel ? <span>{durationLabel}</span> : null}
                {resume?.episode_id ? <span>{resume.episode_id}</span> : null}
              </div>
            ) : null}
          </div>
          <TelemetryBadge status={telemetry.status} />
        </div>

        <div className="mt-6">
          <input
            type="range"
            min={0}
            max={duration}
            step={1}
            value={progress}
            onChange={(event) => seek(Number(event.target.value))}
            aria-label="Posición de reproducción"
            className="w-full accent-brass"
          />
          <div className="mt-1 flex justify-between text-xs text-paper-muted tabular-nums">
            <span>{formatTime(progress)}</span>
            <span>{total}</span>
          </div>
        </div>

        <div className="mt-6 flex flex-wrap items-center justify-between gap-4 border-t border-line pt-5">
          <RatingButtons contentId={contentId} />
        </div>

        <p className="mt-6 max-w-xl text-xs text-paper-muted">
          Este reproductor es una vista previa. El video real y el manifiesto se
          conectarán al servicio de catálogo cuando esté disponible.
        </p>
      </div>
    </main>
  );
}

function PlayerTopBar({ contentId }: { contentId?: string }) {
  return (
    <div className="border-b border-line">
      <div className="mx-auto flex w-full max-w-6xl items-center justify-between gap-4 px-6 py-3">
        {contentId ? (
          <Link
            href={`/title/${contentId}`}
            className="text-sm text-paper-muted transition-colors hover:text-paper"
          >
            ← Volver al detalle
          </Link>
        ) : (
          <Link
            href="/home"
            className="text-sm text-paper-muted transition-colors hover:text-paper"
          >
            ← Volver al inicio
          </Link>
        )}
        <Wordmark className="text-xl" href="/home" />
        <span className="hidden text-xs text-paper-muted sm:inline">
          Vista previa del reproductor
        </span>
      </div>
    </div>
  );
}

function TelemetryBadge({ status }: { status: TelemetryStatus }) {
  const messages: Record<TelemetryStatus, string> = {
    idle: "El progreso se guarda mientras reproduces.",
    saving: "Guardando progreso…",
    saved: "Progreso guardado.",
    error: "No pudimos guardar el progreso. Seguiremos intentando.",
  };

  return (
    <span
      role="status"
      aria-live="polite"
      className={
        status === "error" ? "text-xs text-signal" : "text-xs text-paper-muted"
      }
    >
      {messages[status]}
    </span>
  );
}

function PlayIcon() {
  return (
    <svg viewBox="0 0 24 24" width="28" height="28" aria-hidden="true">
      <path d="M8 5.5v13l11-6.5-11-6.5Z" fill="currentColor" />
    </svg>
  );
}

function PauseIcon() {
  return (
    <svg viewBox="0 0 24 24" width="28" height="28" aria-hidden="true">
      <path d="M7 5h4v14H7V5Zm6 0h4v14h-4V5Z" fill="currentColor" />
    </svg>
  );
}
