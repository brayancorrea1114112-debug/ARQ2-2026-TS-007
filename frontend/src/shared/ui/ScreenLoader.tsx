export function ScreenLoader({ label }: { label: string }) {
  return (
    <div className="flex min-h-[60dvh] items-center justify-center px-6">
      <div className="flex items-center gap-3 text-sm text-paper-muted">
        <span
          className="h-2 w-2 animate-pulse rounded-full bg-brass"
          aria-hidden="true"
        />
        <span role="status">
          {label.endsWith("…") ? label : `${label}…`}
        </span>
      </div>
    </div>
  );
}
