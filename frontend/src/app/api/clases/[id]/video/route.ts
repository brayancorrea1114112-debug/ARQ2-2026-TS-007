import { createReadStream } from "node:fs";
import { stat } from "node:fs/promises";
import { Readable } from "node:stream";
import { findClase } from "@/features/clases/server/library";

export const dynamic = "force-dynamic";

const RANGE_PATTERN = /^bytes=(\d*)-(\d*)$/;

/**
 * Entrega el video de una clase con soporte de `Range`, para que el navegador
 * pueda adelantar y retomar sin descargar el archivo completo. El id se busca en
 * la lista de la carpeta: la ruta del archivo nunca sale del cliente.
 */
export async function GET(
  request: Request,
  context: { params: Promise<{ id: string }> },
) {
  const { id } = await context.params;
  const clase = await findClase(id).catch(() => null);
  if (!clase) {
    return new Response("Clase no encontrada", { status: 404 });
  }

  let size: number;
  try {
    size = (await stat(clase.absolutePath)).size;
  } catch {
    return new Response("Clase no disponible", { status: 404 });
  }

  let start = 0;
  let end = size - 1;
  let status = 200;

  const rangeHeader = request.headers.get("range");
  if (rangeHeader) {
    const match = RANGE_PATTERN.exec(rangeHeader.trim());
    if (!match || (match[1] === "" && match[2] === "")) {
      return new Response(null, {
        status: 416,
        headers: { "Content-Range": `bytes */${size}` },
      });
    }
    if (match[1] === "") {
      start = Math.max(0, size - Number(match[2]));
    } else {
      start = Number(match[1]);
      if (match[2] !== "") {
        end = Math.min(Number(match[2]), size - 1);
      }
    }
    if (start > end || start >= size) {
      return new Response(null, {
        status: 416,
        headers: { "Content-Range": `bytes */${size}` },
      });
    }
    status = 206;
  }

  const stream = createReadStream(clase.absolutePath, { start, end });
  request.signal.addEventListener("abort", () => stream.destroy(), {
    once: true,
  });

  const headers: Record<string, string> = {
    "Content-Type": "video/mp4",
    "Accept-Ranges": "bytes",
    "Content-Length": String(end - start + 1),
    "Cache-Control": "private, max-age=3600",
  };
  if (status === 206) {
    headers["Content-Range"] = `bytes ${start}-${end}/${size}`;
  }

  return new Response(Readable.toWeb(stream) as unknown as BodyInit, {
    status,
    headers,
  });
}
