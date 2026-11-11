export interface Clase {
  id: string;
  title: string;
  session: number;
  /** AAAA-MM-DD */
  date: string;
  /** HH:MM */
  time: string;
  size_bytes: number;
}

export class ClasesError extends Error {
  readonly correlationId: string | null;

  constructor(message: string, correlationId: string | null = null) {
    super(message);
    this.name = "ClasesError";
    this.correlationId = correlationId;
  }
}

export async function fetchClases(): Promise<Clase[]> {
  let response: Response;
  try {
    response = await fetch("/api/clases", { cache: "no-store" });
  } catch {
    throw new ClasesError("No pudimos conectar con la carpeta de clases.");
  }

  const body: unknown = await response.json().catch(() => null);

  if (!response.ok) {
    const error =
      body && typeof body === "object" && "error" in body
        ? (body as { error?: { message?: string; correlation_id?: string } })
            .error
        : undefined;
    throw new ClasesError(
      error?.message ?? "No pudimos cargar las clases.",
      error?.correlation_id ?? null,
    );
  }

  const clases =
    body && typeof body === "object" && "clases" in body
      ? (body as { clases: unknown }).clases
      : null;

  return Array.isArray(clases) ? (clases as Clase[]) : [];
}

export function claseVideoUrl(id: string): string {
  return `/api/clases/${encodeURIComponent(id)}/video`;
}
