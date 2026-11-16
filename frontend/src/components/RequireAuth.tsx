import { type ReactNode, useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { auth } from "../api/dozent";

/** Zeigt den Inhalt nur angemeldeten Dozenten; sonst geht es zur Anmeldung. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<"loading" | "ok" | "anonymous">("loading");

  useEffect(() => {
    auth.me().then(
      () => setStatus("ok"),
      () => setStatus("anonymous"),
    );
  }, []);

  if (status === "loading") {
    return <p className="hint center">Lädt …</p>;
  }
  return status === "ok" ? <>{children}</> : <Navigate to="/login" replace />;
}
