import type { ReactNode } from "react";

/** Encabezado de sección: la regla lleva un tramo naranja, como el avance de una cinta. */
export function SectionHeading({
  heading,
  aside,
}: {
  heading: string;
  aside?: ReactNode;
}) {
  return (
    <div>
      <div className="flex items-end justify-between gap-4">
        <h2 className="titular text-3xl text-tinta sm:text-4xl">{heading}</h2>
        {aside ? (
          <span className="pb-1 text-xs text-tinta-tenue tabular-nums">
            {aside}
          </span>
        ) : null}
      </div>
      <div className="mt-3 flex items-center" aria-hidden="true">
        <span className="h-[3px] w-10 bg-marcador" />
        <span className="h-px flex-1 bg-regla" />
      </div>
    </div>
  );
}
