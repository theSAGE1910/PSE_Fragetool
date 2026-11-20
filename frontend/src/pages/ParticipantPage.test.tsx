// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../api/http";
import { publicApi, tokenStore } from "../api/participant";
import type { LiveSession } from "../live/useSessionState";
import { useSessionState } from "../live/useSessionState";
import { closedF1, me, runningF1, sessionState } from "../test/fixtures";
import { ParticipantPage } from "./ParticipantPage";

vi.mock("../live/useSessionState", () => ({ useSessionState: vi.fn() }));
vi.mock("../api/participant", async (original) => {
  const actual = await original<typeof import("../api/participant")>();
  return {
    tokenStore: actual.tokenStore,
    publicApi: { state: vi.fn(), join: vi.fn(), me: vi.fn(), answer: vi.fn() },
  };
});

const live = vi.mocked(useSessionState);
const api = vi.mocked(publicApi);

function open(session: Partial<LiveSession> = {}) {
  live.mockReturnValue({ state: sessionState(), offsetMs: 0, connected: true, notFound: false, reload: vi.fn(async () => {}), ...session });
  render(
    <MemoryRouter initialEntries={["/join/RN5K2P"]}>
      <Routes>
        <Route path="/join/:code" element={<ParticipantPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("Teilnehmeransicht", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
    api.join.mockResolvedValue({ displayName: "K7X", token: "token-neu" });
    api.me.mockResolvedValue(me());
  });

  afterEach(() => {
    cleanup();
    vi.useRealTimers();
  });

  it("/MK160/, /MK170/ tritt ohne Registrierung bei und zeigt den vergebenen Namen", async () => {
    open();
    await screen.findByText("K7X");
    expect(api.join).toHaveBeenCalledWith("RN5K2P");
    expect(tokenStore.get("RN5K2P")).toBe("token-neu");
    expect(screen.getByText("Warte auf die nächste Frage.")).toBeTruthy();
  });

  it("/MK180/ erkennt den Teilnehmer nach dem Neuladen am gespeicherten Token", async () => {
    tokenStore.set("RN5K2P", "token-alt");
    api.me.mockResolvedValue(me({ displayName: "B4D", points: 3 }));
    open();
    await screen.findByText("B4D");
    expect(api.join).not.toHaveBeenCalled();
    expect(api.me).toHaveBeenCalledWith("token-alt");
    expect(screen.getByText("3")).toBeTruthy();
  });

  it("/MK180/ tritt neu bei, wenn das gespeicherte Token unbekannt ist", async () => {
    tokenStore.set("RN5K2P", "token-alt");
    api.me.mockRejectedValueOnce(new ApiError(404, "Teilnehmer nicht gefunden.")).mockResolvedValue(me());
    open();
    await waitFor(() => expect(api.join).toHaveBeenCalled());
    await screen.findByText("K7X");
  });

  it("/MK330/, /MK430/ zeigt Punkte und Rang, wie sie der Server liefert", async () => {
    api.me.mockResolvedValue(me({ points: 2, rank: 1, participantCount: 12 }));
    open();
    await screen.findByText("Rang 1 von 12");
    expect(screen.getByText("2")).toBeTruthy();
    expect(screen.getByText(/Punkte/)).toBeTruthy();
  });

  it("/MK240/, /MK245/ zeigt „Antwort gespeichert“ erst nach der Bestätigung des Servers", async () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    vi.setSystemTime(new Date("2026-11-16T10:00:05Z"));
    let confirm: () => void = () => {};
    api.answer.mockReturnValue(new Promise<void>((resolve) => { confirm = resolve; }));
    open({ state: sessionState({ run: runningF1() }) });

    fireEvent.click(await screen.findByRole("button", { name: "Schicht 3" }));
    fireEvent.click(screen.getByRole("button", { name: "Absenden" }));
    expect(api.answer).toHaveBeenCalledWith("token-neu", "run-1", { optionIds: ["o2"] });
    expect(screen.queryByText("Antwort gespeichert.")).toBeNull();

    confirm();
    await screen.findByText("Antwort gespeichert.");
  });

  it("/MK245/ meldet, wenn die Antwort nicht gespeichert wurde", async () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    vi.setSystemTime(new Date("2026-11-16T10:00:05Z"));
    api.answer.mockRejectedValue(new ApiError(0, "Keine Verbindung zum Server."));
    open({ state: sessionState({ run: runningF1() }) });

    fireEvent.click(await screen.findByRole("button", { name: "Schicht 2" }));
    fireEvent.click(screen.getByRole("button", { name: "Absenden" }));
    await screen.findByText("Antwort nicht gespeichert. Bitte erneut versuchen.");
    expect(screen.queryByText("Antwort gespeichert.")).toBeNull();
  });

  it("/MK250/ sperrt das Absenden nach Ablauf der Zeit", async () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    vi.setSystemTime(new Date("2026-11-16T10:00:31Z"));
    open({ state: sessionState({ run: runningF1() }) });
    await screen.findByText("Zeit abgelaufen.");
    expect((screen.getByRole("button", { name: "Absenden" }) as HTMLButtonElement).disabled).toBe(true);
  });

  it("/MK270/, /MK280/ zeigt nach dem Ende Lösung und ob die eigene Antwort richtig war", async () => {
    api.me.mockResolvedValue(me({ lastRunId: "run-1", answered: true, correct: true, points: 1 }));
    open({ state: sessionState({ run: closedF1() }) });
    await screen.findByText("Richtig");
    expect(screen.getByText("Schicht 3")).toBeTruthy();
  });

  it("/MK280/ zeigt „Falsch“ und „Keine Antwort“", async () => {
    api.me.mockResolvedValue(me({ lastRunId: "run-1", answered: true, correct: false }));
    open({ state: sessionState({ run: closedF1() }) });
    await screen.findByText("Falsch");
    cleanup();

    api.me.mockResolvedValue(me({ lastRunId: "run-1", answered: false }));
    open({ state: sessionState({ run: closedF1() }) });
    await screen.findByText("Keine Antwort.");
  });

  it("/MK90/, /MK200/ kennzeichnet nicht bewertete Fragen", async () => {
    const quick = closedF1();
    quick.question.quickStart = true;
    quick.question.gradable = false;
    api.me.mockResolvedValue(me({ lastRunId: "run-1", answered: true }));
    open({ state: sessionState({ run: quick }) });
    await screen.findByText(/Diese Frage wird nicht bewertet/);
  });

  it("/MK410/ zeigt einen Verbindungsabbruch an", async () => {
    open({ connected: false });
    await screen.findByText("Verbindung unterbrochen. Verbinde neu …");
  });
});
