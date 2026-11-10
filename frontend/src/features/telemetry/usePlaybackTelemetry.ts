"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useAuthFailure } from "@/features/auth/useAuthFailure";
import { sendPing } from "@/features/telemetry/api";
import { ApiError } from "@/shared/api/errors";
import { createEventId } from "@/shared/lib/ids";

export type TelemetryStatus = "idle" | "saving" | "saved" | "error";

const PING_INTERVAL_MS = 10_000;
const RETRY_DELAYS_MS = [1_000, 3_000, 6_000];

export function usePlaybackTelemetry({
  token,
  contentId,
  episodeId,
  playing,
  progressSeconds,
}: {
  token: string;
  contentId: string;
  episodeId?: string | null;
  playing: boolean;
  progressSeconds: number;
}) {
  const authFailure = useAuthFailure();
  const progressRef = useRef(progressSeconds);
  const [status, setStatus] = useState<TelemetryStatus>("idle");

  useEffect(() => {
    progressRef.current = progressSeconds;
  }, [progressSeconds]);

  const send = useCallback(
    async ({ keepalive = false, silent = false } = {}) => {
      const eventId = createEventId();
      const payload = {
        event_id: eventId,
        content_id: contentId,
        ...(episodeId ? { episode_id: episodeId } : {}),
        progress_seconds: Math.max(0, Math.round(progressRef.current)),
        timestamp: new Date().toISOString(),
      };

      if (!silent) {
        setStatus("saving");
      }

      for (let attempt = 0; attempt < RETRY_DELAYS_MS.length; attempt += 1) {
        try {
          await sendPing(token, payload, keepalive);
          setStatus("saved");
          return;
        } catch (caught) {
          if (authFailure(caught)) {
            return;
          }
          if (
            caught instanceof ApiError &&
            caught.status >= 400 &&
            caught.status < 500 &&
            caught.status !== 429
          ) {
            setStatus("error");
            return;
          }
          if (attempt < RETRY_DELAYS_MS.length - 1) {
            await new Promise((resolve) =>
              setTimeout(resolve, RETRY_DELAYS_MS[attempt]),
            );
          }
        }
      }

      setStatus("error");
    },
    [authFailure, contentId, episodeId, token],
  );

  useEffect(() => {
    if (!playing) {
      return;
    }
    const timer = setInterval(() => {
      void send();
    }, PING_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [playing, send]);

  useEffect(() => {
    if (!playing) {
      return;
    }
    return () => {
      void send({ keepalive: true, silent: true });
    };
  }, [playing, send]);

  return { status, flush: send };
}
