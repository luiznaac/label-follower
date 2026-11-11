import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";

// Served under `/label-follower/` behind the unified dashboard reverse proxy,
// at `/` in local dev and in the Docker image. Override with VITE_BASE.
export default defineConfig(({ mode }) => {
  // Vite does not inject .env files into process.env for config files — loadEnv is the
  // documented way to read them here (a real process.env value still wins over the file).
  const env = loadEnv(mode, process.cwd(), "");
  return {
    base: env.VITE_BASE ?? (mode === "production" ? "/label-follower/" : "/"),
    plugins: [react(), tailwindcss()],
    server: {
      // 127.0.0.1 (not "localhost") so the origin matches the Spotify PKCE
      // loopback redirect URI exactly — Spotify rejects http://localhost.
      host: "127.0.0.1",
      // The frontend-dev role of this project's port block (README's table). strictPort
      // fails loudly instead of drifting away from the origin the Spotify redirect is
      // registered against.
      port: Number(env.FRONTEND_DEV_PORT ?? 8123),
      strictPort: true,
      proxy: {
        // Dev-only: forward API calls to the label-follower backend to dodge CORS.
        "/api": {
          target: env.VITE_API_TARGET ?? "http://localhost:8120",
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/api/, ""),
        },
      },
    },
  };
});
