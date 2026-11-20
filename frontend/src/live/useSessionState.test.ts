// @vitest-environment jsdom
import { act, renderHook, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../api/http";
import { publicApi } from "../api/participant";
import { runningF1, sessionState } from "../test/fixtures";
import { HEARTBEAT_MS, RECONNECT_DELAY_MS, useSessionState } from "./useSessionState";

// Ersatz für den STOMP-Client: merkt sich Konfiguration und Abonnement.
const stomp = vi.hoisted(() => {
  interface FakeClient {
    config: Record<string, unknown> & { onConnect: () => void; onWebSocketClose: () => void };
    destination?: string;
    onMessage?: (frame: { body: string }) => void;
    activated: boolean;
    deactivated: boolean;
  }
  return { clients: [] as FakeClient[] };
});

vi.mock("@stomp/stompjs", () => ({
  Client: class {
    config: never;
    activated = false;
    deactivated = false;
    destination?: string;
    onMessage?: (frame: { body: string }) => void;
    constructor(config: never) {
      this.config = config;
      stomp.clients.push(this as never);
    }
    activate() {
      this.activated = true;
    }
    deactivate() {
      this.deactivated = true;
      return Promise.resolve();
    }
    subscribe(destination: string, onMessage: (frame: { body: string }) => void) {
      this.destination = destination;
      this.onMessage = onMessage;
      return { unsubscribe() {} };
    }
  },
}));

vi.mock("../api/participant", () => ({
  publicApi: { state: vi.fn() },
}));

const state = vi.mocked(publicApi.state);

function client() {
  const last = stomp.clients[stomp.clients.length - 1];
  if (!last) {
    throw new Error("Kein STOMP-Client erzeugt");
  }
  return last;
}

describe("useSessionState: Live-Aktualisierung der Ansichten", () => {
  beforeEach(() => {
    stomp.clients.length = 0;
    state.mockReset();
    state.mockResolvedValue(sessionState());
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("/MK410/ verbindet sich nach einem Abbruch spätestens nach 5 s neu, mit Heartbeat", () => {
    renderHook(() => useSessionState("RN5K2P"));
    expect(RECONNECT_DELAY_MS).toBeLessThanOrEqual(5000);
    expect(client().config.reconnectDelay).toBe(RECONNECT_DELAY_MS);
    expect(client().config.heartbeatIncoming).toBe(HEARTBEAT_MS);
    expect(client().config.heartbeatOutgoing).toBe(HEARTBEAT_MS);
    expect(client().config.brokerURL).toMatch(/^ws:\/\/.+\/ws$/);
    expect(client().activated).toBe(true);
  });

  it("/MK220/ lädt den Zustand beim Öffnen und abonniert die eigene Session", async () => {
    const { result } = renderHook(() => useSessionState("RN5K2P"));
    await waitFor(() => expect(result.current.state?.sessionName).toBe("Rechnernetze VL 05"));
    expect(state).toHaveBeenCalledWith("RN5K2P");

    act(() => client().config.onConnect());
    expect(client().destination).toBe("/topic/session/RN5K2P");
    expect(result.current.connected).toBe(true);
  });

  it("/MK220/ lädt bei STATE_CHANGED ohne Neuladen der Seite den neuen Zustand", async () => {
    const { result } = renderHook(() => useSessionState("RN5K2P"));
    act(() => client().config.onConnect());
    await waitFor(() => expect(result.current.state).not.toBeNull());

    state.mockResolvedValue(sessionState({ run: runningF1() }));
    act(() => client().onMessage?.({ body: JSON.stringify({ type: "STATE_CHANGED", value: null }) }));
    await waitFor(() => expect(result.current.state?.run?.status).toBe("RUNNING"));
  });

  it("/MK300/ übernimmt die Antwortzahl ohne weitere Anfrage", async () => {
    state.mockResolvedValue(sessionState({ run: runningF1() }));
    const { result } = renderHook(() => useSessionState("RN5K2P"));
    act(() => client().config.onConnect());
    await waitFor(() => expect(result.current.state?.run).not.toBeNull());
    const calls = state.mock.calls.length;

    act(() => client().onMessage?.({ body: JSON.stringify({ type: "ANSWER_COUNT", value: 7 }) }));
    act(() => client().onMessage?.({ body: JSON.stringify({ type: "PARTICIPANT_COUNT", value: 12 }) }));
    expect(result.current.state?.run?.answerCount).toBe(7);
    expect(result.current.state?.participantCount).toBe(12);
    expect(state.mock.calls.length).toBe(calls);
  });

  it("/MK410/ meldet den Abbruch und lädt nach der Wiederverbindung den Zustand neu", async () => {
    const { result } = renderHook(() => useSessionState("RN5K2P"));
    act(() => client().config.onConnect());
    await waitFor(() => expect(result.current.connected).toBe(true));

    act(() => client().config.onWebSocketClose());
    expect(result.current.connected).toBe(false);

    // Während der Unterbrechung wurde die Frage beendet
    state.mockResolvedValue(sessionState({ run: runningF1({ status: "CLOSED" }) }));
    act(() => client().config.onConnect());
    expect(result.current.connected).toBe(true);
    await waitFor(() => expect(result.current.state?.run?.status).toBe("CLOSED"));
  });

  it("/MK430/ rechnet mit dem Unterschied zwischen Server- und Geräteuhr", async () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    vi.setSystemTime(new Date("2026-11-16T10:02:00Z")); // Geräteuhr geht 2 Minuten vor
    const { result } = renderHook(() => useSessionState("RN5K2P"));
    await waitFor(() => expect(result.current.state).not.toBeNull());
    expect(result.current.offsetMs).toBe(-120_000);
  });

  it("meldet eine gelöschte Session", async () => {
    state.mockRejectedValue(new ApiError(404, "Session nicht gefunden."));
    const { result } = renderHook(() => useSessionState("WEG"));
    await waitFor(() => expect(result.current.notFound).toBe(true));
  });

  it("trennt die Verbindung beim Verlassen der Ansicht", () => {
    const { unmount } = renderHook(() => useSessionState("RN5K2P"));
    unmount();
    expect(client().deactivated).toBe(true);
  });
});
