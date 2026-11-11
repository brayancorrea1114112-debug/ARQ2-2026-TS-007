import { randomUUID } from "node:crypto";
import { listClases } from "@/features/clases/server/library";

export const dynamic = "force-dynamic";

export async function GET() {
  try {
    const clases = await listClases();
    return Response.json({
      clases: clases.map((clase) => ({
        id: clase.id,
        title: clase.title,
        session: clase.session,
        date: clase.date,
        time: clase.time,
        size_bytes: clase.sizeBytes,
      })),
    });
  } catch {
    return Response.json(
      {
        error: {
          code: "CLASES_UNAVAILABLE",
          message:
            "No pudimos leer la carpeta de clases. Revisa que exista y que CLASES_DIR apunte a ella.",
          correlation_id: randomUUID(),
        },
      },
      { status: 503 },
    );
  }
}
