import { cn } from "@/shared/lib/cn";
import { buttonStyles } from "@/shared/ui/Button";

export function ErrorNotice({
  message,
  correlationId,
  onRetry,
  className,
}: {
  message: string;
  correlationId?: string | null;
  onRetry?: () => void;
  className?: string;
}) {
  return (
    <div
      role="alert"
      className={cn(
        "border-l-2 border-signal bg-ink-900/70 px-4 py-3",
        className,
      )}
    >
      <p className="text-sm text-paper">{message}</p>
      {correlationId ? (
        <p className="mt-1 text-xs text-paper-muted">
          Si necesitas soporte, comparte este identificador:{" "}
          <span className="break-all text-paper-muted">{correlationId}</span>
        </p>
      ) : null}
      {onRetry ? (
        <button
          type="button"
          onClick={onRetry}
          className={buttonStyles({
            variant: "ghost",
            size: "sm",
            className: "mt-2 px-0",
          })}
        >
          Reintentar
        </button>
      ) : null}
    </div>
  );
}
