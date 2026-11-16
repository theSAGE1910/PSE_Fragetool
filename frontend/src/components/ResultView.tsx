import type { RunState } from "../api/types";

/**
 * Auswertung nach Fragenende (/MK270/, /MK290/):
 * Balken für Single/Multiple Choice, Werte mit Häufigkeit für numerische Fragen, Liste für Freitext.
 */
export function ResultView({ run }: { run: RunState }) {
  const result = run.result;
  if (!result) {
    return null;
  }
  const isFreeText = run.question.type === "FREE_TEXT";
  if (isFreeText) {
    return result.distribution.length === 0 ? (
      <p className="hint">Die Freitextantworten sind noch nicht freigegeben.</p>
    ) : (
      <ul className="free-text-list">
        {result.distribution.map((entry, index) => (
          <li key={index}>{entry.label}</li>
        ))}
      </ul>
    );
  }
  const max = Math.max(1, ...result.distribution.map((entry) => entry.count));
  return (
    <div className="result">
      {result.distribution.length === 0 && <p className="hint">Keine Antworten.</p>}
      {result.distribution.map((entry, index) => (
        <div key={index} className={entry.correct ? "bar-row bar-correct" : "bar-row"}>
          <div className="bar-label">
            {entry.correct && <span aria-label="korrekt">✓ </span>}
            {entry.label}
          </div>
          <div className="bar-track">
            <div className="bar-fill" style={{ width: `${(entry.count / max) * 100}%` }} />
          </div>
          <div className="bar-count">{entry.count}</div>
        </div>
      ))}
      {result.solution && (
        <p className="solution">
          Lösung: <strong>{result.solution}</strong>
        </p>
      )}
    </div>
  );
}
