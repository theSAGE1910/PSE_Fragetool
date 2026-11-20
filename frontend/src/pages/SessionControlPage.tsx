import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { questions as questionApi, runs as runApi, sessions as sessionApi } from "../api/dozent";
import { errorMessage } from "../api/http";
import { QUESTION_TYPE_LABELS, type QuestionDto, type QuestionType, type SessionDto } from "../api/types";
import { Countdown } from "../components/Countdown";
import { ResultView } from "../components/ResultView";
import { ScoreboardTable } from "../components/ScoreboardTable";
import { useSessionState } from "../live/useSessionState";

/** Lädt die Session und zeigt dann die Steuerung. */
export function SessionControlPage() {
  const { id = "" } = useParams();
  const [session, setSession] = useState<SessionDto | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    sessionApi.get(id).then(setSession, (e) => setError(errorMessage(e)));
  }, [id]);

  if (error) {
    return <main className="page"><p className="error" role="alert">{error}</p><Link to="/dozent">Zurück</Link></main>;
  }
  return session ? <SessionControl session={session} /> : <p className="hint center">Lädt …</p>;
}

/**
 * Steuerung einer Session durch den Dozenten: Frage starten (/MK190/), Schnellstart (/MK200/),
 * vorzeitig beenden (/MK260/), Freitext freigeben (/MK295/), Scoreboard einblenden (/MK320/).
 */
function SessionControl({ session }: { session: SessionDto }) {
  const { state, offsetMs, connected, reload } = useSessionState(session.joinCode);
  const [catalog, setCatalog] = useState<QuestionDto[]>([]);
  const [selected, setSelected] = useState("");
  const [quickType, setQuickType] = useState<QuestionType>("SINGLE_CHOICE");
  const [quickOptions, setQuickOptions] = useState(4);
  const [quickSeconds, setQuickSeconds] = useState(20);
  const [freeTexts, setFreeTexts] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);

  const run = state?.run ?? null;
  const running = run?.status === "RUNNING";
  const presentationUrl = `/present/${session.joinCode}`;

  useEffect(() => {
    questionApi.list().then((list) => {
      setCatalog(list);
      setSelected((old) => old || (list[0]?.id ?? ""));
    }, (e) => setError(errorMessage(e)));
  }, []);

  // Freitextantworten sieht zuerst nur der Dozent (/MK290/).
  const runId = run?.id;
  const showFreeTexts = run?.question.type === "FREE_TEXT";
  const answerCount = run?.answerCount ?? 0;
  useEffect(() => {
    if (runId && showFreeTexts) {
      runApi.freeTextAnswers(runId).then(setFreeTexts, () => setFreeTexts([]));
    } else {
      setFreeTexts([]);
    }
  }, [runId, showFreeTexts, answerCount, run?.status]);

  const act = useCallback(async (action: () => Promise<unknown>) => {
    setError(null);
    try {
      await action();
    } catch (e) {
      setError(errorMessage(e));
    }
    // Nicht nur auf die WebSocket-Benachrichtigung warten: Die eigene Aktion soll sofort sichtbar sein.
    await reload();
  }, [reload]);

  return (
    <main className="page">
      <header className="topbar">
        <div>
          <h1>{session.name}</h1>
          <p className="hint">
            Code <span className="mono">{session.joinCode}</span> · {state?.participantCount ?? 0} Teilnehmer
            {!connected && <span className="badge warning"> Verbindung unterbrochen</span>}
          </p>
        </div>
        <div className="actions">
          <a className="button primary" href={presentationUrl} target="_blank" rel="noreferrer">Präsentationsansicht</a>
          <Link className="button" to="/dozent">Zurück</Link>
        </div>
      </header>
      {error && <p className="error" role="alert">{error}</p>}

      {running && run && (
        <section className="card highlight">
          <h2>Laufende Frage</h2>
          <p><strong>{run.question.text}</strong></p>
          <div className="inline-form">
            <Countdown endsAt={run.endsAt} offsetMs={offsetMs} />
            <span>{run.answerCount} {run.answerCount === 1 ? "Antwort" : "Antworten"}</span>
            <button type="button" className="danger" onClick={() => void act(() => runApi.close(run.id))}>Beenden</button>
          </div>
        </section>
      )}

      <section className="card">
        <h2>Frage aus dem Katalog starten</h2>
        {running && <p className="hint">Es läuft bereits eine Frage.</p>}
        <div className="inline-form">
          <select value={selected} onChange={(e) => setSelected(e.target.value)} aria-label="Frage auswählen" className="grow">
            {catalog.length === 0 && <option value="">Keine Fragen im Katalog</option>}
            {catalog.map((q) => (
              <option key={q.id} value={q.id}>
                {q.text} ({QUESTION_TYPE_LABELS[q.type]}, {q.timeLimitSeconds} s)
              </option>
            ))}
          </select>
          <button
            type="button"
            className="primary"
            disabled={running || !selected}
            onClick={() => void act(() => runApi.start(session.id, selected))}
          >
            Starten
          </button>
        </div>
      </section>

      <section className="card">
        <h2>Schnellstart</h2>
        <p className="hint">Die Frage stellst du mündlich oder auf deinen Folien. Sie wird nicht bewertet.</p>
        <div className="inline-form">
          <label>
            Fragetyp
            <select value={quickType} onChange={(e) => setQuickType(e.target.value as QuestionType)}>
              {(Object.keys(QUESTION_TYPE_LABELS) as QuestionType[]).map((t) => (
                <option key={t} value={t}>{QUESTION_TYPE_LABELS[t]}</option>
              ))}
            </select>
          </label>
          {(quickType === "SINGLE_CHOICE" || quickType === "MULTIPLE_CHOICE") && (
            <label>
              Antworten
              <input type="number" min={2} max={6} value={quickOptions} onChange={(e) => setQuickOptions(Number(e.target.value))} />
            </label>
          )}
          <label>
            Sekunden
            <input type="number" min={10} max={300} value={quickSeconds} onChange={(e) => setQuickSeconds(Number(e.target.value))} />
          </label>
          <button
            type="button"
            className="primary"
            disabled={running}
            onClick={() => void act(() => runApi.quickStart(session.id, quickType, quickOptions, quickSeconds))}
          >
            Schnellstart
          </button>
        </div>
      </section>

      {run && !running && (
        <section className="card">
          <h2>Letztes Ergebnis</h2>
          <p><strong>{run.question.text}</strong> · {run.answerCount} {run.answerCount === 1 ? "Antwort" : "Antworten"}</p>
          {run.question.type === "FREE_TEXT" ? (
            <>
              <ul className="free-text-list">
                {freeTexts.map((text, index) => (
                  <li key={index}>{text}</li>
                ))}
              </ul>
              <button type="button" onClick={() => void act(() => runApi.setFreeTextReleased(run.id, !run.freeTextReleased))}>
                {run.freeTextReleased ? "Antworten ausblenden" : "Antworten freigeben"}
              </button>
            </>
          ) : (
            <ResultView run={run} />
          )}
        </section>
      )}

      <section className="card">
        <h2>Scoreboard</h2>
        <button
          type="button"
          onClick={() => void act(() => sessionApi.setScoreboardVisible(session.id, !(state?.scoreboardVisible ?? false)))}
        >
          {state?.scoreboardVisible ? "Scoreboard ausblenden" : "Scoreboard anzeigen"}
        </button>
        {state?.scoreboardVisible && <ScoreboardTable entries={state.scoreboard} />}
      </section>
    </main>
  );
}
