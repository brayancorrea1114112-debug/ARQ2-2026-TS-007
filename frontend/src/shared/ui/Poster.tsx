import { cn } from "@/shared/lib/cn";
import { hueFromId } from "@/shared/lib/format";
import { safeImageUrl } from "@/shared/lib/url";

export function Poster({
  contentId,
  name,
  poster,
  progress,
  ratio = "poster",
  className,
}: {
  contentId: string;
  name: string;
  poster?: string | null;
  progress?: number | null;
  ratio?: "poster" | "wide";
  className?: string;
}) {
  const hue = hueFromId(contentId);
  const image = safeImageUrl(poster);
  const gradient = `linear-gradient(155deg, hsl(${hue} 62% 44%) 0%, hsl(${
    (hue + 48) % 360
  } 58% 26%) 58%, #0b1030 100%)`;
  const backgroundImage = image
    ? `linear-gradient(180deg, rgba(11, 16, 48, 0) 45%, rgba(11, 16, 48, 0.82) 100%), url("${image.replace(
        /"/g,
        '\\"',
      )}"), ${gradient}`
    : gradient;
  const percent =
    typeof progress === "number" && progress > 0
      ? Math.min(100, Math.max(0, progress))
      : null;

  return (
    <div
      className={cn(
        "relative w-full overflow-hidden rounded-hoja bg-pizarra ring-1 ring-black/10 ring-inset",
        ratio === "wide" ? "aspect-video" : "aspect-[2/3]",
        className,
      )}
      style={{
        backgroundImage,
        backgroundSize: "cover",
        backgroundPosition: "center",
      }}
      aria-hidden="true"
    >
      <span
        className={cn(
          "titular absolute text-tiza/25",
          ratio === "wide"
            ? "-bottom-2 left-3 text-7xl"
            : "bottom-1 left-3 text-8xl",
        )}
      >
        {name.charAt(0)}
      </span>
      {percent !== null ? (
        <span className="absolute inset-x-0 bottom-0 h-1 bg-pizarra/70">
          <span
            className="block h-full bg-marcador"
            style={{ width: `${percent}%` }}
          />
        </span>
      ) : null}
    </div>
  );
}
