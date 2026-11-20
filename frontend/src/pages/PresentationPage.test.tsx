// @vitest-environment jsdom
import { cleanup, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { LiveSession } from "../live/useSessionState";
import { useSessionState } from "../live/useSessionState";
import { closedF1, runningF1, sessionState } from "../test/fixtures";
import { PresentationPage } from "./PresentationPage";

vi.mock("../live/useSessionState", () => ({ useSessionState: vi.fn() }));
const live = vi.mocked(useSessionState);

function show(session: Partial<LiveSession>) {
  live.mockReturnValue({ state: sessionState(), offsetMs: 0, connected: true, notFound: false, reload: vi.fn(async () => {}), ...session });
  render(
    <MemoryRouter initialEntries={["/present/RN5K2P"]}>
      <Routes>
        <Route path="/present/:code" element={<PresentationPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("Präsentationsansicht", () => {
  afterEach(() => {
    cleanup();
    vi.useRealTimers();
  });

  it("/MK140/, /MK150/ zeigt QR-Code und Teilnahme-Link groß für den Beamer", () => {
    show({});
    expect(screen.getByRole("heading", { name: "Jetzt beitreten" })).toBeTruthy();
    const link = `${window.location.origin}/join/RN5K2P`;
    expect(screen.getAllByText(link).length).toBeGreaterThan(0);
    expect(document.querySelector("svg")).not.toBeNull();
    expect(screen.getByRole("button", { name: "Vollbild" })).toBeTruthy();
    expect(screen.getByText(/3 Teilnehmer/)).toBeTruthy();
  });

  it("/MK220/, /MK230/, /MK300/ zeigt laufende Frage, Restzeit und Anzahl der Antworten", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-11-16T10:00:10Z"));
    show({ state: sessionState({ run: runningF1({ answerCount: 1 }) }) });
    expect(screen.getByRole("heading", { name: "Auf welcher OSI-Schicht arbeitet IP?" })).toBeTruthy();
    expect(screen.getByText("Schicht 3")).toBeTruthy();
    expect(screen.getByRole("timer").textContent).toBe("20s");
    expect(screen.getByText("1 Antwort")).toBeTruthy();
    // Die Lösung ist während der Frage nicht sichtbar
    expect(screen.queryByText(/Lösung/)).toBeNull();
  });

  it("/MK270/, /MK290/ zeigt nach dem Ende Verteilung und Lösung", () => {
    show({ state: sessionState({ run: closedF1() }) });
    expect(screen.getByText("Lösung:")).toBeTruthy();
    expect(screen.getAllByText("Schicht 3").length).toBeGreaterThan(0);
    expect(screen.getByLabelText("korrekt")).toBeTruthy();
    expect(screen.getByText("2 Antworten")).toBeTruthy();
  });

  it("/MK295/ zeigt Freitextantworten erst nach Freigabe", () => {
    const text = closedF1();
    text.question.type = "FREE_TEXT";
    text.question.gradable = false;
    text.result = { solution: null, distribution: [] };
    show({ state: sessionState({ run: text }) });
    expect(screen.getByText("Die Freitextantworten sind noch nicht freigegeben.")).toBeTruthy();
    cleanup();

    text.freeTextReleased = true;
    text.result = { solution: null, distribution: [{ label: "Routing", count: 1, correct: null }] };
    show({ state: sessionState({ run: text }) });
    expect(screen.getByText("Routing")).toBeTruthy();
  });

  it("/MK320/, /MK335/ zeigt das Scoreboard mit gleichen Rängen", () => {
    show({
      state: sessionState({
        scoreboardVisible: true,
        scoreboard: [
          { rank: 1, displayName: "B4D", points: 2 },
          { rank: 1, displayName: "K7X", points: 2 },
          { rank: 3, displayName: "Z9Q", points: 1 },
        ],
      }),
    });
    expect(screen.getByRole("heading", { name: "Scoreboard" })).toBeTruthy();
    expect(screen.getAllByText("1.").length).toBe(2);
    expect(screen.getByText("3.")).toBeTruthy();
    expect(screen.getAllByRole("row").length).toBe(4);
  });

  it("/MK410/ zeigt einen Verbindungsabbruch an", () => {
    show({ connected: false });
    expect(screen.getByText("Verbindung unterbrochen")).toBeTruthy();
  });

  it("meldet eine nicht vorhandene Session", () => {
    show({ state: null, notFound: true });
    expect(screen.getByText("Session nicht gefunden.")).toBeTruthy();
  });
});
