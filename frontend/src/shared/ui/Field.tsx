import type { InputHTMLAttributes } from "react";
import { useId } from "react";
import { cn } from "@/shared/lib/cn";

export function Field({
  label,
  hint,
  error,
  className,
  inputClassName,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  hint?: string;
  error?: string;
  inputClassName?: string;
}) {
  const id = useId();
  const hintId = hint ? `${id}-hint` : undefined;
  const errorId = error ? `${id}-error` : undefined;

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <label
        htmlFor={id}
        className="text-sm font-medium text-paper-muted"
      >
        {label}
      </label>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={cn(hintId, errorId) || undefined}
        className={cn(
          "min-h-11 w-full border-0 border-b bg-transparent px-0 py-2 text-base text-paper",
          "placeholder:text-paper-muted/60",
          "focus:border-brass focus:outline-none focus-visible:outline-none",
          error ? "border-signal" : "border-line",
          inputClassName,
        )}
        {...props}
      />
      {hint && !error ? (
        <p id={hintId} className="text-xs text-paper-muted">
          {hint}
        </p>
      ) : null}
      {error ? (
        <p id={errorId} className="text-xs text-signal">
          {error}
        </p>
      ) : null}
    </div>
  );
}
