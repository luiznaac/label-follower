import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

// Served under `/label-follower/` behind the unified dashboard reverse proxy,
// at `/` in local dev and in the Docker image. Override with VITE_BASE.
export default defineConfig(({ mode }) => ({
  base: process.env.VITE_BASE ?? (mode === "production" ? "/label-follower/" : "/"),
  plugins: [react(), tailwindcss()],
  server: {
    // 127.0.0.1 (not "localhost") so the origin matches the Spotify PKCE
    // loopback redirect URI exactly — Spotify rejects http://localhost.
    host: "127.0.0.1",
    port: 5274, // shougong uses 5273; keep them distinct so both can run
    strictPort: true,
    proxy: {
      // Dev-only: forward API calls to the label-follower backend to dodge CORS.
      "/api": {
        target: process.env.VITE_API_TARGET ?? "http://localhost:8080",
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/api/, ""),
      },
    },
  },
}));
