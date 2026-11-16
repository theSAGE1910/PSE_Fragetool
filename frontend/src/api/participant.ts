import { api } from "./http";
import type { AnswerRequest, Me, SessionState } from "./types";

// Schnittstelle ohne Anmeldung: Teilnehmer und Präsentationsansicht.

const TOKEN_HEADER = "X-Participant-Token";

export const publicApi = {
  state: (joinCode: string) => api<SessionState>("GET", `/api/public/sessions/${joinCode}/state`),
  join: (joinCode: string) =>
    api<{ displayName: string; token: string }>("POST", `/api/public/sessions/${joinCode}/participants`),
  me: (token: string) => api<Me>("GET", "/api/public/participants/me", undefined, { [TOKEN_HEADER]: token }),
  answer: (token: string, runId: string, answer: AnswerRequest) =>
    api<void>("POST", `/api/public/runs/${runId}/answers`, answer, { [TOKEN_HEADER]: token }),
};

/**
 * Das Token erkennt den Teilnehmer nach dem Neuladen wieder (/MK180/).
 * Es liegt im localStorage dieses Browsers, getrennt nach Session.
 */
export const tokenStore = {
  get(joinCode: string): string | null {
    try {
      return localStorage.getItem(key(joinCode));
    } catch {
      return null;
    }
  },
  set(joinCode: string, token: string): void {
    try {
      localStorage.setItem(key(joinCode), token);
    } catch {
      // Ohne localStorage (z. B. privater Modus) gilt das Token nur bis zum Neuladen.
    }
  },
  clear(joinCode: string): void {
    try {
      localStorage.removeItem(key(joinCode));
    } catch {
      // nichts zu tun
    }
  },
};

function key(joinCode: string): string {
  return `fragetool.token.${joinCode}`;
}
