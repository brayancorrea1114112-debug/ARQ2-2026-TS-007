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
        "rounded-hoja border border-l-4 border-regla border-l-error bg-hoja-2 px-4 py-3 text-tinta",
        className,
      )}
    >
      <p className="text-sm font-medium">{message}</p>
      {correlationId ? (
        <p className="mt-1 text-xs text-tinta-suave">
          Si necesitas soporte, comparte este identificador:{" "}
          <span className="break-all">{correlationId}</span>
        </p>
      ) : null}
      {onRetry ? (
        <button
          type="button"
          onClick={onRetry}
          className={buttonStyles({
            variant: "ghost",
            size: "sm",
            className:
              "mt-2 px-0 text-azul hover:bg-transparent hover:text-azul-fuerte",
          })}
        >
          Reintentar
        </button>
      ) : null}
    </div>
  );
}
