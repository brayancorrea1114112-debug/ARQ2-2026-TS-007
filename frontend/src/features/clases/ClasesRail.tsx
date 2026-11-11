"use client";

import type { HomeView } from "@/shared/api/contracts";
import { ClaseCard } from "@/features/clases/ClaseCard";
import { SemesterRuler } from "@/features/clases/SemesterRuler";
import { useClases } from "@/features/clases/useClases";
import { SectionHeading } from "@/shared/ui/SectionHeading";

export function resumeByClase(home?: HomeView): Record<string, number> {
  const map: Record<string, number> = {};
  for (const item of home?.continue_watching ?? []) {
    map[item.content_id] = item.progress_seconds;
  }
  return map;
}

/** Fila de Home con las clases grabadas. Si la carpeta no responde, no estorba. */
export function ClasesRail({ home }: { home?: HomeView }) {
  const query = useClases();
  const clases = query.data ?? [];
  if (clases.length === 0) {
    return null;
  }
  const resume = resumeByClase(home);

  return (
    <section className="mx-auto w-full max-w-6xl px-6 py-8">
      <SectionHeading
        heading="Clases grabadas"
        aside={`${clases.length} ${clases.length === 1 ? "sesión" : "sesiones"}`}
      />
      <div className="mt-5">
        <SemesterRuler clases={clases} />
      </div>
      <ul className="rail mt-4 flex snap-x gap-4 overflow-x-auto pb-2">
        {clases.map((clase) => (
          <li
            key={clase.id}
            className="w-[272px] shrink-0 snap-start sm:w-[300px]"
          >
            <ClaseCard
              clase={clase}
              resumeSeconds={resume[clase.id]}
              className="h-full"
            />
          </li>
        ))}
      </ul>
    </section>
  );
}
