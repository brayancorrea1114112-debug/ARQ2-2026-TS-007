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
