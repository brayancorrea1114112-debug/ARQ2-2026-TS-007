import type { ButtonHTMLAttributes } from "react";
import { cn } from "@/shared/lib/cn";

type Variant = "primary" | "secondary" | "ghost" | "tiza";
type Size = "sm" | "md";

const variants: Record<Variant, string> = {
  primary: "border border-transparent bg-azul text-white hover:bg-azul-fuerte",
  secondary:
    "border border-regla-fuerte bg-hoja-2 text-tinta hover:border-azul hover:bg-azul-claro",
  ghost:
    "border border-transparent text-tinta-suave hover:bg-hoja-3 hover:text-tinta",
  tiza: "border border-transparent bg-tiza text-pizarra hover:bg-white",
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
    "inline-flex items-center justify-center gap-2 rounded-hoja font-medium transition-[background-color,border-color,color,transform] duration-150 active:scale-[0.97] disabled:cursor-not-allowed disabled:opacity-50 disabled:active:scale-100",
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
