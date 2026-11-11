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
      <label htmlFor={id} className="text-sm font-medium text-tinta">
        {label}
      </label>
      <input
        id={id}
        suppressHydrationWarning
        aria-invalid={error ? true : undefined}
        aria-describedby={cn(hintId, errorId) || undefined}
        className={cn(
          "min-h-11 w-full rounded-hoja border bg-hoja-2 px-3 py-2 text-base text-tinta transition-[border-color,box-shadow] duration-150",
          "placeholder:text-tinta-tenue/70",
          "focus:border-azul focus:ring-2 focus:ring-azul/20 focus:outline-none focus-visible:outline-none",
          error ? "border-error" : "border-regla-fuerte",
          inputClassName,
        )}
        {...props}
      />
      {hint && !error ? (
        <p id={hintId} className="text-xs text-tinta-tenue">
          {hint}
        </p>
      ) : null}
      {error ? (
        <p id={errorId} className="text-xs text-error">
          {error}
        </p>
      ) : null}
    </div>
  );
}
