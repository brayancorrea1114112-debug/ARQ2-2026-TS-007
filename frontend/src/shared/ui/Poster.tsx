import { cn } from "@/shared/lib/cn";
import { hueFromId } from "@/shared/lib/format";
import { safeImageUrl } from "@/shared/lib/url";

export function Poster({
  contentId,
  name,
  poster,
  progress,
  className,
}: {
  contentId: string;
  name: string;
  poster?: string | null;
  progress?: number | null;
  className?: string;
}) {
  const hue = hueFromId(contentId);
  const image = safeImageUrl(poster);
  const gradient = `linear-gradient(155deg, hsl(${hue} 42% 30%) 0%, hsl(${
    (hue + 42) % 360
  } 34% 16%) 52%, #0b0f14 100%)`;
  const backgroundImage = image
    ? `linear-gradient(180deg, rgba(11, 15, 20, 0) 45%, rgba(11, 15, 20, 0.86) 100%), url("${image.replace(
        /"/g,
        '\\"',
      )}"), ${gradient}`
    : gradient;

  return (
    <div
      className={cn(
        "relative aspect-[2/3] w-full overflow-hidden rounded-card border border-line/80 bg-ink-900",
        className,
      )}
      style={{
        backgroundImage,
        backgroundSize: "cover",
        backgroundPosition: "center",
      }}
      aria-hidden="true"
    >
      <span className="absolute top-3 left-3 font-display text-4xl text-paper/20">
        {name.charAt(0)}
      </span>
      {typeof progress === "number" && progress > 0 ? (
        <span className="absolute inset-x-0 bottom-0 h-1 bg-ink-950/80">
          <span
            className="block h-full bg-brass"
            style={{ width: `${Math.min(100, Math.max(0, progress))}%` }}
          />
        </span>
      ) : null}
    </div>
  );
}
