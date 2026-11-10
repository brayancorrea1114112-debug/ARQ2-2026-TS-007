import type { ApiErrorBody } from "@/shared/api/contracts";

const fallbackByStatus: Record<number, { code: string; message: string }> = {
  400: { code: "VALIDATION_ERROR", message: "La solicitud no es válida." },
  401: {
    code: "UNAUTHORIZED",
    message: "Tu sesión expiró. Vuelve a iniciar sesión.",
  },
  403: {
    code: "FORBIDDEN",
    message: "No tienes acceso a este contenido.",
  },
  404: {
    code: "NOT_FOUND",
    message: "No encontramos lo que buscabas.",
  },
  429: {
    code: "RATE_LIMITED",
    message: "Hicimos demasiadas solicitudes. Espera un momento.",
  },
  503: {
    code: "SERVICE_UNAVAILABLE",
    message: "El servicio no está disponible en este momento.",
  },
};

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly correlationId: string | null;

  constructor(params: {
    status: number;
    code: string;
    message: string;
    correlationId?: string | null;
  }) {
    super(params.message);
    this.name = "ApiError";
    this.status = params.status;
    this.code = params.code;
    this.correlationId = params.correlationId ?? null;
  }
}

export function isApiError(value: unknown): value is ApiError {
  return value instanceof ApiError;
}

export function parseApiError(
  status: number,
  body: unknown,
  fallbackCorrelationId: string | null,
): ApiError {
  const envelope = body as ApiErrorBody | null;
  if (envelope?.error?.code && envelope.error.message) {
    return new ApiError({
      status,
      code: envelope.error.code,
      message: envelope.error.message,
      correlationId: envelope.error.correlation_id ?? fallbackCorrelationId,
    });
  }

  const fallback = fallbackByStatus[status] ?? {
    code: status >= 500 ? "INTERNAL_ERROR" : "UNEXPECTED_ERROR",
    message: "Ocurrió un error inesperado.",
  };

  return new ApiError({
    status,
    code: fallback.code,
    message: fallback.message,
    correlationId: fallbackCorrelationId,
  });
}
