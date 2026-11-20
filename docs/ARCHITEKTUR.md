# Architektur und Zuordnung zum Pflichtenheft

## Überblick

```
Browser (React)                         Server (Spring Boot)                      PostgreSQL
───────────────                         ────────────────────                      ──────────
Dozentenansicht      ── REST ──►  Controller ─► Service ─► Repository  ── JDBC ──►  Tabellen
Präsentationsansicht ◄─ WebSocket ─  LiveSessionPublisher ◄─ Ereignisse der Services
Teilnehmeransicht    ── REST ──►  PublicController (ohne Anmeldung, Token im Header)
```

Zwei Regeln prägen den Entwurf:

1. **Aktionen laufen über REST, Benachrichtigungen über WebSocket.** Der Server sendet über
   `/topic/session/{joinCode}` nur „Zustand hat sich geändert“ (`STATE_CHANGED`) oder einen neuen
   Zählerstand. Die Ansicht lädt den Zustand danach per `GET /api/public/sessions/{code}/state`.
   Nach einem Verbindungsabbruch passiert genau dasselbe, deshalb geht nichts verloren (/MK410/).
2. **Die Regeln stehen in den Domänenklassen, nicht in den Services.** `Question` weiß, wann eine
   Antwort korrekt ist; `QuestionRun` weiß, ob noch Antworten angenommen werden. Diese Klassen brauchen
   weder Spring noch eine Datenbank und sind deshalb mit reinen Unit-Tests prüfbar.

## Pakete im Backend (`backend/src/main/java/edu/kit/pse/fragetool/`)

| Paket | Inhalt | hängt ab von |
|---|---|---|
| `common` | Ausnahmen (→ HTTP-Status) und Ereignisse | – |
| `auth` | `Dozent`, Registrierung, Login | `common` |
| `question` | `Question` mit vier Unterklassen, `AnswerOption`, Bilder, Fragenkatalog | `auth`, `common` |
| `session` | `Session`, `Participant`, Anzeigenamen, Beitritt | `auth`, `common` |
| `run` | `QuestionRun`, `Answer`, Punktevergabe, Zeitgeber | `question`, `session`, `auth`, `common` |
| `scoreboard` | `Scoreboard` (Rangfolge) | `session` |
| `live` | WebSocket, öffentlicher Zustand, Schnittstelle für Teilnehmer | alle fachlichen Pakete |
| `config` | Spring-Konfiguration, Security, Fehlerbehandlung | – |

Es gibt keine zyklischen Abhängigkeiten zwischen den Paketen.

## Ordner im Frontend (`frontend/src/`)

| Ordner | Inhalt |
|---|---|
| `api/` | REST-Aufrufe (`dozent.ts`, `participant.ts`), Datentypen (`types.ts`), Fehlerbehandlung (`http.ts`) |
| `live/` | `useSessionState` (WebSocket + Zustand), `countdown.ts` (Restzeit aus Server-Zeitstempeln) |
| `components/` | `Countdown`, `ResultView`, `ScoreboardTable`, `RequireAuth` |
| `pages/` | `LoginPage`, `DashboardPage`, `QuestionEditorPage`, `SessionControlPage` (Dozent), `PresentationPage` (Beamer), `ParticipantPage` (Smartphone) |

## Musskriterien → Code → Test

Die Testspalte zeigt den ersten Stand. Die vollständige, aktuelle Zuordnung aller Musskriterien zu
automatisierten Tests steht in `docs/TESTMATRIX.md`.

„Unit“ = Unit-Test im Backend, „Flow“ = `FlowIntegrationTest`, „Vitest“ = Frontend-Test.

