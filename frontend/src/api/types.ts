// Datentypen der REST-Schnittstelle. Sie entsprechen den DTO-Klassen im Backend.

export type QuestionType = "SINGLE_CHOICE" | "MULTIPLE_CHOICE" | "NUMERIC" | "FREE_TEXT";

export const QUESTION_TYPE_LABELS: Record<QuestionType, string> = {
  SINGLE_CHOICE: "Single Choice",
  MULTIPLE_CHOICE: "Multiple Choice",
  NUMERIC: "Numerisch",
  FREE_TEXT: "Freitext",
};

export interface OptionDraft {
  text: string;
  correct: boolean;
}

export interface QuestionDto {
  id: string;
  type: QuestionType;
  text: string;
  timeLimitSeconds: number;
  tags: string[];
  options: { id: string; text: string; correct: boolean }[];
  correctValue: number | null;
  tolerance: number | null;
  imageUrl: string | null;
}

export interface QuestionRequest {
  type: QuestionType;
  text: string;
  timeLimitSeconds: number;
  tags: string[];
  options: OptionDraft[];
  correctValue: number | null;
  tolerance: number | null;
}

export interface SessionDto {
  id: string;
  name: string;
  joinCode: string;
  createdAt: string;
}

export interface ScoreboardEntry {
  rank: number;
  displayName: string;
  points: number;
}

export interface DistributionEntry {
  label: string;
  count: number;
  /** null bei nicht bewertbaren Fragen */
  correct: boolean | null;
}

export interface RunState {
  id: string;
  status: "RUNNING" | "CLOSED";
  startedAt: string;
  endsAt: string;
  answerCount: number;
  freeTextReleased: boolean;
  question: {
    type: QuestionType;
    text: string;
    quickStart: boolean;
    gradable: boolean;
    imageUrl: string | null;
    options: { id: string; text: string }[];
  };
  /** erst nach Fragenende vorhanden */
  result: { distribution: DistributionEntry[]; solution: string | null } | null;
}

export interface SessionState {
  sessionName: string;
  joinCode: string;
  serverNow: string;
  participantCount: number;
  scoreboardVisible: boolean;
  scoreboard: ScoreboardEntry[];
  run: RunState | null;
}

export interface Me {
  displayName: string;
  points: number;
  rank: number;
  participantCount: number;
  sessionName: string;
  joinCode: string;
  lastRunId: string | null;
  answered: boolean;
  correct: boolean | null;
}

export interface AnswerRequest {
  optionIds?: string[];
  numericValue?: number;
  text?: string;
}

export interface LiveMessage {
  type: "STATE_CHANGED" | "ANSWER_COUNT" | "PARTICIPANT_COUNT";
  value: number | null;
  serverNow: string;
}
