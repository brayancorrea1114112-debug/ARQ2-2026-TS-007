import Link from "next/link";
import type { HomeView } from "@/shared/api/contracts";
import { formatTime, progressPercent } from "@/shared/lib/format";
import { Poster } from "@/shared/ui/Poster";
import { SectionHeading } from "@/shared/ui/SectionHeading";
import { titleOrStub } from "@/features/home/lookup";

export function ContinueRail({ home }: { home: HomeView }) {
  const items = home.continue_watching;
  if (items.length === 0) {
    return null;
  }

  return (
    <section className="mx-auto w-full max-w-6xl px-6 py-8">
      <SectionHeading heading="Seguir viendo" aside="Tu progreso guardado" />

      <ul className="rail mt-5 flex snap-x gap-4 overflow-x-auto pb-2">
        {items.map((item) => {
          const title = titleOrStub(home, item.content_id);
          const percent = title.duration_seconds
            ? progressPercent(item.progress_seconds, title.duration_seconds)
            : null;

          return (
            <li
              key={`${item.content_id}-${item.episode_id ?? "main"}`}
              className="w-[240px] shrink-0 snap-start sm:w-[272px]"
            >
              <Link
                href={`/watch/${item.content_id}?t=${item.progress_seconds}`}
                className="group block rounded-hoja"
              >
                <div className="transition-transform duration-150 group-hover:-translate-y-0.5">
                  <Poster
                    contentId={item.content_id}
                    name={title.name}
                    poster={title.poster}
                    progress={percent}
                    ratio="wide"
                  />
                </div>
                <p className="mt-2 line-clamp-1 text-sm font-medium text-tinta transition-colors duration-150 group-hover:text-azul">
                  {title.name}
                </p>
                <p className="mt-1 text-xs text-tinta-tenue tabular-nums">
                  {formatTime(item.progress_seconds)} vistos
                  {item.episode_id ? ` · ${item.episode_id}` : ""}
                </p>
              </Link>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
