import { type FormEvent, useCallback, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { ApiError, errorMessage } from "../api/http";
import { publicApi, tokenStore } from "../api/participant";
import type { AnswerRequest, Me, RunState } from "../api/types";
import { Countdown, useRemainingSeconds } from "../components/Countdown";
import { useSessionState } from "../live/useSessionState";

/**
 * Teilnehmeransicht auf dem Smartphone: beitreten ohne Registrierung (/MK160/), automatisch
 * vergebenen Namen sehen (/MK170/), antworten (/MK240/), Ergebnis, Punkte und Rang sehen.
 */
export function ParticipantPage() {
  const { code = "" } = useParams();
  const [token, setToken] = useState<string | null>(() => tokenStore.get(code));
  const [me, setMe] = useState<Me | null>(null);
  const [joinError, setJoinError] = useState<string | null>(null);
  /** Frage, deren Antwort der Server in diesem Browser bestätigt hat (/MK245/). */
  const [confirmedRunId, setConfirmedRunId] = useState<string | null>(null);

  // Beitritt: vorhandenes Token wiederverwenden (/MK180/), sonst neu beitreten.
  useEffect(() => {
    let cancelled = false;
    async function join() {
      try {
        const joined = await publicApi.join(code);
        if (!cancelled) {
          tokenStore.set(code, joined.token);
          setToken(joined.token);
        }
      } catch (e) {
        if (!cancelled) {
          setJoinError(errorMessage(e));
        }
      }
    }
    if (!token) {
      void join();
    }
    return () => {
      cancelled = true;
    };
  }, [code, token]);

  const loadMe = useCallback(async () => {
    if (!token) {
      return;
    }
    try {
      setMe(await publicApi.me(token));
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) {
        // Token unbekannt (z. B. Session neu angelegt): neu beitreten.
        tokenStore.clear(code);
        setToken(null);
      }
    }
  }, [code, token]);

  useEffect(() => {
    void loadMe();
  }, [loadMe]);

  const { state, offsetMs, connected, notFound } = useSessionState(code, loadMe);

  if (notFound) {
    return <main className="participant"><p className="hint center">Session nicht gefunden.</p></main>;
  }
  if (joinError) {
    return <main className="participant"><p className="error" role="alert">{joinError}</p></main>;
  }
  if (!state || !me || !token) {
    return <main className="participant"><p className="hint center">Lädt …</p></main>;
  }

  const run = state.run;
  const answered = run !== null && (confirmedRunId === run.id || (me.lastRunId === run.id && me.answered));

  return (
    <main className="participant">
      <header className="participant-header">
        <div>
          <div className="hint">{state.sessionName}</div>
          <div>Dein Name: <strong className="mono">{me.displayName}</strong></div>
        </div>
        <div className="participant-score">
          <strong>{me.points}</strong> {me.points === 1 ? "Punkt" : "Punkte"}
          <div className="hint">Rang {me.rank} von {me.participantCount}</div>
        </div>
      </header>
      {!connected && <p className="banner warning" role="status">Verbindung unterbrochen. Verbinde neu …</p>}

      {!run && <p className="waiting">Warte auf die nächste Frage.</p>}

      {run && run.status === "RUNNING" && (
        <section>
          <div className="question-head">
            <h1>{run.question.text}</h1>
            <Countdown endsAt={run.endsAt} offsetMs={offsetMs} />
          </div>
          {run.question.imageUrl && <img className="question-image" src={run.question.imageUrl} alt="Bild zur Frage" />}
          {answered ? (
            <p className="banner success" role="status">Antwort gespeichert.</p>
          ) : (
            <AnswerForm
              key={run.id}
              run={run}
              offsetMs={offsetMs}
              onSubmit={async (answer) => {
                await publicApi.answer(token, run.id, answer);
                setConfirmedRunId(run.id);
              }}
            />
          )}
        </section>
      )}

      {run && run.status === "CLOSED" && (
        <section>
          <h1>{run.question.text}</h1>
          {run.question.gradable ? (
            <>
              {!answered && <p className="banner neutral">Keine Antwort.</p>}
              {answered && me.lastRunId === run.id && me.correct === true && <p className="banner success">Richtig</p>}
              {answered && me.lastRunId === run.id && me.correct === false && <p className="banner wrong">Falsch</p>}
              {run.result?.solution && <p className="solution">Lösung: <strong>{run.result.solution}</strong></p>}
            </>
          ) : (
            <p className="banner neutral">{answered ? "Danke für deine Antwort." : "Keine Antwort."} Diese Frage wird nicht bewertet.</p>
          )}
          <p className="waiting">Warte auf die nächste Frage.</p>
        </section>
      )}
    </main>
  );
}

/** Eingabe der Antwort, passend zum Fragetyp. "Gespeichert" erscheint erst nach Bestätigung des Servers. */
function AnswerForm({ run, offsetMs, onSubmit }: {
  run: RunState;
  offsetMs: number;
  onSubmit: (answer: AnswerRequest) => Promise<void>;
}) {
  const [selected, setSelected] = useState<string[]>([]);
  const [value, setValue] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const timeUp = useRemainingSeconds(run.endsAt, offsetMs) === 0;
  const type = run.question.type;

  function toggle(optionId: string) {
    if (type === "SINGLE_CHOICE") {
      setSelected([optionId]);
    } else {
      setSelected(selected.includes(optionId) ? selected.filter((id) => id !== optionId) : [...selected, optionId]);
    }
  }

  function buildAnswer(): AnswerRequest | null {
    if (type === "SINGLE_CHOICE" || type === "MULTIPLE_CHOICE") {
      return selected.length > 0 ? { optionIds: selected } : null;
    }
    if (type === "NUMERIC") {
      const number = Number(value.replace(",", "."));
      return value.trim() !== "" && Number.isFinite(number) ? { numericValue: number } : null;
    }
    return value.trim() !== "" ? { text: value.trim() } : null;
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    const answer = buildAnswer();
    if (!answer) {
      setError("Bitte zuerst eine Antwort eingeben.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await onSubmit(answer);
    } catch (e) {
      setError(e instanceof ApiError && e.status === 0
        ? "Antwort nicht gespeichert. Bitte erneut versuchen."
        : errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} className="answer-form">
      {(type === "SINGLE_CHOICE" || type === "MULTIPLE_CHOICE") && (
        <div className="answer-options" role={type === "SINGLE_CHOICE" ? "radiogroup" : "group"}>
          {run.question.options.map((option) => (
            <button
              key={option.id}
              type="button"
              className={selected.includes(option.id) ? "answer-option selected" : "answer-option"}
              aria-pressed={selected.includes(option.id)}
              onClick={() => toggle(option.id)}
            >
              {option.text}
            </button>
          ))}
        </div>
      )}
      {type === "NUMERIC" && (
        <input value={value} onChange={(e) => setValue(e.target.value)} inputMode="decimal" placeholder="Zahl eingeben" aria-label="Antwort als Zahl" />
      )}
      {type === "FREE_TEXT" && (
        <textarea value={value} onChange={(e) => setValue(e.target.value)} rows={4} maxLength={500} placeholder="Antwort eingeben" aria-label="Antwort als Text" />
      )}
      {error && <p className="error" role="alert">{error}</p>}
      {timeUp && <p className="banner neutral">Zeit abgelaufen.</p>}
      <button type="submit" className="primary big" disabled={busy || timeUp}>Absenden</button>
    </form>
  );
}