| Kriterium | Umsetzung | Test |
|---|---|---|
| /MK10/ Registrieren | `DozentService.register`, `AuthController` | Flow |
| /MK20/ An-/Abmelden | `AuthController.login`, `SecurityConfig` (Logout), `LoginPage` | Flow |
| /MK30/ Passwort-Hash | `SecurityConfig.passwordEncoder` (bcrypt) | – (per DB-Abfrage prüfen) |
| /MK40/ Nur eigene Daten | `QuestionService.requireOwned`, `SessionService.requireOwned`, `RunService.requireOwned` | Flow |
| /MK50/ Fragen verwalten | `QuestionService`, `QuestionController`, `QuestionEditorPage` | Flow |
| /MK60/ Vier Fragetypen | `SingleChoiceQuestion`, `MultipleChoiceQuestion`, `NumericQuestion`, `FreeTextQuestion`, `QuestionFactory` | Unit |
| /MK70/ 2–6 Antworten | `ChoiceQuestion.validateDetails`, `validateCorrectCount` | Unit, Flow |
| /MK80/ Toleranz | `NumericQuestion.evaluate`, `validateDetails` | Unit |
| /MK90/ Freitext unbewertet | `FreeTextQuestion.hasSolution` | Unit |
| /MK100/ Zeitlimit 10–300 s | `Question.validate`, Konstanten in `Question` | Unit |
| /MK110/ Tags | `Question.tags`, `Question.update` | Flow |
| /MK120/ Tag-Filter | `QuestionRepository.findCatalogByTag`, `DashboardPage` | Flow |
| /MK130/ Sessions verwalten | `SessionService`, `SessionController` | Flow |
| /MK140/ Link und QR-Code | `Session.joinCode`, `PresentationPage` (`qrcode.react`) | Flow |
| /MK150/ Präsentationsansicht | `PresentationPage` | manuell |
| /MK155/ Bild anhängen | `ImageService`, `ImageController`, `QuestionImage`, `QuestionEditorPage` | manuell |
| /MK156/, /MK440/ Format und Größe | `ImageProcessor.process` | Unit |
| /MK441/ Verkleinern auf 1200 px | `ImageProcessor.process` | Unit |
| /MK160/ Beitritt ohne Registrierung | `ParticipantService.join`, `PublicController.join`, `ParticipantPage` | Flow |
| /MK170/ Automatischer Name | `DisplayNameGenerator`, `ConsonantCodeGenerator` | Unit, Flow |
| /MK171/ Zeichenvorrat | `ConsonantCodeGenerator.ALPHABET` | Unit |
| /MK172/ Länge | `DisplayNameLengthPolicy.lengthFor` | Unit |
| /MK173/ Eindeutig | `ParticipantService.join` + UNIQUE-Regel `uq_participant_name` | Flow |
| /MK180/ Neuladen | `tokenStore` (localStorage), `PublicController.me` | manuell |
| /MK190/ Frage starten | `RunService.start` | Flow |
| /MK200/ Schnellstart | `RunService.quickStart`, `Question.quickStart` | Unit |
| /MK210/ Eine Frage gleichzeitig | `RunService.startRun` | Flow |
| /MK220/ Ohne Neuladen | `LiveSessionPublisher`, `useSessionState` | manuell |
| /MK230/ Restzeit | `Countdown`, `countdown.ts` | Vitest |
| /MK240/ Eine Antwort | `AnswerService.submit` + UNIQUE-Regel `uq_answer_once` | Flow |
| /MK245/ Bestätigung | `ParticipantPage` (`confirmedRunId` erst nach HTTP 201) | manuell |
| /MK250/ Zeitablauf | `QuestionRun.ensureAccepting`, `RunScheduler` | Unit |
| /MK255/ Nach vorzeitigem Ende | `QuestionRun.ensureAccepting` | Unit, Flow |
| /MK260/ Vorzeitig beenden | `RunService.close`, `RunCloser` | Flow |
| /MK270/ Lösung anzeigen | `Question.solutionText`, `SessionStateService.result`, `ResultView` | Unit, Flow |
| /MK280/ Eigenes Ergebnis | `SessionStateService.me`, `ParticipantPage` | Flow |
| /MK290/ Antwortverteilung | `Question.distribution` (je Fragetyp), `ResultView` | Unit, Flow |
| /MK295/ Freitext freigeben | `RunService.setFreeTextReleased`, `SessionStateService.result` | manuell |
| /MK300/ Anzahl Antworten | `AnswerCountChangedEvent`, `RunDto.answerCount` | Flow |
| /MK310/ Punkte | `QuestionRun.closeAndEvaluate`, `StandardScoring` | Unit, Flow |
| /MK320/ Scoreboard Top 10 | `Scoreboard.top`, `SessionService.setScoreboardVisible`, `ScoreboardTable` | Unit |
| /MK330/ Eigener Rang | `SessionStateService.me` | Flow |
| /MK335/ Gleichstand | `Scoreboard.of` | Unit |
| /MK340/, /MK350/ Browser, 360 px | `styles.css` (Teilnehmeransicht) | manuell auf Geräten |
| /MK360/ Datensparsamkeit | Tabelle `participant` in `V1__init.sql` | – (Schema prüfen) |
| /MK370/ Deutsch | alle Texte im Frontend, Meldungen der Ausnahmen | manuell |
| /MK380/ Docker Compose | `Dockerfile`, `docker-compose.yml`, `README.md` | manuell |
| /MK390/ 200 Verbindungen | `WebSocketConfig` (einfacher Broker) | Lasttest fehlt |
| /MK400/ 1 Sekunde | `LiveSessionPublisher` (sendet direkt nach dem Commit) | Lasttest fehlt |
| /MK410/ Wiederverbindung | `useSessionState` (`reconnectDelay` 2 s, Heartbeat 10 s, danach Zustand laden) | manuell |
| /MK430/ Server bestimmt Zeit und Punkte | `Clock`-Bean in `AppConfig`, `QuestionRun`, `countdown.ts` (nur Anzeige) | Unit, Vitest |

