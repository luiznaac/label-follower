import { describe, expect, it } from "vitest";
import { isValidIsrc, normalizeIsrc } from "./isrc.ts";

describe("normalizeIsrc", () => {
  it("removes spaces and hyphens", () => {
    expect(normalizeIsrc("US-RC1-17-00001")).toBe("USRC11700001");
    expect(normalizeIsrc("US RC1 17 00001")).toBe("USRC11700001");
  });

  it("upper-cases the result", () => {
    expect(normalizeIsrc("us-rc1-17-00001")).toBe("USRC11700001");
  });

  it("is idempotent", () => {
    const once = normalizeIsrc("us-rc1-17-00001");
    expect(normalizeIsrc(once)).toBe(once);
  });

  it("leaves an already canonical value untouched", () => {
    expect(normalizeIsrc("USRC11700001")).toBe("USRC11700001");
  });
});

describe("isValidIsrc", () => {
  it("accepts a canonical ISRC", () => {
    expect(isValidIsrc("USRC11700001")).toBe(true);
  });

  it("accepts hyphenated and lower-case input", () => {
    expect(isValidIsrc("US-RC1-17-00001")).toBe(true);
    expect(isValidIsrc("us-rc1-17-00001")).toBe(true);
  });

  it("rejects a wrong length", () => {
    expect(isValidIsrc("USRC1170000")).toBe(false);
    expect(isValidIsrc("USRC117000011")).toBe(false);
  });

  it("rejects a country code with digits or wrong case", () => {
    expect(isValidIsrc("U1RC11700001")).toBe(false);
  });

  it("rejects letters in the registrant's numeric part", () => {
    expect(isValidIsrc("USRC1170000A")).toBe(false);
  });

  it("rejects an empty or junk value", () => {
    expect(isValidIsrc("")).toBe(false);
    expect(isValidIsrc("não é um isrc")).toBe(false);
  });
});
