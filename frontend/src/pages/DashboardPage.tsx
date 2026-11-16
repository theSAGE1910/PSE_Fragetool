import { type FormEvent, useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { auth, questions as questionApi, sessions as sessionApi } from "../api/dozent";
import { errorMessage } from "../api/http";
import { QUESTION_TYPE_LABELS, type QuestionDto, type SessionDto } from "../api/types";

/** Übersicht des Dozenten: Sessions (/MK130/) und Fragenkatalog (/MK50/, /MK120/). */
export function DashboardPage() {
  const navigate = useNavigate();
  const [sessions, setSessions] = useState<SessionDto[]>([]);
  const [questions, setQuestions] = useState<QuestionDto[]>([]);
  const [allTags, setAllTags] = useState<string[]>([]);
  const [tag, setTag] = useState("");
  const [sessionName, setSessionName] = useState("");
  const [error, setError] = useState<string | null>(null);

  const loadSessions = useCallback(() => sessionApi.list().then(setSessions), []);
  const loadQuestions = useCallback(async (filter: string) => {
    setQuestions(await questionApi.list(filter || undefined));
    if (!filter) {
      return;
    }
  }, []);
  const loadTags = useCallback(async () => {
    const all = await questionApi.list();
    setAllTags([...new Set(all.flatMap((q) => q.tags))].sort());
  }, []);

  useEffect(() => {
    Promise.all([loadSessions(), loadTags()]).catch((e) => setError(errorMessage(e)));
  }, [loadSessions, loadTags]);

  useEffect(() => {
    loadQuestions(tag).catch((e) => setError(errorMessage(e)));
  }, [tag, loadQuestions]);

  async function run(action: () => Promise<unknown>) {
    setError(null);
    try {
      await action();
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  function createSession(event: FormEvent) {
    event.preventDefault();
    void run(async () => {
      await sessionApi.create(sessionName);
      setSessionName("");
      await loadSessions();
    });
  }

  function deleteSession(session: SessionDto) {
    if (window.confirm(`Session „${session.name}“ mit allen Teilnehmern und Antworten löschen?`)) {
      void run(async () => {
        await sessionApi.remove(session.id);
        await loadSessions();
      });
    }
  }

  function deleteQuestion(question: QuestionDto) {
    if (window.confirm("Frage löschen?")) {
      void run(async () => {
        await questionApi.remove(question.id);
        await Promise.all([loadQuestions(tag), loadTags()]);
      });
    }
  }

  function logout() {
    void run(async () => {
      await auth.logout();
      navigate("/login");
    });
  }

  return (
    <main className="page">
      <header className="topbar">
        <h1>Fragetool</h1>
        <button type="button" onClick={logout}>Abmelden</button>
      </header>
      {error && <p className="error" role="alert">{error}</p>}

      <section className="card">
        <h2>Sessions</h2>
        <form onSubmit={createSession} className="inline-form">
          <input
            value={sessionName}
            onChange={(e) => setSessionName(e.target.value)}
            placeholder="Name, z. B. Rechnernetze VL 05"
            aria-label="Name der neuen Session"
            required
            maxLength={200}
          />
          <button type="submit" className="primary">Neue Session</button>
        </form>
        {sessions.length === 0 && <p className="hint">Noch keine Session.</p>}
        <ul className="list">
          {sessions.map((session) => (
            <li key={session.id}>
              <div>
                <Link to={`/dozent/sessions/${session.id}`}><strong>{session.name}</strong></Link>
                <div className="hint">Code <span className="mono">{session.joinCode}</span></div>
              </div>
              <div className="actions">
                <Link className="button" to={`/dozent/sessions/${session.id}`}>Öffnen</Link>
                <button type="button" className="danger" onClick={() => deleteSession(session)}>Löschen</button>
              </div>
            </li>
          ))}
        </ul>
      </section>

      <section className="card">
        <h2>Fragenkatalog</h2>
        <div className="inline-form">
          <label className="grow">
            Tag-Filter
            <select value={tag} onChange={(e) => setTag(e.target.value)}>
              <option value="">Alle Fragen</option>
              {allTags.map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
          </label>
          <Link className="button primary" to="/dozent/fragen/neu">Frage erstellen</Link>
        </div>
        {questions.length === 0 && <p className="hint">Keine Fragen.</p>}
        <ul className="list">
          {questions.map((question) => (
            <li key={question.id}>
              <div>
                <strong>{question.text}</strong>
                <div className="hint">
                  {QUESTION_TYPE_LABELS[question.type]} · {question.timeLimitSeconds} s
                  {question.tags.map((t) => (
                    <span key={t} className="tag">{t}</span>
                  ))}
                </div>
              </div>
              <div className="actions">
                <Link className="button" to={`/dozent/fragen/${question.id}`}>Bearbeiten</Link>
                <button type="button" className="danger" onClick={() => deleteQuestion(question)}>Löschen</button>
              </div>
            </li>
          ))}
        </ul>
      </section>
    </main>
  );
}
