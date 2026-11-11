import Link from "next/link";
import type { HomeTitle } from "@/shared/api/contracts";
import { formatDuration } from "@/shared/lib/format";
import { Poster } from "@/shared/ui/Poster";

export function TitleRail({
  heading,
  titles,
  emptyMessage,
}: {
  heading: string;
  titles: HomeTitle[];
  emptyMessage?: string;
}) {
  return (
    <section className="mx-auto w-full max-w-6xl px-6 py-7">
      <div className="flex items-baseline justify-between gap-4 border-b border-line pb-3">
        <h2 className="font-display text-2xl text-paper">{heading}</h2>
        <span className="text-xs text-paper-muted">
          {titles.length} {titles.length === 1 ? "título" : "títulos"}
        </span>
      </div>

      {titles.length === 0 ? (
        <p className="mt-6 max-w-md text-sm text-paper-muted">
          {emptyMessage ?? "Aún no hay títulos en esta lista."}
        </p>
      ) : (
        <ul className="rail mt-5 flex snap-x gap-4 overflow-x-auto pb-2">
          {titles.map((title) => {
            const meta = [
              title.age_rating,
              formatDuration(title.duration_seconds),
            ]
              .filter(Boolean)
              .join(" · ");
            return (
              <li
                key={title.content_id}
                className="w-[148px] shrink-0 snap-start sm:w-[168px]"
              >
                <Link
                  href={`/title/${title.content_id}`}
                  className="group block rounded-card"
                >
                  <Poster
                    contentId={title.content_id}
                    name={title.name}
                    poster={title.poster}
                  />
                  <p className="mt-2 line-clamp-2 text-sm text-paper transition-colors group-hover:text-brass">
                    {title.name}
                  </p>
                  {meta ? (
                    <p className="mt-1 text-xs text-paper-muted tabular-nums">
                      {meta}
                    </p>
                  ) : null}
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
