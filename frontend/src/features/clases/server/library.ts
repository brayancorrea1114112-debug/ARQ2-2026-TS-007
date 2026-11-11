import { promises as fs } from "node:fs";
import path from "node:path";

/**
 * Carpeta con las grabaciones de clase. Se puede cambiar con la variable de
 * entorno CLASES_DIR sin tocar el código.
 */
export const DEFAULT_CLASES_DIR =
  "G:\\Mi unidad\\Aprendizaje\\Desarrollo de software\\Arquitectura de software\\Arquitectura2\\Clases";

export function clasesDir(): string {
  return process.env.CLASES_DIR?.trim() || DEFAULT_CLASES_DIR;
}

export interface ClaseFile {
  id: string;
  title: string;
  session: number;
  /** Fecha de la grabación (AAAA-MM-DD), tal como aparece en el nombre del archivo. */
  date: string;
  /** Hora de inicio (HH:MM), hora local de la clase. */
  time: string;
  sizeBytes: number;
  absolutePath: string;
}

export const CLASE_ID_PATTERN = /^clase-\d{8}$/;

// Ejemplo: "190304011-1 ARQUITECTURAS DE SOFTWARE II-20260908_200111-Meeting Recording.mp4"
const FILE_PATTERN =
  /^(.+?)-(\d{4})(\d{2})(\d{2})_(\d{2})(\d{2})\d{2}-.*\.mp4$/i;

const SMALL_WORDS = new Set(["de", "del", "la", "el", "y", "en", "a"]);

function courseTitle(raw: string): string {
  const withoutCode = raw.replace(/^\d+-\d+\s+/, "").trim();
  return withoutCode
    .toLowerCase()
    .split(/\s+/)
    .filter(Boolean)
    .map((word, index) => {
      if (/^[ivx]+$/.test(word)) {
        return word.toUpperCase();
      }
      if (index > 0 && SMALL_WORDS.has(word)) {
        return word;
      }
      return word.charAt(0).toUpperCase() + word.slice(1);
    })
    .join(" ");
}

export async function listClases(): Promise<ClaseFile[]> {
  const dir = clasesDir();
  const entries = await fs.readdir(dir, { withFileTypes: true });

  const parsed = entries
    .filter((entry) => entry.isFile())
    .flatMap((entry) => {
      const match = FILE_PATTERN.exec(entry.name);
      if (!match) {
        return [];
      }
      const [, course, y, m, d, hh, mm] = match;
      return [
        {
          id: `clase-${y}${m}${d}`,
          title: courseTitle(course),
          date: `${y}-${m}-${d}`,
          time: `${hh}:${mm}`,
          absolutePath: path.join(/*turbopackIgnore: true*/ dir, entry.name),
        },
      ];
    });

  const withSize = await Promise.all(
    parsed.map(async (item) => ({
      ...item,
      sizeBytes: (await fs.stat(item.absolutePath)).size,
    })),
  );

  withSize.sort((a, b) =>
    `${a.date}T${a.time}`.localeCompare(`${b.date}T${b.time}`),
  );

  // Si dos archivos comparten fecha, gana el más reciente: el id es por día.
  const byId = new Map<string, (typeof withSize)[number]>();
  for (const item of withSize) {
    byId.set(item.id, item);
  }

  return [...byId.values()].map((item, index) => ({
    ...item,
    session: index + 1,
  }));
}

export async function findClase(id: string): Promise<ClaseFile | null> {
  if (!CLASE_ID_PATTERN.test(id)) {
    return null;
  }
  const clases = await listClases();
  return clases.find((clase) => clase.id === id) ?? null;
}
