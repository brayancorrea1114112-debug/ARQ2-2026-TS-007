export function safeImageUrl(value?: string | null): string | null {
  if (!value) {
    return null;
  }
  if (value.startsWith("/")) {
    return value;
  }
  try {
    const url = new URL(value);
    if (
      url.protocol !== "http:" &&
      url.protocol !== "https:"
    ) {
      return null;
    }
    // The spec and the auth seed use example.com as a placeholder CDN.
    // Loading it only produces net::ERR_FAILED noise in the console.
    if (url.hostname === "example.com" || url.hostname.endsWith(".example.com")) {
      return null;
    }
    return value;
  } catch {
    return null;
  }
}
