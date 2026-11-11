import Link from "next/link";
import type { Clase } from "@/features/clases/api";
import {
  claseDateParts,
  dayNumber,
  monthLabel,
} from "@/features/clases/format";

/**
 * Regla del semestre: cada clase grabada es una marca en su fecha real. Muestra
 * de un vistazo el ritmo de las clases y los huecos entre una y otra.
 */
export function SemesterRuler({ clases }: { clases: Clase[] }) {
  if (clases.length < 2) {
    return null;
  }

  const days = clases.map((clase) => dayNumber(clase.date));
  const first = Math.min(...days) - 4;
  const last = Math.max(...days) + 4;
  const span = last - first;
  const position = (day: number) => ((day - first) / span) * 100;

  const weekTicks: number[] = [];
  for (let day = first; day <= last; day += 7) {
    weekTicks.push(day);
  }

  const startDate = new Date(first * 86_400_000);
  const months: Array<{ day: number; label: string }> = [
    { day: first, label: monthLabel(startDate.getUTCMonth()) },
  ];
  for (let day = first + 1; day <= last; day += 1) {
    const date = new Date(day * 86_400_000);
    if (date.getUTCDate() === 1) {
      months.push({ day, label: monthLabel(date.getUTCMonth()) });
    }
  }

  return (
    <div className="rail -mx-1 overflow-x-auto px-1">
      <div className="relative h-24 min-w-[640px]">
        {months.map((month) => (
          <span
            key={month.day}
            className="absolute top-0 h-5 border-l border-regla-fuerte pl-1.5 text-xs font-medium text-tinta-tenue"
            style={{ left: `${position(month.day)}%` }}
          >
            {month.label}
          </span>
        ))}

        <span
          aria-hidden="true"
          className="absolute inset-x-0 top-9 h-px bg-regla-fuerte"
        />
        {weekTicks.map((day) => (
          <span
            key={day}
            aria-hidden="true"
            className="absolute top-9 h-1.5 w-px bg-regla-fuerte"
            style={{ left: `${position(day)}%` }}
          />
        ))}

        {clases.map((clase) => {
          const parts = claseDateParts(clase.date, clase.time);
          return (
            <Link
              key={clase.id}
              href={`/watch/${clase.id}`}
              title={`Clase ${clase.session}: ${parts.long}`}
              aria-label={`Clase ${clase.session}, ${parts.long}`}
              className="group absolute top-6 flex -translate-x-1/2 flex-col items-center px-2"
              style={{ left: `${position(dayNumber(clase.date))}%` }}
            >
              <span className="h-8 w-[3px] bg-azul transition-colors duration-150 group-hover:bg-marcador" />
              <span className="mt-1 text-xs font-semibold text-tinta tabular-nums">
                {parts.dayNumber}
              </span>
              <span className="text-[11px] text-tinta-tenue tabular-nums">
                C{clase.session}
              </span>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
