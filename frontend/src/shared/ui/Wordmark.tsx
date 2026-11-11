import Link from "next/link";
import { cn } from "@/shared/lib/cn";

/** El marcador naranja es la cabeza lectora: la misma marca que indica el progreso. */
export function Wordmark({
  className,
  href,
  tone = "light",
}: {
  className?: string;
  href?: string;
  tone?: "light" | "dark";
}) {
  const content = (
    <span
      translate="no"
      className={cn(
        "titular inline-flex items-baseline",
        tone === "dark" ? "text-tiza" : "text-tinta",
        className,
      )}
    >
      StreamHub
      <span
        aria-hidden="true"
        className="ml-[0.1em] inline-block h-[0.74em] w-[0.11em] bg-marcador"
      />
    </span>
  );

  if (!href) {
    return content;
  }

  return (
    <Link
      href={href}
      className="inline-flex rounded-hoja focus-visible:outline-offset-4"
      aria-label="StreamHub, ir al inicio"
    >
      {content}
    </Link>
  );
}
