"use client";

import { useState } from "react";
import { useSessionStore } from "@/features/auth/session";
import { useAuthFailure } from "@/features/auth/useAuthFailure";
import { sendRating } from "@/features/interactions/api";
import type { Rating } from "@/shared/api/contracts";
import { isApiError } from "@/shared/api/errors";
import { cn } from "@/shared/lib/cn";

type Status = "idle" | "sending" | "sent" | "error";

const labels: Record<Rating, string> = {
  LIKE: "Me gusta",
  DISLIKE: "No me gusta",
};

export function RatingButtons({
  contentId,
  tone = "light",
}: {
  contentId: string;
  tone?: "light" | "dark";
}) {
  const token = useSessionStore((state) => state.profileToken);
  const authFailure = useAuthFailure();
  const [rating, setRating] = useState<Rating | null>(null);
  const [status, setStatus] = useState<Status>("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (!token) {
    return null;
  }

  const submit = async (value: Rating) => {
    setRating(value);
    setStatus("sending");
    setErrorMessage(null);
    try {
      await sendRating(token, contentId, value);
      setStatus("sent");
    } catch (caught) {
      if (authFailure(caught)) {
        return;
      }
      setStatus("error");
      setErrorMessage(
        isApiError(caught)
          ? caught.message
          : "No pudimos registrar tu valoración.",
      );
    }
  };

  const dark = tone === "dark";

  return (
    <div className="flex flex-wrap items-center gap-3">
      {(Object.keys(labels) as Rating[]).map((value) => (
        <button
          key={value}
          type="button"
          onClick={() => submit(value)}
          disabled={status === "sending"}
          aria-pressed={rating === value}
          className={cn(
            "min-h-11 rounded-hoja border px-4 text-sm font-medium transition-[background-color,border-color,color,transform] duration-150 active:scale-[0.97] disabled:cursor-not-allowed disabled:opacity-60",
            dark
              ? rating === value
                ? "border-tiza bg-tiza text-pizarra"
                : "border-tiza/30 text-tiza hover:border-tiza"
              : rating === value
                ? "border-azul bg-azul text-white"
                : "border-regla-fuerte bg-hoja-2 text-tinta hover:border-azul hover:bg-azul-claro",
          )}
        >
          {labels[value]}
        </button>
      ))}

      <span
        role="status"
        aria-live="polite"
        className={cn(
          "text-xs",
          status === "error"
            ? dark
              ? "text-[#ff9c8f]"
              : "text-error"
            : dark
              ? "text-tiza/70"
              : "text-tinta-suave",
        )}
      >
        {status === "sending"
          ? "Enviando…"
          : status === "sent"
            ? "Recibido. Tus recomendaciones se actualizarán en un momento."
            : status === "error"
              ? errorMessage
              : null}
      </span>
    </div>
  );
}
