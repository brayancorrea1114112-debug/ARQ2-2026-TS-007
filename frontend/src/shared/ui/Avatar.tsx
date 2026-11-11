import { cn } from "@/shared/lib/cn";
import { initials } from "@/shared/lib/format";
import { safeImageUrl } from "@/shared/lib/url";

export function Avatar({
  name,
  src,
  tone = "adult",
  className,
}: {
  name: string;
  src?: string | null;
  tone?: "adult" | "kids";
  className?: string;
}) {
  const image = safeImageUrl(src);

  return (
    <span
      className={cn(
        "relative flex shrink-0 items-center justify-center overflow-hidden rounded-full border text-sm font-semibold",
        tone === "kids"
          ? "border-mint/40 bg-mint/10 text-mint"
          : "border-brass/40 bg-brass/10 text-brass",
        "h-12 w-12",
        className,
      )}
      style={
        image
          ? {
              backgroundImage: `url("${image.replace(/"/g, '\\"')}")`,
              backgroundSize: "cover",
              backgroundPosition: "center",
            }
          : undefined
      }
      aria-hidden="true"
    >
      {image ? null : initials(name)}
    </span>
  );
}
