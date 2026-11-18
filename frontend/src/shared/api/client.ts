import { API_BASE_URL } from "@/shared/api/config";
import { ApiError, parseApiError } from "@/shared/api/errors";
import { createEventId } from "@/shared/lib/ids";

export interface ApiRequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  token?: string | null;
  body?: unknown;
  keepalive?: boolean;
  signal?: AbortSignal;
  correlationId?: string;
}

const SESSION_TRACE_KEY = "streamhub.session-trace-id";

function randomHex(bytes: number): string {
  const values = new Uint8Array(bytes);
  crypto.getRandomValues(values);
  return Array.from(values, (value) => value.toString(16).padStart(2, "0")).join("");
}

/**
 * One trace id per session: a new one is created at login and reused by every later request,
 * so Jaeger shows the whole session (login, profile, Home, pings, likes) as a single trace.
 * Each request still gets its own span id. Kong continues the trace from this W3C header.
 */
function sessionTraceparent(path: string): string | undefined {
  if (typeof window === "undefined") {
    return undefined;
  }
  try {
    let traceId = window.sessionStorage.getItem(SESSION_TRACE_KEY);
    if (path === "/auth/login" || !traceId) {
      traceId = randomHex(16);
      window.sessionStorage.setItem(SESSION_TRACE_KEY, traceId);
    }
    return `00-${traceId}-${randomHex(8)}-01`;
  } catch {
    return undefined;
  }
}

function safeParse(text: string): unknown {
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return text;
  }
}

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const correlationId = options.correlationId ?? createEventId();
  const headers: Record<string, string> = {
    Accept: "application/json",
    "X-Correlation-ID": correlationId,
  };

  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json; charset=utf-8";
  }
  if (options.token) {
    headers.Authorization = `Bearer ${options.token}`;
  }
  const traceparent = sessionTraceparent(path);
  if (traceparent) {
    headers.traceparent = traceparent;
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: options.method ?? "GET",
      headers,
      body:
        options.body === undefined ? undefined : JSON.stringify(options.body),
      keepalive: options.keepalive,
      signal: options.signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      throw error;
    }
    throw new ApiError({
      status: 0,
      code: "NETWORK_ERROR",
      message:
        "No pudimos conectar con el servidor. Revisa que Kong esté en marcha.",
      correlationId,
    });
  }

  const text = await response.text();
  const body = text ? safeParse(text) : undefined;

  if (!response.ok) {
    throw parseApiError(response.status, body, correlationId);
  }

  return body as T;
}
