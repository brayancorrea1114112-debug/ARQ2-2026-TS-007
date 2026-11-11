import Link from "next/link";
import type { HomeTitle } from "@/shared/api/contracts";
import { formatDuration } from "@/shared/lib/format";
import { Poster } from "@/shared/ui/Poster";
import { SectionHeading } from "@/shared/ui/SectionHeading";

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
    <section className="mx-auto w-full max-w-6xl px-6 py-8">
      <SectionHeading
        heading={heading}
        aside={`${titles.length} ${titles.length === 1 ? "título" : "títulos"}`}
      />

      {titles.length === 0 ? (
        <p className="mt-6 max-w-md text-sm text-tinta-suave">
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
                  className="group block rounded-hoja"
                >
                  <div className="transition-transform duration-150 group-hover:-translate-y-0.5">
                    <Poster
                      contentId={title.content_id}
                      name={title.name}
                      poster={title.poster}
                    />
                  </div>
                  <p className="mt-2 line-clamp-2 text-sm font-medium text-tinta transition-colors duration-150 group-hover:text-azul">
                    {title.name}
                  </p>
                  {meta ? (
                    <p className="mt-1 text-xs text-tinta-tenue tabular-nums">
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
