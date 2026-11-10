import { delay, http, HttpResponse } from "msw";
import { API_BASE_URL } from "@/shared/api/config";
import { buildHome, MOCK_ACCOUNTS, MOCK_PASSWORD } from "@/mocks/fixtures";

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

type TokenInfo =
  | { kind: "account"; accountId: string }
  | { kind: "profile"; accountId: string; profileId: string };

function correlationId(request: Request): string {
  return request.headers.get("X-Correlation-ID") ?? crypto.randomUUID();
}

function errorResponse(
  request: Request,
  status: number,
  code: string,
  message: string,
) {
  return HttpResponse.json(
    {
      error: {
        code,
        message,
        correlation_id: correlationId(request),
      },
    },
    { status },
  );
}

function parseToken(request: Request): TokenInfo | null {
  const header = request.headers.get("Authorization") ?? "";
  const match = /^Bearer\s+(.+)$/i.exec(header);
  if (!match) {
    return null;
  }
  const [prefix, accountId, profileId] = match[1].split(":");
  if (prefix === "mock-account" && accountId) {
    return { kind: "account", accountId };
  }
  if (prefix === "mock-profile" && accountId && profileId) {
    return { kind: "profile", accountId, profileId };
  }
  return null;
}

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  const parts = token.split(".");
  if (parts.length !== 3) {
    return null;
  }
  try {
    const normalized = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
    const payload = JSON.parse(atob(padded)) as unknown;
    return payload !== null && typeof payload === "object"
      ? (payload as Record<string, unknown>)
      : null;
  } catch {
    return null;
  }
}

/**
 * In `missing` mode the browser sends real JWTs issued by auth-service to the
 * mocked telemetry/interaction endpoints, so a mock token format is not enough.
 */
function requireProfileToken(
  request: Request,
): { token: TokenInfo & { kind: "profile" } } | { response: Response } {
  const header = request.headers.get("Authorization") ?? "";
  const match = /^Bearer\s+(.+)$/i.exec(header);
  if (!match) {
    return {
      response: errorResponse(
        request,
        401,
        "UNAUTHORIZED",
        "La sesión no es válida o expiró.",
      ),
    };
  }

  const token = parseToken(request);
  if (token) {
    if (token.kind !== "profile") {
      return {
        response: errorResponse(
          request,
          403,
          "PROFILE_NOT_SELECTED",
          "Selecciona un perfil para continuar.",
        ),
      };
    }
    return { token };
  }

  const payload = decodeJwtPayload(match[1]);
  if (payload) {
    const profileId =
      typeof payload.profile_id === "string" ? payload.profile_id : null;
    return profileId
      ? {
          token: {
            kind: "profile",
            accountId:
              typeof payload.sub === "string" ? payload.sub : "",
            profileId,
          },
        }
      : {
          response: errorResponse(
            request,
            403,
            "PROFILE_NOT_SELECTED",
            "Selecciona un perfil para continuar.",
          ),
        };
  }

  return {
    response: errorResponse(
      request,
      401,
      "UNAUTHORIZED",
      "La sesión no es válida o expiró.",
    ),
  };
}

async function readJson<T>(request: Request): Promise<T | null> {
  try {
    return (await request.json()) as T;
  } catch {
    return null;
  }
}

const loginHandler = http.post(
  `${API_BASE_URL}/auth/login`,
  async ({ request }) => {
    await delay(260);
    const body = await readJson<{ email?: string; password?: string }>(request);
    if (!body?.email || !body.password) {
      return errorResponse(
        request,
        400,
        "VALIDATION_ERROR",
        "El correo y la contraseña son obligatorios.",
      );
    }

    const account = MOCK_ACCOUNTS.find(
      (item) => item.email.toLowerCase() === body.email?.toLowerCase(),
    );
    if (!account || body.password !== MOCK_PASSWORD) {
      return errorResponse(
        request,
        401,
        "INVALID_CREDENTIALS",
        "El correo o la contraseña no son correctos.",
      );
    }

    return HttpResponse.json({
      access_token: `mock-account:${account.accountId}`,
      token_type: "Bearer",
      expires_in: 3600,
      account: { account_id: account.accountId, email: account.email },
    });
  },
);