## Wunschkriterien → vorgesehene Stelle

Nichts davon ist umgesetzt. Die Tabelle zeigt, wo die Erweiterung ansetzt und was dafür schon vorhanden ist.

| Kriterium | Ansatzpunkt | Aufwand |
|---|---|---|
| /WK10/ Volltextsuche | neue Abfrage in `QuestionRepository`, Parameter `q` in `QuestionController.list`, Suchfeld in `DashboardPage` | klein |
| /WK20/ Live-Diagramm | `AnswerCountChangedEvent` um die Verteilung erweitern oder bei `ANSWER_COUNT` den Zustand laden; `ResultView` wiederverwenden | klein |
| /WK30/ Duplizieren | Methode `copy()` in `Question` (je Unterklasse), `POST /api/questions/{id}/copy` | klein |
| /WK50/ CSV-Export | neues Paket `export` mit `ExportFormat`-Schnittstelle und `CsvExport`; liest `AnswerRepository` | mittel |
| /WK70/ Scoreboard zurücksetzen | `Participant.resetPoints()`, Methode in `SessionService`, Knopf in `SessionControlPage` | klein |
| /WK40/ JSON-Export/-Import | `ExportFormat` aus /WK50/ mit `JsonExport`; Import über `QuestionService.create` | mittel |
| /WK60/ Zufällige Reihenfolge | in `ParticipantPage` mischen (Bewertung arbeitet mit IDs, nicht mit Positionen) | klein |
| /WK80/ Formeln, Quellcode | Komponente `QuestionText` im Frontend (KaTeX), ersetzt die direkte Textausgabe | mittel |
| /WK100/ Zeitbonus | neue Klasse `TimeBonusScoring implements ScoringStrategy`, Bean in `AppConfig` tauschen | klein |
| /WK110/ Quiz-Modus | neue Entity `QuestionSequence`, `RunCloser` startet die nächste Frage | groß |
| /WK120/ Wortwolke | neue Komponente statt der Liste in `ResultView` | mittel |
| /WK130/ Englisch | Texte in eine Sprachdatei auslagern (`react-i18next`) | mittel |
| /WK140/ Dark Mode | CSS-Variablen in `styles.css` sind vorbereitet; zweiter Variablensatz | klein |
| /WK150/ Um 15 s verlängern | Methode `extend()` in `QuestionRun`, `RunScheduler.schedule` erneut aufrufen | klein |
| /WK200/ MP4/GIF | `ImageProcessor.detectFormat` erweitern, Größenlimit und Auslieferung überdenken | mittel |
| /WK210/ Bereinigung nach 24 h | geplanter Job (`@Scheduled`) im Paket `session` | klein |
| /WK160/, /WK170/ Mehrere Dozenten, Freigabe | `owner` wird zu einer Menge von Berechtigten; betrifft alle `requireOwned`-Methoden | groß |
| /WK190/ Statistik | neue Abfragen über `Answer` und `QuestionRun` | mittel |

## Entscheidungen, die vom Klassendiagramm vom 13.11. abweichen

Das Diagramm sollte vor der Abgabe des Designdokuments an den Code angepasst werden.

- **Tags** sind eine Menge von Zeichenketten an der Frage (`question_tag`), keine eigene Entity `Tag`.
- **Anzeigenamen** sind Zeichenketten; es gibt kein Wertobjekt `DisplayName`.
- **`Session` und `QuestionRun` halten keine Listen** ihrer Teilnehmer, Durchläufe oder Antworten. Die Services
  laden diese über die Repositories. `Session.startRun` aus dem Diagramm ist deshalb `RunService.startRun`.
- **`RunCloser`** ist neu. Die Klasse beendet eine Frage und wird vom Zeitgeber und vom Dozenten genutzt.
- **`AnswerContent`** ist neu. Dadurch hängt das Paket `question` nicht vom Paket `run` ab.
- **WebSocket-Nachrichten** tragen keine Fachdaten, sondern lösen das Nachladen des Zustands aus.
- **Bilder** hängen über `Question.imageId` an der Frage; `QuestionImage` hält die Bilddaten.
