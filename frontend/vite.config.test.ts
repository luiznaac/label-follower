import { afterEach, beforeEach, describe, expect, it } from "vitest";
import config from "./vite.config";

// npm test runs with `frontend/` as the cwd, so loadEnv reads the committed
// .env.development — these assertions pin the dev server's slice of the port block.
function devServer() {
  const resolved = config({ mode: "development", command: "serve" });
  return resolved.server ?? {};
}

function clearPortOverrides() {
  Reflect.deleteProperty(process.env, "FRONTEND_DEV_PORT");
  Reflect.deleteProperty(process.env, "VITE_API_TARGET");
}

describe("vite dev server", () => {
  // A shell that exports either key would otherwise shadow the committed value.
  beforeEach(clearPortOverrides);
  afterEach(clearPortOverrides);

  it("serves the block's frontend-dev port strictly on the loopback host", () => {
    const server = devServer();
    expect(server.host).toBe("127.0.0.1");
    expect(server.port).toBe(8123);
    expect(server.strictPort).toBe(true);
  });

  it("proxies /api to the backend's block port", () => {
    expect(devServer().proxy?.["/api"]).toMatchObject({ target: "http://localhost:8120" });
  });

  it("lets an explicit FRONTEND_DEV_PORT override the committed port", () => {
    process.env.FRONTEND_DEV_PORT = "9999";
    expect(devServer().port).toBe(9999);
  });
});
