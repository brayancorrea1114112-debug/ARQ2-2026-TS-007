import { humanizeId } from "@/shared/lib/format";

export type ProfileType = "ADULT" | "KIDS";
export type Rating = "LIKE" | "DISLIKE";
export type HomeSource = "cache" | "fallback";

export interface AccountSummary {
  account_id: string;
  email: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
  account: AccountSummary;
}

export interface ProfileSummary {
  profile_id: string;
  name: string;
  type: ProfileType;
  avatar_url?: string | null;
}

export interface ProfilesResponse {
  profiles: ProfileSummary[];
}

export interface SelectProfileRequest {
  profile_id: string;
}

export interface SelectProfileResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
  profile: ProfileSummary;
}

export interface HomeTitle {
  content_id: string;
  name: string;
  poster?: string | null;
  synopsis?: string | null;
  duration_seconds?: number | null;
  age_rating?: string | null;
  genres?: string[];
}

export interface HomeCategory {
  name: string;
  titles: HomeTitle[];
}

export interface ContinueWatchingItem {
  content_id: string;
  episode_id?: string | null;
  progress_seconds: number;
}

export interface HomeView {
  profile_id: string;
  generated_at: string;
  source: HomeSource;
  categories: HomeCategory[];
  continue_watching: ContinueWatchingItem[];
  recommended: HomeTitle[];
}

export interface PingRequest {
  event_id: string;
  content_id: string;
  episode_id?: string;
  progress_seconds: number;
  timestamp: string;
}

export interface LikeRequest {
  event_id: string;
  content_id: string;
  rating: Rating;
  timestamp: string;
}

export interface AcceptedResponse {
  event_id: string;
  status: "ACCEPTED";
}

export interface ApiErrorBody {
  error: {
    code: string;
    message: string;
    correlation_id: string;
  };
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value !== null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function asString(value: unknown): string | null {
  return typeof value === "string" && value.length > 0 ? value : null;
}

function asNumber(value: unknown): number | null {
  return typeof value === "number" && Number.isFinite(value) ? value : null;
}

function toStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.filter((item): item is string => typeof item === "string");
}

export function toHomeTitle(value: unknown): HomeTitle | null {
  if (typeof value === "string") {
    return { content_id: value, name: humanizeId(value) };
  }

  const record = asRecord(value);
  if (!record) {
    return null;
  }

  const contentId =
    asString(record.content_id) ?? asString(record._id) ?? asString(record.id);
  if (!contentId) {
    return null;
  }

  const images = asRecord(record.images);
  const poster = asString(record.poster) ?? asString(images?.poster);

  return {
    content_id: contentId,
    name:
      asString(record.name) ??
      asString(record.title) ??
      humanizeId(contentId),
    poster,
    synopsis: asString(record.synopsis) ?? asString(record.description),
    duration_seconds: asNumber(record.duration_seconds),
    age_rating: asString(record.age_rating),
    genres: toStringArray(record.genres),
  };
}

function toCategory(value: unknown): HomeCategory | null {
  const record = asRecord(value);
  if (!record) {
    return null;
  }

  const name = asString(record.name);
  if (!name) {
    return null;
  }

  const source = Array.isArray(record.titles)
    ? record.titles
    : toStringArray(record.title_ids);
  const titles = source
    .map(toHomeTitle)
    .filter((title): title is HomeTitle => title !== null);

  return { name, titles };
}

function toContinueWatching(value: unknown): ContinueWatchingItem | null {
  const record = asRecord(value);
  if (!record) {
    return null;
  }
  const contentId = asString(record.content_id);
  if (!contentId) {
    return null;
  }
  return {
    content_id: contentId,
    episode_id: asString(record.episode_id),
    progress_seconds: asNumber(record.progress_seconds) ?? 0,
  };
}

/**
 * Accepts the shape published by the Catalog & Home Service, but also tolerates
 * the raw Redis projection, where `recommended` is a list of ids and categories
 * use `title_ids`.
 */
export function normalizeHome(raw: unknown): HomeView {
  const record = asRecord(raw) ?? {};

  const categories = Array.isArray(record.categories)
    ? record.categories
        .map(toCategory)
        .filter((category): category is HomeCategory => category !== null)
    : [];

  const recommended = Array.isArray(record.recommended)
    ? record.recommended
        .map(toHomeTitle)
        .filter((title): title is HomeTitle => title !== null)
    : [];

  const continueWatching = Array.isArray(record.continue_watching)
    ? record.continue_watching
        .map(toContinueWatching)
        .filter(
          (item): item is ContinueWatchingItem => item !== null,
        )
    : [];

  return {
    profile_id: asString(record.profile_id) ?? "",
    generated_at: asString(record.generated_at) ?? new Date().toISOString(),
    source: record.source === "cache" ? "cache" : "fallback",
    categories,
    continue_watching: continueWatching,
    recommended,
  };
}
