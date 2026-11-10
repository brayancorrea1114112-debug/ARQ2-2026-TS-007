import type { ButtonHTMLAttributes } from "react";
import { cn } from "@/shared/lib/cn";

type Variant = "primary" | "secondary" | "ghost";
type Size = "sm" | "md";

const variants: Record<Variant, string> = {
  primary:
    "bg-brass text-ink-950 hover:bg-brass-strong border border-transparent",
  secondary:
    "border border-line bg-ink-900 text-paper hover:border-paper-muted",
  ghost:
    "border border-transparent text-paper-muted hover:text-paper hover:bg-ink-800",
};

const sizes: Record<Size, string> = {
  sm: "min-h-11 px-3 text-sm",
  md: "min-h-11 px-5 text-sm",
};

export function buttonStyles({
  variant = "primary",
  size = "md",
  className,
}: {
  variant?: Variant;
  size?: Size;
  className?: string;
} = {}): string {
  return cn(
    "inline-flex items-center justify-center gap-2 rounded-card font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50",
    variants[variant],
    sizes[size],
    className,
  );
}

export function Button({
  variant = "primary",
  size = "md",
  className,
  type = "button",
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
  size?: Size;
}) {
  return (
    <button
      type={type}
      className={buttonStyles({ variant, size, className })}
      {...props}
    />
  );
}
