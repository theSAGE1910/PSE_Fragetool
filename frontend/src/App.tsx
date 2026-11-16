import { Navigate, Route, Routes } from "react-router-dom";
import { RequireAuth } from "./components/RequireAuth";
import { DashboardPage } from "./pages/DashboardPage";
import { LoginPage } from "./pages/LoginPage";
import { ParticipantPage } from "./pages/ParticipantPage";
import { PresentationPage } from "./pages/PresentationPage";
import { QuestionEditorPage } from "./pages/QuestionEditorPage";
import { SessionControlPage } from "./pages/SessionControlPage";

/**
 * Drei Bereiche:
 *  - /dozent/...      Dozentenansicht (nur angemeldet)
 *  - /present/:code   Präsentationsansicht für den Beamer
 *  - /join/:code      Teilnehmeransicht (Ziel des QR-Codes)
 */
export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/dozent" element={<RequireAuth><DashboardPage /></RequireAuth>} />
      <Route path="/dozent/fragen/neu" element={<RequireAuth><QuestionEditorPage /></RequireAuth>} />
      <Route path="/dozent/fragen/:id" element={<RequireAuth><QuestionEditorPage /></RequireAuth>} />
      <Route path="/dozent/sessions/:id" element={<RequireAuth><SessionControlPage /></RequireAuth>} />
      <Route path="/present/:code" element={<PresentationPage />} />
      <Route path="/join/:code" element={<ParticipantPage />} />
      <Route path="*" element={<Navigate to="/dozent" replace />} />
    </Routes>
  );
}
