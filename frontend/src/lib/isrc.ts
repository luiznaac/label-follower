// ISRC — International Standard Recording Code. 12 chars: 2-letter country,
// 3 alphanumeric registrant, 2-digit year, 5-digit designation. Often written
// with hyphens (US-RC1-17-00001) — we strip those.

const ISRC_RE = /^[A-Z]{2}[A-Z0-9]{3}\d{7}$/;

/** Uppercase, drop spaces and hyphens. */
export function normalizeIsrc(raw: string): string {
  return raw.replace(/[\s-]/g, "").toUpperCase();
}

export function isValidIsrc(raw: string): boolean {
  return ISRC_RE.test(normalizeIsrc(raw));
}
