import { describe, expect, it } from "vitest";
import { clockOffset, remainingSeconds } from "./countdown";

describe("Restzeit", () => {
  const endsAt = "2026-11-01T10:00:30Z";
  const serverNow = "2026-11-01T10:00:00Z";

  it("zeigt die volle Zeit, wenn Server- und Geräteuhr übereinstimmen", () => {
    const device = Date.parse(serverNow);
    expect(remainingSeconds(endsAt, clockOffset(serverNow, device), device)).toBe(30);
  });

  it("gleicht eine vorgestellte Geräteuhr aus", () => {
    const device = Date.parse(serverNow) + 120_000;
    const offset = clockOffset(serverNow, device);
    expect(remainingSeconds(endsAt, offset, device)).toBe(30);
    expect(remainingSeconds(endsAt, offset, device + 10_000)).toBe(20);
  });

  it("rundet auf und wird nie negativ", () => {
    const device = Date.parse(serverNow);
    expect(remainingSeconds(endsAt, 0, device + 29_100)).toBe(1);
    expect(remainingSeconds(endsAt, 0, device + 30_000)).toBe(0);
    expect(remainingSeconds(endsAt, 0, device + 45_000)).toBe(0);
  });
});
