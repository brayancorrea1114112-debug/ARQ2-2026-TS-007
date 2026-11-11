/** Barra de carga: una cabeza lectora que recorre una regla. */
export function ScanBar() {
  return (
    <span aria-hidden="true" className="relative block h-3 w-28 overflow-hidden">
      <span className="absolute top-1/2 right-0 left-0 h-px -translate-y-1/2 bg-regla-fuerte" />
      <span className="absolute top-0 h-3 w-[3px] animate-scan bg-marcador" />
    </span>
  );
}
