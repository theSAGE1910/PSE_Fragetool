import type { ScoreboardEntry } from "../api/types";

/** Die bestplatzierten Teilnehmer (/MK320/, /MK335/). */
export function ScoreboardTable({ entries }: { entries: ScoreboardEntry[] }) {
  if (entries.length === 0) {
    return <p className="hint">Noch keine Teilnehmer.</p>;
  }
  return (
    <table className="scoreboard">
      <thead>
        <tr>
          <th>Rang</th>
          <th>Name</th>
          <th>Punkte</th>
        </tr>
      </thead>
      <tbody>
        {entries.map((entry) => (
          <tr key={entry.displayName}>
            <td>{entry.rank}.</td>
            <td className="mono">{entry.displayName}</td>
            <td>{entry.points}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
