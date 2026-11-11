const MONTHS = [
  "ene",
  "feb",
  "mar",
  "abr",
  "may",
  "jun",
  "jul",
  "ago",
  "sep",
  "oct",
  "nov",
  "dic",
];

const MONTHS_LONG = [
  "enero",
  "febrero",
  "marzo",
  "abril",
  "mayo",
  "junio",
  "julio",
  "agosto",
  "septiembre",
  "octubre",
  "noviembre",
  "diciembre",
];

const WEEKDAYS = [
  "domingo",
  "lunes",
  "martes",
  "miércoles",
  "jueves",
  "viernes",
  "sábado",
];

export function isClaseId(id?: string | null): boolean {
  return Boolean(id && /^clase-\d{8}$/.test(id));
}

export function dateFromClaseId(id: string): string | null {
  const match = /^clase-(\d{4})(\d{2})(\d{2})$/.exec(id);
  return match ? `${match[1]}-${match[2]}-${match[3]}` : null;
}

/** Días desde 1970 en UTC: evita desfases de zona horaria al comparar fechas. */
export function dayNumber(date: string): number {
  const [year, month, day] = date.split("-").map(Number);
  return Math.round(Date.UTC(year, month - 1, day) / 86_400_000);
}

export interface ClaseDateParts {
  day: string;
  dayNumber: number;
  month: string;
  monthLong: string;
  weekday: string;
  time: string;
  long: string;
}

export function claseDateParts(date: string, time: string): ClaseDateParts {
  const [year, month, day] = date.split("-").map(Number);
  const weekdayIndex = new Date(Date.UTC(year, month - 1, day)).getUTCDay();
  const [hours, minutes] = time.split(":").map(Number);
  const meridiem = hours >= 12 ? "p. m." : "a. m.";
  const hour12 = hours % 12 === 0 ? 12 : hours % 12;
  const weekday = WEEKDAYS[weekdayIndex];

  return {
    day: String(day).padStart(2, "0"),
    dayNumber: day,
    month: MONTHS[month - 1],
    monthLong: MONTHS_LONG[month - 1],
    weekday,
    time: `${hour12}:${String(minutes).padStart(2, "0")} ${meridiem}`,
    long: `${weekday.charAt(0).toUpperCase()}${weekday.slice(1)} ${day} de ${MONTHS_LONG[month - 1]}`,
  };
}

export function monthLabel(monthIndex: number): string {
  return MONTHS[monthIndex];
}
