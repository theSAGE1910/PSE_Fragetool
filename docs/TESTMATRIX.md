# Testmatrix: Musskriterien → automatisierte Tests

Ergänzt Abschnitt 8.7 des Pflichtenhefts. Für jedes Musskriterium steht hier, welcher automatisierte Test
es prüft. Die manuellen Testfallszenarien (/T…/) und Prüfverfahren (/P…/) stehen im Pflichtenheft.

**Wo die Tests liegen**

| Kürzel | Datei | Werkzeug | Ausführen |
|---|---|---|---|
| B: | `backend/src/test/java/edu/kit/pse/fragetool/…` | JUnit, Spring Boot Test, MockMvc, STOMP-Client | `cd backend && mvn verify` |
| F: | `frontend/src/…/*.test.ts(x)` | Vitest, Testing Library | `cd frontend && npm test` |
| E: | `frontend/e2e/*.spec.ts` | Playwright | `cd frontend && npm run e2e` (Anwendung muss laufen) |
| L: | `loadtest/k6-200-teilnehmer.js` | k6 | `k6 run loadtest/k6-200-teilnehmer.js` (Anwendung muss laufen) |

Methodennamen stehen hinter dem `#`. In den Testberichten erscheinen die Tests mit ihrem `@DisplayName`,
der immer die Kriteriennummer enthält.

