import Link from "next/link";
import { cn } from "@/shared/lib/cn";

export function Wordmark({
  className,
  href,
}: {
  className?: string;
  href?: string;
}) {
  const content = (
    <span
      translate="no"
      className={cn("font-display tracking-[-0.02em]", className)}
    >
      StreamHub<span className="text-brass">.</span>
    </span>
  );

  if (!href) {
    return content;
  }

  return (
    <Link
      href={href}
      className="inline-flex rounded-card focus-visible:outline-offset-4"
      aria-label="StreamHub, ir al inicio"
    >
      {content}
    </Link>
  );
}
