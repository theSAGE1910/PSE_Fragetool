import { QRCodeSVG } from "qrcode.react";
import { useParams } from "react-router-dom";
import { QUESTION_TYPE_LABELS } from "../api/types";
import { Countdown } from "../components/Countdown";
import { ResultView } from "../components/ResultView";
import { ScoreboardTable } from "../components/ScoreboardTable";
import { useSessionState } from "../live/useSessionState";

/**
 * Präsentationsansicht für den Beamer (/MK150/). Sie braucht keine Anmeldung und zeigt nur,
 * was alle sehen dürfen: QR-Code, laufende Frage mit Restzeit, Auswertung, Scoreboard.
 */
export function PresentationPage() {
  const { code = "" } = useParams();
  const { state, offsetMs, connected, notFound } = useSessionState(code);
  const joinUrl = `${window.location.origin}/join/${code}`;

  if (notFound) {
    return <main className="presentation"><p className="hint center">Session nicht gefunden.</p></main>;
  }
  if (!state) {
    return <main className="presentation"><p className="hint center">Lädt …</p></main>;
  }
  const run = state.run;

  return (
    <main className="presentation">
      <header className="presentation-header">
        <div>
          <strong>{state.sessionName}</strong>
          <span className="hint"> · {state.participantCount} Teilnehmer</span>
        </div>
        <div className="presentation-join">
          {!connected && <span className="badge warning">Verbindung unterbrochen</span>}
          <span className="mono">{joinUrl}</span>
          <button type="button" onClick={() => void document.documentElement.requestFullscreen()}>Vollbild</button>
        </div>
      </header>

      {state.scoreboardVisible ? (
        <section className="presentation-body">
          <h1>Scoreboard</h1>
          <ScoreboardTable entries={state.scoreboard} />
        </section>
      ) : !run ? (
        <section className="presentation-body center">
          <h1>Jetzt beitreten</h1>
          <QRCodeSVG value={joinUrl} size={360} marginSize={2} />
          <p className="join-url mono">{joinUrl}</p>
        </section>
      ) : (
        <section className="presentation-body">
          <p className="hint">{QUESTION_TYPE_LABELS[run.question.type]}{run.question.quickStart ? " · nicht bewertet" : ""}</p>
          <h1>{run.question.text}</h1>
          {run.question.imageUrl && <img className="question-image" src={run.question.imageUrl} alt="Bild zur Frage" />}
          {run.status === "RUNNING" ? (
            <>
              <ol className="option-list" type="A">
                {run.question.options.map((option) => (
                  <li key={option.id}>{option.text}</li>
                ))}
              </ol>
              <div className="presentation-status">
                <Countdown endsAt={run.endsAt} offsetMs={offsetMs} large />
                <div className="answer-count">
                  {run.answerCount} {run.answerCount === 1 ? "Antwort" : "Antworten"}
                </div>
              </div>
            </>
          ) : (
            <>
              <ResultView run={run} />
              <p className="answer-count">
                {run.answerCount} {run.answerCount === 1 ? "Antwort" : "Antworten"}
              </p>
            </>
          )}
          <aside className="presentation-qr" aria-label="QR-Code zum Beitreten">
            <QRCodeSVG value={joinUrl} size={120} marginSize={1} />
          </aside>
        </section>
      )}
    </main>
  );
}
