// Authorization Code + PKCE against Spotify, entirely client-side. The backend is
// never involved — it only does client-credentials for its own reads. The client
// id is public (same value as backend/src/main/resources/application.properties).
//
// Token exchange and refresh both hit https://accounts.spotify.com/api/token
// directly; Spotify serves CORS for that endpoint to public PKCE clients.

const CLIENT_ID = import.meta.env.VITE_SPOTIFY_CLIENT_ID ?? "5a5af3ea4d104213872ebff79136fcda";

const REDIRECT_URI =
  import.meta.env.VITE_SPOTIFY_REDIRECT_URI ??
  `${window.location.origin}${import.meta.env.BASE_URL}callback`;

const SCOPES = "playlist-modify-public playlist-modify-private";
const AUTHORIZE_URL = "https://accounts.spotify.com/authorize";
const TOKEN_URL = "https://accounts.spotify.com/api/token";

const TOKEN_KEY = "lf.spotify.token";
const VERIFIER_KEY = "lf.spotify.verifier";

interface StoredToken {
  access_token: string;
  refresh_token: string;
  expires_at: number; // epoch ms, already includes a safety margin
}

interface TokenResponse {
  access_token: string;
  refresh_token?: string;
  expires_in: number;
}

const ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";

function randomString(length: number): string {
  const bytes = crypto.getRandomValues(new Uint8Array(length));
  let out = "";
  for (const b of bytes) out += ALPHABET[b % ALPHABET.length];
  return out;
}

function base64Url(bytes: ArrayBuffer): string {
  let binary = "";
  for (const b of new Uint8Array(bytes)) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

async function codeChallenge(verifier: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier));
  return base64Url(digest);
}

function readToken(): StoredToken | null {
  const raw = localStorage.getItem(TOKEN_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as StoredToken;
  } catch {
    return null;
  }
}

function persistToken(res: TokenResponse): void {
  const prev = readToken();
  const stored: StoredToken = {
    access_token: res.access_token,
    refresh_token: res.refresh_token ?? prev?.refresh_token ?? "",
    expires_at: Date.now() + (res.expires_in - 60) * 1000,
  };
  localStorage.setItem(TOKEN_KEY, JSON.stringify(stored));
}

export function isSpotifyConnected(): boolean {
  return readToken() !== null;
}

export function disconnectSpotify(): void {
  localStorage.removeItem(TOKEN_KEY);
}

/** Redirect to Spotify's consent screen. */
export async function beginSpotifyLogin(): Promise<void> {
  const verifier = randomString(64);
  sessionStorage.setItem(VERIFIER_KEY, verifier);
  const params = new URLSearchParams({
    client_id: CLIENT_ID,
    response_type: "code",
    redirect_uri: REDIRECT_URI,
    scope: SCOPES,
    code_challenge_method: "S256",
    code_challenge: await codeChallenge(verifier),
  });
  window.location.assign(`${AUTHORIZE_URL}?${params.toString()}`);
}

/** Called by the /callback page with the `code` from the query string. */
export async function completeSpotifyLogin(code: string): Promise<void> {
  const verifier = sessionStorage.getItem(VERIFIER_KEY);
  if (!verifier) throw new Error("Sessão de login expirada. Tente conectar novamente.");

  const res = await fetch(TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      client_id: CLIENT_ID,
      grant_type: "authorization_code",
      code,
      redirect_uri: REDIRECT_URI,
      code_verifier: verifier,
    }).toString(),
  });
  if (!res.ok) throw new Error(`Falha na troca de token com o Spotify (${res.status}).`);

  persistToken((await res.json()) as TokenResponse);
  sessionStorage.removeItem(VERIFIER_KEY);
}

/** A non-expired access token, refreshing if needed; null if not connected. */
export async function getFreshAccessToken(): Promise<string | null> {
  const token = readToken();
  if (!token) return null;
  if (Date.now() < token.expires_at) return token.access_token;
  if (!token.refresh_token) {
    disconnectSpotify();
    return null;
  }

  const res = await fetch(TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      client_id: CLIENT_ID,
      grant_type: "refresh_token",
      refresh_token: token.refresh_token,
    }).toString(),
  });
  if (!res.ok) {
    disconnectSpotify();
    return null;
  }

  persistToken((await res.json()) as TokenResponse);
  return readToken()?.access_token ?? null;
}