const profilesHandler = http.get(
  `${API_BASE_URL}/profiles`,
  async ({ request }) => {
    await delay(180);
    const token = parseToken(request);
    if (!token || token.kind !== "account") {
      return errorResponse(
        request,
        401,
        "UNAUTHORIZED",
        "La sesión no es válida o expiró.",
      );
    }
    const account = MOCK_ACCOUNTS.find(
      (item) => item.accountId === token.accountId,
    );
    if (!account) {
      return errorResponse(
        request,
        401,
        "UNAUTHORIZED",
        "La cuenta no existe.",
      );
    }
    return HttpResponse.json({ profiles: account.profiles });
  },
);

const selectProfileHandler = http.post(
  `${API_BASE_URL}/profiles/select`,
  async ({ request }) => {
    await delay(200);
    const token = parseToken(request);
    if (!token || token.kind !== "account") {
      return errorResponse(
        request,
        401,
        "UNAUTHORIZED",
        "La sesión no es válida o expiró.",
      );
    }

    const body = await readJson<{ profile_id?: string }>(request);
    if (!body?.profile_id) {
      return errorResponse(
        request,
        400,
        "VALIDATION_ERROR",
        "Falta el identificador del perfil.",
      );
    }

    const account = MOCK_ACCOUNTS.find(
      (item) => item.accountId === token.accountId,
    );
    if (!account) {
      return errorResponse(
        request,
        401,
        "UNAUTHORIZED",
        "La cuenta no existe.",
      );
    }

    const profile = account.profiles.find(
      (item) => item.profile_id === body.profile_id,
    );
    if (!profile) {
      const belongsToAnotherAccount = MOCK_ACCOUNTS.some(
        (item) =>
          item.accountId !== account.accountId &&
          item.profiles.some((entry) => entry.profile_id === body.profile_id),
      );
      return belongsToAnotherAccount
        ? errorResponse(
            request,
            403,
            "PROFILE_FORBIDDEN",
            "El perfil pertenece a otra cuenta.",
          )
        : errorResponse(
            request,
            404,
            "PROFILE_NOT_FOUND",
            "El perfil no existe.",
          );
    }

    return HttpResponse.json({
      access_token: `mock-profile:${account.accountId}:${profile.profile_id}`,
      token_type: "Bearer",
      expires_in: 3600,
      profile,
    });
  },
);

const homeHandler = http.get(
  `${API_BASE_URL}/home`,
  async ({ request }) => {
    await delay(220);
    const auth = requireProfileToken(request);
    if ("response" in auth) {
      return auth.response;
    }
    return HttpResponse.json(buildHome(auth.token.profileId));
  },
);

const telemetryPingHandler = http.post(
  `${API_BASE_URL}/telemetry/ping`,
  async ({ request }) => {
    const auth = requireProfileToken(request);
    if ("response" in auth) {
      return auth.response;
    }

    const body = await readJson<{
      event_id?: string;
      content_id?: string;
      progress_seconds?: number;
      timestamp?: string;
    }>(request);

    if (
      !body?.event_id ||
      !UUID_PATTERN.test(body.event_id) ||
      !body.content_id ||
      typeof body.progress_seconds !== "number" ||
      body.progress_seconds < 0 ||
      !body.timestamp
    ) {
      return errorResponse(
        request,
        400,
        "VALIDATION_ERROR",
        "El ping de reproducción no es válido.",
      );
    }

    await delay(80);
    return HttpResponse.json(
      { event_id: body.event_id, status: "ACCEPTED" },
      { status: 202 },
    );
  },
);

const interactionLikeHandler = http.post(
  `${API_BASE_URL}/interactions/like`,
  async ({ request }) => {
    const auth = requireProfileToken(request);
    if ("response" in auth) {
      return auth.response;
    }

    const body = await readJson<{
      event_id?: string;
      content_id?: string;
      rating?: string;
      timestamp?: string;
    }>(request);

    if (
      !body?.event_id ||
      !UUID_PATTERN.test(body.event_id) ||
      !body.content_id ||
      (body.rating !== "LIKE" && body.rating !== "DISLIKE") ||
      !body.timestamp
    ) {
      return errorResponse(
        request,
        400,
        "VALIDATION_ERROR",
        "La valoración no es válida.",
      );
    }

    await delay(120);
    return HttpResponse.json(
      { event_id: body.event_id, status: "ACCEPTED" },
      { status: 202 },
    );
  },
);

/** Auth and profile endpoints implemented by auth-service. */
export const authHandlers = [
  loginHandler,
  profilesHandler,
  selectProfileHandler,
];

/** Endpoints whose services are not implemented yet. */
export const missingHandlers = [telemetryPingHandler, interactionLikeHandler];

/** Every endpoint, for offline demos and E2E. */
export const allHandlers = [...authHandlers, homeHandler, ...missingHandlers];
