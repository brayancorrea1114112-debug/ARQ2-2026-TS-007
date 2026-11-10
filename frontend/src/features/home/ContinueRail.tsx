import Link from "next/link";
import type { HomeView } from "@/shared/api/contracts";
import { formatTime, progressPercent } from "@/shared/lib/format";
import { Poster } from "@/shared/ui/Poster";
import { titleOrStub } from "@/features/home/lookup";

export function ContinueRail({ home }: { home: HomeView }) {
  const items = home.continue_watching;
  if (items.length === 0) {
    return null;
  }

  return (
    <section className="mx-auto w-full max-w-6xl px-6 py-7">
      <div className="flex items-baseline justify-between gap-4 border-b border-line pb-3">
        <h2 className="font-display text-2xl text-paper">Seguir viendo</h2>
        <span className="text-xs text-paper-muted">Tu progreso guardado</span>
      </div>

      <ul className="rail mt-5 flex snap-x gap-4 overflow-x-auto pb-2">
        {items.map((item) => {
          const title = titleOrStub(home, item.content_id);
          const percent = title.duration_seconds
            ? progressPercent(item.progress_seconds, title.duration_seconds)
            : null;

          return (
            <li
              key={`${item.content_id}-${item.episode_id ?? "main"}`}
              className="w-[176px] shrink-0 snap-start sm:w-[196px]"
            >
              <Link
                href={`/watch/${item.content_id}?t=${item.progress_seconds}`}
                className="group block rounded-card"
              >
                <Poster
                  contentId={item.content_id}
                  name={title.name}
                  poster={title.poster}
                  progress={percent}
                />
                <p className="mt-2 line-clamp-1 text-sm text-paper transition-colors group-hover:text-brass">
                  {title.name}
                </p>
                <p className="mt-1 text-xs text-paper-muted tabular-nums">
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
