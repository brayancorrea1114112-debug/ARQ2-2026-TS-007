import Link from "next/link";
import type { Clase } from "@/features/clases/api";
import { claseDateParts } from "@/features/clases/format";
import { formatTime } from "@/shared/lib/format";

/**
 * Ficha de una clase grabada. El día es el protagonista: una clase se recuerda
 * por cuándo ocurrió, no por una carátula.
 */
export function ClaseCard({
  clase,
  resumeSeconds,
  className,
}: {
  clase: Clase;
  resumeSeconds?: number | null;
  className?: string;
}) {
  const parts = claseDateParts(clase.date, clase.time);
  const href =
    resumeSeconds && resumeSeconds > 0
      ? `/watch/${clase.id}?t=${resumeSeconds}`
      : `/watch/${clase.id}`;

  return (
    <Link
      href={href}
      className={`group flex flex-col overflow-hidden rounded-hoja border border-regla bg-hoja-2 transition-[border-color,transform] duration-150 hover:-translate-y-0.5 hover:border-azul ${className ?? ""}`}
      aria-label={`Clase ${clase.session}, ${parts.long}`}
    >
      <div
        className="pizarra-zona relative aspect-video overflow-hidden bg-pizarra text-tiza"
        style={{
          backgroundImage:
            "radial-gradient(120% 150% at 100% 0%, rgba(36, 51, 232, 0.5), transparent 62%), linear-gradient(rgba(241, 243, 255, 0.05) 1px, transparent 1px), linear-gradient(90deg, rgba(241, 243, 255, 0.05) 1px, transparent 1px)",
          backgroundSize: "auto, 24px 24px, 24px 24px",
        }}
      >
        <span className="absolute top-3 left-4 text-sm font-medium text-tiza/80">
          {parts.month}
        </span>
        <span className="absolute top-3 right-3 rounded-[3px] bg-tiza/12 px-2 py-0.5 text-xs font-medium text-tiza">
          Clase {clase.session}
        </span>
        <span
          aria-hidden="true"
          className="titular absolute -bottom-3 left-3 text-[7.5rem] leading-none text-tiza"
        >
          {parts.day}
        </span>
        <span
          aria-hidden="true"
          className="absolute right-3 bottom-3 flex h-11 w-11 items-center justify-center rounded-full bg-tiza text-pizarra transition-transform duration-150 group-hover:scale-105"
        >
          <svg viewBox="0 0 24 24" width="20" height="20">
            <path d="M8 5.5v13l11-6.5-11-6.5Z" fill="currentColor" />
          </svg>
        </span>
      </div>

      <div className="flex flex-col gap-1 p-4">
        <p className="font-medium text-tinta">{clase.title}</p>
        <p className="text-sm text-tinta-suave">
          {parts.long}, {parts.time}
        </p>
        {resumeSeconds && resumeSeconds > 0 ? (
          <p className="text-xs font-medium text-marcador-texto tabular-nums">
            Retomar en {formatTime(resumeSeconds)}
          </p>
        ) : null}
      </div>
    </Link>
  );
}
