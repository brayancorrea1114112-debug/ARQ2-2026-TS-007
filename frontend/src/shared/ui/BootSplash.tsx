import { ScanBar } from "@/shared/ui/ScanBar";
import { Wordmark } from "@/shared/ui/Wordmark";

export function BootSplash({ label }: { label: string }) {
  return (
    <div className="papel flex min-h-dvh flex-col items-center justify-center gap-6 px-6">
      <Wordmark className="text-6xl" />
      <div className="flex flex-col items-center gap-3 text-sm text-tinta-suave">
        <ScanBar />
        <span role="status">{label.endsWith("…") ? label : `${label}…`}</span>
      </div>
    </div>
  );
}
