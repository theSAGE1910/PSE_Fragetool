import { type APIRequestContext, expect, request } from "@playwright/test";

/**
 * Testdaten über die REST-Schnittstelle anlegen, damit sich die Browsertests auf die
 * Oberfläche konzentrieren. Entspricht den Testdaten aus Abschnitt 8.1.
 */
export const PASSWORD = "Sicher123!";

export const F1 = {
  type: "SINGLE_CHOICE",
  text: "Auf welcher OSI-Schicht arbeitet IP?",
  timeLimitSeconds: 30,
  tags: ["ip"],
  options: [
    { text: "Schicht 2", correct: false },
    { text: "Schicht 3", correct: true },
    { text: "Schicht 4", correct: false },
  ],
};

export function uniqueEmail(): string {
  return `e2e-${Date.now()}-${Math.floor(Math.random() * 1e6)}@test.de`;
}

/** Ein angemeldeter Dozent mit eigener Cookie-Sitzung. */
export class Dozent {
  private constructor(readonly api: APIRequestContext, readonly email: string) {}

  static async create(baseURL: string): Promise<Dozent> {
    const api = await request.newContext({ baseURL });
    const email = uniqueEmail();
    expect((await api.post("/api/auth/register", { data: { email, password: PASSWORD } })).status()).toBe(201);
    expect((await api.post("/api/auth/login", { data: { email, password: PASSWORD } })).ok()).toBe(true);
    return new Dozent(api, email);
  }

  async question(body: object = F1): Promise<{ id: string }> {
    const response = await this.api.post("/api/questions", { data: body });
    expect(response.status()).toBe(201);
    return response.json();
  }

  async session(name = "Rechnernetze VL 05"): Promise<{ id: string; joinCode: string }> {
    const response = await this.api.post("/api/sessions", { data: { name } });
    expect(response.status()).toBe(201);
    return response.json();
  }

  async start(sessionId: string, questionId: string): Promise<string> {
    const response = await this.api.post(`/api/sessions/${sessionId}/runs`, { data: { questionId } });
    expect(response.status()).toBe(201);
    return (await response.json()).runId;
  }

  async close(runId: string): Promise<void> {
    expect((await this.api.post(`/api/runs/${runId}/close`)).status()).toBe(204);
  }

  async dispose(): Promise<void> {
    await this.api.dispose();
  }
}
