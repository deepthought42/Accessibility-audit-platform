/**
 * Permissive URL parsing — accepts anything the URL constructor can parse,
 * auto-prepends https:// when scheme is missing, and only rejects obviously-
 * invalid hostnames.
 *
 * See docs/design/05-landing-and-onboarding.md §1.
 *
 * Replaces the legacy hardcoded-TLD regex (which rejected valid URLs like
 * `.tech`, `.design`, `.store`, etc.).
 */

export type NormaliseUrlSuccess = { ok: true; url: string; schemeAdded: boolean };
export type NormaliseUrlError = { ok: false; error: string };
export type NormaliseUrlResult = NormaliseUrlSuccess | NormaliseUrlError;

export function normaliseUrl(input: string | null | undefined): NormaliseUrlResult {
  const trimmed = (input ?? '').trim();
  if (!trimmed) {
    return { ok: false, error: 'Enter a website URL.' };
  }

  const hasScheme = /^https?:\/\//i.test(trimmed);
  const withScheme = hasScheme ? trimmed : `https://${trimmed}`;

  let parsed: URL;
  try {
    parsed = new URL(withScheme);
  } catch {
    return { ok: false, error: "That doesn't look like a valid website address." };
  }

  const host = parsed.hostname;

  if (!host || !host.includes('.') || host.endsWith('.') || host.startsWith('.')) {
    return { ok: false, error: "That doesn't look like a valid website address." };
  }

  // Reject local / private ranges politely — we can't audit what we can't reach.
  const lowerHost = host.toLowerCase();
  if (
    lowerHost === 'localhost' ||
    /^127\./.test(host) ||
    /^10\./.test(host) ||
    /^192\.168\./.test(host) ||
    /^172\.(1[6-9]|2\d|3[01])\./.test(host) ||
    lowerHost.endsWith('.local') ||
    lowerHost.endsWith('.internal')
  ) {
    return { ok: false, error: "We can't audit local addresses. Try a public URL." };
  }

  return { ok: true, url: parsed.toString(), schemeAdded: !hasScheme };
}

/** Lightweight predicate for quick field validation. */
export function isValidWebsiteUrl(input: string | null | undefined): boolean {
  return normaliseUrl(input).ok;
}
