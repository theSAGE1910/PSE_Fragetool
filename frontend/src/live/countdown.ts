/**
 * Hilfsfunktionen für die Anzeige der Restzeit.
 *
 * Der Server legt Start und Ende jeder Frage fest (/MK250/, /MK430/). Der Browser zeigt die
 * Restzeit nur an. Weil die Uhr des Geräts falsch gehen kann, wird mit dem Unterschied
 * zwischen Serveruhr und Geräteuhr gerechnet.
 */

/** Unterschied zwischen Serveruhr und Geräteuhr in Millisekunden. */
export function clockOffset(serverNow: string, deviceNowMs: number): number {
  return Date.parse(serverNow) - deviceNowMs;
}

/** Restzeit in ganzen Sekunden, aufgerundet und nie negativ. */
export function remainingSeconds(endsAt: string, offsetMs: number, deviceNowMs: number): number {
  const remainingMs = Date.parse(endsAt) - (deviceNowMs + offsetMs);
  return Math.max(0, Math.ceil(remainingMs / 1000));
}
