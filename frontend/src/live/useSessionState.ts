import { Client } from "@stomp/stompjs";
import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError } from "../api/http";
import { publicApi } from "../api/participant";
import type { LiveMessage, SessionState } from "../api/types";
import { clockOffset } from "./countdown";

/** Wartezeit bis zum nächsten Verbindungsversuch; /MK410/ verlangt höchstens 5 s. */
export const RECONNECT_DELAY_MS = 2000;
export const HEARTBEAT_MS = 10000;

export interface LiveSession {
  /** Aktueller Zustand der Session, null solange er lädt. */
  state: SessionState | null;
  /** Unterschied zwischen Serveruhr und Geräteuhr, für die Anzeige der Restzeit. */
  offsetMs: number;
  /** Ob die WebSocket-Verbindung steht (/MK410/). */
  connected: boolean;
  /** Ob die Session nicht (mehr) existiert. */
  notFound: boolean;
  /** Zustand sofort neu laden, z. B. direkt nach einer eigenen Aktion. */
  reload: () => Promise<void>;
}

/**
 * Hält eine Ansicht mit einer Session synchron.
 *
 * Ablauf: Zustand per REST laden, WebSocket abonnieren, bei jeder STATE_CHANGED-Nachricht
 * neu laden. Bricht die Verbindung ab, verbindet sich der Client nach 2 Sekunden selbst neu
 * und lädt den Zustand erneut. So gehen verpasste Änderungen nicht verloren.
 *
 * @param onStateChanged wird nach jedem Neuladen aufgerufen, z. B. um eigene Daten nachzuladen
 */
export function useSessionState(joinCode: string, onStateChanged?: () => void): LiveSession {
  const [state, setState] = useState<SessionState | null>(null);
  const [offsetMs, setOffsetMs] = useState(0);
  const [connected, setConnected] = useState(false);
  const [notFound, setNotFound] = useState(false);
  const callback = useRef(onStateChanged);
  callback.current = onStateChanged;

  const reload = useCallback(async () => {
    try {
      const fresh = await publicApi.state(joinCode);
      setState(fresh);
      setOffsetMs(clockOffset(fresh.serverNow, Date.now()));
      setNotFound(false);
      callback.current?.();
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        setNotFound(true);
      }
      // Bei Verbindungsfehlern bleibt der alte Zustand stehen; nach der Wiederverbindung wird neu geladen.
    }
  }, [joinCode]);

  useEffect(() => {
    void reload();
    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    const client = new Client({
      brokerURL: `${protocol}://${window.location.host}/ws`,
      reconnectDelay: RECONNECT_DELAY_MS,
      heartbeatIncoming: HEARTBEAT_MS,
      heartbeatOutgoing: HEARTBEAT_MS,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/session/${joinCode}`, (frame) => {
          const message = JSON.parse(frame.body) as LiveMessage;
          if (message.type === "STATE_CHANGED") {
            void reload();
          } else if (message.type === "ANSWER_COUNT") {
            setState((old) =>
              old?.run ? { ...old, run: { ...old.run, answerCount: message.value ?? old.run.answerCount } } : old,
            );
          } else if (message.type === "PARTICIPANT_COUNT") {
            setState((old) => (old ? { ...old, participantCount: message.value ?? old.participantCount } : old));
          }
        });
        // Nach jeder (Wieder-)Verbindung den Zustand laden: Es könnten Nachrichten gefehlt haben.
        void reload();
      },
      onWebSocketClose: () => setConnected(false),
    });
    client.activate();
    return () => {
      void client.deactivate();
    };
  }, [joinCode, reload]);

  return { state, offsetMs, connected, notFound, reload };
}
