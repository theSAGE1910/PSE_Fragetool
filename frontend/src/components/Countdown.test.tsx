// @vitest-environment jsdom
import { act, cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { Countdown } from "./Countdown";

describe("Countdown", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    cleanup();
    vi.useRealTimers();
  });

  it("/MK230/ zeigt die Restzeit und zählt herunter", () => {
    vi.setSystemTime(new Date("2026-11-16T10:00:00Z"));
    render(<Countdown endsAt="2026-11-16T10:00:30Z" offsetMs={0} />);
    expect(screen.getByRole("timer").textContent).toBe("30s");

    act(() => vi.advanceTimersByTime(10_000));
    expect(screen.getByRole("timer").textContent).toBe("20s");

    act(() => vi.advanceTimersByTime(25_000));
    expect(screen.getByRole("timer").textContent).toBe("0s");
  });

  it("/MK230/, /MK430/ gleicht eine falsch gehende Geräteuhr mit der Serverzeit aus", () => {
    // Geräteuhr geht 2 Minuten vor; der Unterschied kommt vom Server (serverNow)
    vi.setSystemTime(new Date("2026-11-16T10:02:00Z"));
    render(<Countdown endsAt="2026-11-16T10:00:30Z" offsetMs={-120_000} />);
    expect(screen.getByRole("timer").textContent).toBe("30s");
  });
});
