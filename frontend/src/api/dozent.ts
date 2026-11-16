import { api } from "./http";
import type { QuestionDto, QuestionRequest, QuestionType, SessionDto } from "./types";

// Schnittstelle für angemeldete Dozenten. Die Anmeldung steckt im Session-Cookie.

export const auth = {
  me: () => api<{ email: string }>("GET", "/api/auth/me"),
  register: (email: string, password: string) => api<void>("POST", "/api/auth/register", { email, password }),
  login: (email: string, password: string) => api<{ email: string }>("POST", "/api/auth/login", { email, password }),
  logout: () => api<void>("POST", "/api/auth/logout"),
};

export const questions = {
  list: (tag?: string) =>
    api<QuestionDto[]>("GET", tag ? `/api/questions?tag=${encodeURIComponent(tag)}` : "/api/questions"),
  get: (id: string) => api<QuestionDto>("GET", `/api/questions/${id}`),
  create: (request: QuestionRequest) => api<QuestionDto>("POST", "/api/questions", request),
  update: (id: string, request: QuestionRequest) => api<QuestionDto>("PUT", `/api/questions/${id}`, request),
  remove: (id: string) => api<void>("DELETE", `/api/questions/${id}`),
  uploadImage: (id: string, file: File) => {
    const form = new FormData();
    form.append("file", file);
    return api<{ imageUrl: string }>("POST", `/api/questions/${id}/image`, form);
  },
  removeImage: (id: string) => api<void>("DELETE", `/api/questions/${id}/image`),
};

export const sessions = {
  list: () => api<SessionDto[]>("GET", "/api/sessions"),
  get: (id: string) => api<SessionDto>("GET", `/api/sessions/${id}`),
  create: (name: string) => api<SessionDto>("POST", "/api/sessions", { name }),
  remove: (id: string) => api<void>("DELETE", `/api/sessions/${id}`),
  setScoreboardVisible: (id: string, visible: boolean) =>
    api<void>("PUT", `/api/sessions/${id}/scoreboard-visible`, { visible }),
};

export const runs = {
  start: (sessionId: string, questionId: string) =>
    api<{ runId: string }>("POST", `/api/sessions/${sessionId}/runs`, { questionId }),
  quickStart: (sessionId: string, type: QuestionType, optionCount: number, timeLimitSeconds: number) =>
    api<{ runId: string }>("POST", `/api/sessions/${sessionId}/quickstart`, { type, optionCount, timeLimitSeconds }),
  close: (runId: string) => api<void>("POST", `/api/runs/${runId}/close`),
  setFreeTextReleased: (runId: string, released: boolean) =>
    api<void>("PUT", `/api/runs/${runId}/freetext-released`, { released }),
  freeTextAnswers: (runId: string) => api<string[]>("GET", `/api/runs/${runId}/freetext`),
};
