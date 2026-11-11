import { defineConfig } from "vitest/config";

// src/lib/** holds the pure logic and vite.config.test.ts pins the dev server's port wiring.
// No React rendering — see AGENTS.md: this app deliberately doesn't set up component tests yet.
export default defineConfig({
  test: { include: ["src/lib/**/*.test.ts", "vite.config.test.ts"], environment: "node" },
});
