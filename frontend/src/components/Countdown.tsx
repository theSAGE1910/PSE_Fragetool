import { useEffect, useState } from "react";
import { remainingSeconds } from "../live/countdown";

const TICK_MS = 250;

/** Zeigt die verbleibende Antwortzeit in Sekunden (/MK230/). */
export function Countdown({ endsAt, offsetMs, large = false }: { endsAt: string; offsetMs: number; large?: boolean }) {
  const seconds = useRemainingSeconds(endsAt, offsetMs);
  return (
    <div className={large ? "countdown countdown-large" : "countdown"} role="timer" aria-live="off">
      <span className="countdown-value">{seconds}</span>
      <span className="countdown-unit">s</span>
    </div>
  );
}

export function useRemainingSeconds(endsAt: string, offsetMs: number): number {
  const [seconds, setSeconds] = useState(() => remainingSeconds(endsAt, offsetMs, Date.now()));
  useEffect(() => {
    const update = () => setSeconds(remainingSeconds(endsAt, offsetMs, Date.now()));
    update();
    const timer = window.setInterval(update, TICK_MS);
    return () => window.clearInterval(timer);
  }, [endsAt, offsetMs]);
  return seconds;
}
