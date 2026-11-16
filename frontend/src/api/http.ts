/** Fehler einer Anfrage. status 0 bedeutet: Der Server war nicht erreichbar. */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

type Method = "GET" | "POST" | "PUT" | "DELETE";

/**
 * Sendet eine JSON-Anfrage an das Backend.
 * Wirft einen ApiError mit der deutschen Meldung des Servers, wenn die Anfrage scheitert.
 */
export async function api<T>(
  method: Method,
  url: string,
  body?: unknown,
  headers: Record<string, string> = {},
): Promise<T> {
  const isForm = body instanceof FormData;
  let response: Response;
  try {
    response = await fetch(url, {
      method,
      credentials: "same-origin",
      // FormData (Datei-Upload) setzt seinen Content-Type selbst.
      headers: body === undefined || isForm ? headers : { "Content-Type": "application/json", ...headers },
      body: body === undefined ? undefined : isForm ? (body as FormData) : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, "Keine Verbindung zum Server.");
  }
  const text = await response.text();
  const data: unknown = text.length > 0 ? safeParse(text) : undefined;
  if (!response.ok) {
    throw new ApiError(response.status, messageOf(data) ?? `Fehler ${response.status}`);
  }
  return data as T;
}

function safeParse(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return undefined;
  }
}

function messageOf(data: unknown): string | undefined {
  if (typeof data === "object" && data !== null && "message" in data) {
    const message = (data as { message: unknown }).message;
    return typeof message === "string" ? message : undefined;
  }
  return undefined;
}

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "Unbekannter Fehler.";
}
