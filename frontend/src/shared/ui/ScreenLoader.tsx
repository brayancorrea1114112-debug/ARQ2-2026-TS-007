import { ScanBar } from "@/shared/ui/ScanBar";

export function ScreenLoader({ label }: { label: string }) {
  return (
    <div className="flex min-h-[60dvh] flex-1 items-center justify-center px-6">
      <div className="flex flex-col items-center gap-4 text-sm opacity-80">
        <ScanBar />
        <span role="status">{label.endsWith("…") ? label : `${label}…`}</span>
      </div>
    </div>
  );
}
