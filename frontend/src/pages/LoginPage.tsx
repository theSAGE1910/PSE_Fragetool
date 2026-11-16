import { type FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { auth } from "../api/dozent";
import { errorMessage } from "../api/http";

/** Anmeldung und Registrierung für Dozenten (/MK10/, /MK20/). */
export function LoginPage() {
  const navigate = useNavigate();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (mode === "register") {
        await auth.register(email, password);
      }
      await auth.login(email, password);
      navigate("/dozent");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="page narrow">
      <h1>Fragetool</h1>
      <h2>{mode === "login" ? "Anmelden" : "Konto erstellen"}</h2>
      <form onSubmit={submit} className="card form">
        <label>
          E-Mail-Adresse
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
        </label>
        <label>
          Passwort
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            autoComplete={mode === "login" ? "current-password" : "new-password"}
          />
        </label>
        {error && <p className="error" role="alert">{error}</p>}
        <button type="submit" className="primary" disabled={busy}>
          {mode === "login" ? "Anmelden" : "Konto erstellen"}
        </button>
      </form>
      <button type="button" className="link" onClick={() => setMode(mode === "login" ? "register" : "login")}>
        {mode === "login" ? "Noch kein Konto? Registrieren" : "Schon registriert? Anmelden"}
      </button>
    </main>
  );
}
