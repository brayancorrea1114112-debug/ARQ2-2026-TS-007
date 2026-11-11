import { Wordmark } from "@/shared/ui/Wordmark";

export function BootSplash({ label }: { label: string }) {
  return (
    <div className="screen-grain flex min-h-dvh flex-col items-center justify-center gap-6 px-6">
      <Wordmark className="text-4xl" />
      <div className="flex items-center gap-3 text-sm text-paper-muted">
        <span className="h-px w-10 bg-brass" aria-hidden="true" />
        <span role="status">
          {label.endsWith("…") ? label : `${label}…`}
        </span>
      </div>
    </div>
  );
}
