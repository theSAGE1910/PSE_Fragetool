import type { Me, RunState, SessionState } from "../api/types";

// Testdaten nach Abschnitt 8.1 des Pflichtenhefts (Frage F1, Session "Rechnernetze VL 05").

export const SERVER_NOW = "2026-11-16T10:00:00Z";

export function runningF1(overrides: Partial<RunState> = {}): RunState {
  return {
    id: "run-1",
    status: "RUNNING",
    startedAt: SERVER_NOW,
    endsAt: "2026-11-16T10:00:30Z",
    answerCount: 0,
    freeTextReleased: false,
    question: {
      type: "SINGLE_CHOICE",
      text: "Auf welcher OSI-Schicht arbeitet IP?",
      quickStart: false,
      gradable: true,
      imageUrl: null,
      options: [
        { id: "o1", text: "Schicht 2" },
        { id: "o2", text: "Schicht 3" },
        { id: "o3", text: "Schicht 4" },
      ],
    },
    result: null,
    ...overrides,
  };
}

export function closedF1(): RunState {
  return runningF1({
    status: "CLOSED",
    answerCount: 2,
    result: {
      solution: "Schicht 3",
      distribution: [
        { label: "Schicht 2", count: 1, correct: false },
        { label: "Schicht 3", count: 1, correct: true },
        { label: "Schicht 4", count: 0, correct: false },
      ],
    },
  });
}

export function sessionState(overrides: Partial<SessionState> = {}): SessionState {
  return {
    sessionName: "Rechnernetze VL 05",
    joinCode: "RN5K2P",
    serverNow: SERVER_NOW,
    participantCount: 3,
    scoreboardVisible: false,
    scoreboard: [],
    run: null,
    ...overrides,
  };
}

export function me(overrides: Partial<Me> = {}): Me {
  return {
    displayName: "K7X",
    points: 0,
    rank: 1,
    participantCount: 3,
    sessionName: "Rechnernetze VL 05",
    joinCode: "RN5K2P",
    lastRunId: null,
    answered: false,
    correct: null,
    ...overrides,
  };
}