| Kriterium | Inhalt (kurz) | Automatisierte Tests | Manuell |
|---|---|---|---|
| /MK10/ | Registrieren | B: `DozentKontoTest#registrieren`, `FlowIntegrationTest` · E: `dozent.spec.ts` | /T10/ |
| /MK20/ | Anmelden, Abmelden | B: `DozentKontoTest#anmeldenUndAbmelden` · E: `dozent.spec.ts` | /T20/ |
| /MK30/ | Passwort nur als Hash | B: `DozentKontoTest#passwortNurAlsHash` | /T10/ |
| /MK40/ | Nur eigene Daten | B: `DozentKontoTest#fremdeDatenSindGeschuetzt` | /T30/ |
| /MK50/ | Fragen erstellen, bearbeiten, löschen | B: `FragenkatalogTest#alleFragetypenAnlegen`, `#bearbeitenUndLoeschen` | /T40/ |
| /MK60/ | Vier Fragetypen | B: `FragenkatalogTest#alleFragetypenAnlegen`, `FragendurchfuehrungTest` | /T40/, /T60/ |
| /MK70/ | 2–6 Antworten, SC genau eine, MC mind. eine korrekt | B: `FragenkatalogTest#antwortmoeglichkeiten`, `QuestionEvaluationTest` | /T50/ |
| /MK80/ | Wert und Toleranz, Grenzen inklusive | B: `FragenkatalogTest#numerischeFragen`, `FragendurchfuehrungTest#numerischeFrage`, `QuestionEvaluationTest` | /T60/, /T140/ |
| /MK90/ | Freitext ohne Bewertung | B: `FragenkatalogTest#freitextOhneLoesung`, `FragendurchfuehrungTest#freitext` · F: `ParticipantPage.test.tsx` | /T150/ |
| /MK100/ | Zeitlimit 10–300 s, Standard 45 s | B: `FragenkatalogTest#zeitlimit`, `QuestionEvaluationTest` | /T50/ |
| /MK110/ | Mehrere Tags | B: `FragenkatalogTest#tagsUndFilter` | /T80/ |
| /MK120/ | Filter nach Tag | B: `FragenkatalogTest#tagsUndFilter` | /T80/ |
| /MK130/ | Sessions erstellen, benennen, löschen | B: `SessionUndTeilnahmeTest#sessionLebenszyklus` · E: `dozent.spec.ts` | /T90/, /T95/ |
| /MK140/ | Link und QR-Code, gültig solange die Session existiert | B: `SessionUndTeilnahmeTest#sessionLebenszyklus`, `WebSchnittstelleTest` · F: `PresentationPage.test.tsx` | /T90/, /T95/ |
| /MK150/ | Präsentationsansicht mit QR-Code im Vollbild | F: `PresentationPage.test.tsx` · E: `dozent.spec.ts` | /T90/ |
| /MK155/ | Bilder an Fragen | B: `FragenbildTest#grossesPngWirdVerkleinert`, `#bildErsetzenUndEntfernen` | /T45/, /T125/ |
| /MK156/ | Falsche Formate und zu große Dateien abgelehnt | B: `FragenbildTest#ungueltigeDateienWerdenAbgelehnt`, `ImageProcessorTest` | /T45/ |
| /MK160/ | Beitritt ohne Registrierung | B: `SessionUndTeilnahmeTest#beitrittMitAutomatischemNamen` · F: `ParticipantPage.test.tsx` · E: `teilnehmer.spec.ts` | /T100/ |
| /MK170/ | Automatischer Anzeigename | B: `SessionUndTeilnahmeTest`, `ParticipantServiceTest` · F: `ParticipantPage.test.tsx` | /T100/ |
| /MK171/ | Zeichen ohne Vokale und Verwechsler | B: `DisplayNameTest`, `SessionUndTeilnahmeTest` | /T100/ |
| /MK172/ | Mindestens 3 Zeichen, Verlängerung | B: `DisplayNameTest#laengeWaechstAnDenGrenzen`, `SessionUndTeilnahmeTest` | /T100/ |
| /MK173/ | Eindeutig, unveränderlich | B: `SessionUndTeilnahmeTest#gleichzeitigeBeitritte`, `ParticipantServiceTest` | /T110/ |
| /MK180/ | Neuladen behält Name und Punkte | B: `SessionUndTeilnahmeTest#neuladenBehaeltNameUndPunkte` · F: `ParticipantPage.test.tsx` · E: `teilnehmer.spec.ts` | /T110/ |
| /MK190/ | Katalogfrage starten | B: `FragendurchfuehrungTest#katalogfrageStarten` · E: `dozent.spec.ts` | /T120/ |
| /MK200/ | Schnellstart | B: `FragendurchfuehrungTest#schnellstart` · F: `ParticipantPage.test.tsx` | /T160/ |
| /MK210/ | Höchstens eine laufende Frage je Session | B: `FragendurchfuehrungTest#eineFrageJeSession` | /T185/ |
| /MK220/ | Frage erscheint ohne Neuladen | B: `LiveBenachrichtigungTest` · F: `useSessionState.test.ts` · E: `teilnehmer.spec.ts` | /T115/, /T120/ |
| /MK230/ | Restzeit in beiden Ansichten | B: `FragendurchfuehrungTest#katalogfrageStarten` · F: `Countdown.test.tsx`, `PresentationPage.test.tsx` | /T120/ |
| /MK240/ | Antwort abgeben, nur einmal | B: `FragendurchfuehrungTest#antwortenAbgeben` · F: `ParticipantPage.test.tsx` | /T120/ |
| /MK245/ | Gespeichert erst nach Bestätigung | B: `FragendurchfuehrungTest#antwortenAbgeben` · F: `ParticipantPage.test.tsx` | /T120/ |
| /MK250/ | Ablehnung nach Zeitablauf (Serverzeit) | B: `FragendurchfuehrungTest#zeitablauf`, `RunSchedulerTest`, `QuestionRunTest` · F: `ParticipantPage.test.tsx` | /T170/ |
| /MK255/ | Ablehnung nach vorzeitigem Ende | B: `FragendurchfuehrungTest#vorzeitigBeenden`, `QuestionRunTest` | /T175/ |
| /MK260/ | Vorzeitig beenden | B: `FragendurchfuehrungTest#vorzeitigBeenden`, `RunSchedulerTest#abbrechen` · E: `dozent.spec.ts` | /T180/ |
| /MK270/ | Lösung nach Ende | B: `FragendurchfuehrungTest#loesungErstNachEnde` · F: `PresentationPage.test.tsx`, `ParticipantPage.test.tsx` | /T120/ |
| /MK280/ | Teilnehmer sieht richtig/falsch | B: `FragendurchfuehrungTest#loesungErstNachEnde` · F: `ParticipantPage.test.tsx` | /T120/ |
| /MK290/ | Antwortverteilung | B: `FragendurchfuehrungTest#multipleChoice`, `#numerischeFrage`, `#freitext` · F: `PresentationPage.test.tsx` | /T130/–/T150/ |
| /MK295/ | Freitextliste freigeben/ausblenden | B: `FragendurchfuehrungTest#freitext` · F: `PresentationPage.test.tsx` | /T150/ |
| /MK300/ | Anzahl der Antworten | B: `FragendurchfuehrungTest#antwortenAbgeben` · F: `useSessionState.test.ts`, `PresentationPage.test.tsx` | /T120/ |
| /MK310/ | 1 Punkt je richtiger Antwort | B: `FragendurchfuehrungTest#punkteUeberMehrereFragen`, `#multipleChoice`, `QuestionRunTest` | /T120/, /T130/ |
| /MK320/ | Scoreboard Top 10 | B: `ScoreboardAnzeigeTest`, `ScoreboardTest` · F: `PresentationPage.test.tsx` | /T190/ |
| /MK330/ | Eigener Punktestand und Rang | B: `ScoreboardAnzeigeTest` · F: `ParticipantPage.test.tsx` | /T190/ |
| /MK335/ | Gleicher Rang, alphabetisch, max. 10 | B: `ScoreboardAnzeigeTest`, `ScoreboardTest` · F: `PresentationPage.test.tsx` | /T190/ |
| /MK340/ | Teilnehmeransicht ab 360 px, Chrome/Safari | E: `teilnehmer.spec.ts` (Projekte `teilnehmer-360px`, `teilnehmer-iphone`) | /T125/, /P80/ |
| /MK350/ | Dozentenansicht in Chrome und Firefox | E: `dozent.spec.ts` (Projekte `dozent-chrome`, `dozent-firefox`) | /P80/ |
| /MK360/ | Nur nötige Teilnehmerdaten | B: `DatensparsamkeitTest` | /P90/ |
| /MK370/ | Oberfläche auf Deutsch | F: `sprache.test.ts` · B: `WebSchnittstelleTest#fehlerMitDeutscherMeldung` | /P80/ |
| /MK380/ | Start mit README und Docker Compose | – (Dockerfile und Compose-Datei; CI baut nur ohne Docker) | /T200/ |
| /MK390/ | 200 gleichzeitige WebSocket-Verbindungen | L: `k6-200-teilnehmer.js` | /P60/ |
| /MK400/ | Änderung innerhalb von 1 s übertragen | B: `LiveBenachrichtigungTest#benachrichtigungen` · E: `messung.spec.ts` (/P50/) | /P50/ |
| /MK410/ | Abbruch erkennen, Wiederverbindung ≤ 5 s | B: `LiveBenachrichtigungTest#heartbeat` · F: `useSessionState.test.ts` · E: `teilnehmer.spec.ts`, `messung.spec.ts` (/P40/) | /T115/, /P40/ |
| /MK430/ | Zeiten und Punkte nur auf dem Server | B: `FragendurchfuehrungTest#clientGibtKeineZeitenUndPunkteVor`, `#katalogfrageStarten` · F: `useSessionState.test.ts`, `Countdown.test.tsx` | /P70/ |
| /MK440/ | PNG/JPEG bis 5 MB | B: `FragenbildTest#jpegWirdAngenommen`, `#ungueltigeDateienWerdenAbgelehnt`, `ImageProcessorTest` | /T45/ |
| /MK441/ | Verkleinern auf 1200 px, Seitenverhältnis bleibt | B: `FragenbildTest#grossesPngWirdVerkleinert`, `ImageProcessorTest` | /T45/ |

**Nicht automatisiert:** /MK380/ (Docker-Start) bleibt eine manuelle Prüfung nach /T200/, weil der
CI-Runner kein Docker-in-Docker hat. Echte Geräte für /MK340/ ersetzt Playwright nur teilweise
(WebKit statt echtem iOS-Safari); die Sichtprüfung /P80/ auf einem iPhone und einem Android-Smartphone
bleibt nötig.
