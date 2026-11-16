# Interaktives Fragetool für Vorlesungen

PSE WS 2026/27, Institut für Telematik. Dozenten stellen Fragen, Studierende antworten per Smartphone
über einen QR-Code, die Ergebnisse erscheinen auf dem Beamer.

| Teil | Technik | Ordner |
|---|---|---|
| Frontend | React 19, Vite 8, TypeScript 6 | `frontend/` |
| Backend | Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Security, WebSocket/STOMP) | `backend/` |
| Datenbank | PostgreSQL 16, Schema über Flyway | `backend/src/main/resources/db/migration/` |

Welche Klasse welches Kriterium des Pflichtenhefts umsetzt, steht in [`docs/ARCHITEKTUR.md`](docs/ARCHITEKTUR.md).
Bildschirmfotos der Oberfläche liegen in `docs/vorschau/`.

## Schnellstart mit Docker (/MK380/)

Voraussetzung: Docker mit Docker Compose.

```bash
docker compose up --build
```

Danach im Browser `http://localhost:8080` öffnen, ein Konto erstellen, eine Frage und eine Session anlegen.
Damit Smartphones im selben WLAN beitreten können, die Präsentationsansicht über die IP-Adresse des
Rechners öffnen (z. B. `http://192.168.0.20:8080`), denn der QR-Code enthält die Adresse aus der Adresszeile.

## Entwicklung

Voraussetzungen: JDK 21, Maven 3.9, Node.js 22.12 oder neuer (empfohlen: 24), Docker für die Datenbank.

```bash
docker compose up db            # 1. Datenbank starten
cd backend && mvn spring-boot:run    # 2. Backend auf Port 8080
cd frontend && npm install && npm run dev   # 3. Frontend auf Port 5173
```

Im Entwicklungsmodus `http://localhost:5173` öffnen. Vite leitet `/api` und `/ws` an das Backend weiter
und lädt geänderte Dateien sofort neu.

## Tests

```bash
cd backend && mvn verify      # Unit-Tests + Integrationstest (H2 im Arbeitsspeicher), JaCoCo-Bericht in target/site/jacoco
cd frontend && npm test       # Vitest
cd frontend && npm run build  # TypeScript-Prüfung + Produktions-Build
```

## Stand dieses Grundgerüsts

Umgesetzt sind alle Musskriterien des Pflichtenhefts. Die Wunschkriterien sind vorbereitet
(Erweiterungspunkte in `docs/ARCHITEKTUR.md`), aber nicht umgesetzt.

**Geprüft (beim Erstellen des Grundgerüsts ausgeführt):**

- Frontend: TypeScript-Prüfung, Produktions-Build und Vitest-Tests laufen fehlerfrei.
- Backend-Domänenlogik (Fragetypen, Bewertung, Zeitablauf, Anzeigenamen, Scoreboard, Bildverkleinerung):
  28 Unit-Tests laufen fehlerfrei.
- Datenbankschema: `V1__init.sql` läuft fehlerfrei in PostgreSQL 16; alle Spalten passen zu den Entities.

**Noch nicht geprüft (bitte als Erstes tun):**

- Die Spring-Schicht (Controller, Services, Security, WebSocket) und `FlowIntegrationTest` konnten in der
  Erstellungsumgebung nicht kompiliert werden, weil Maven Central dort gesperrt war. Der erste Schritt ist
  deshalb `cd backend && mvn verify`. Kleinere Compilerfehler oder ein falscher Abhängigkeitsname sind möglich.
- `Dockerfile` und `docker-compose.yml` wurden nicht ausgeführt.
- Tests auf echten Smartphones (Chrome auf Android, Safari auf iOS) und der Lasttest mit 200 Verbindungen fehlen.

## Bekannte offene Punkte

- CSRF-Schutz der Dozenten-API ist abgeschaltet (`SecurityConfig`, TODO). Vor einem echten Einsatz aktivieren.
- `spring.jpa.hibernate.ddl-auto` steht auf `none`. Nach dem ersten erfolgreichen Start auf `validate` stellen.
- Wird eine Katalogfrage gelöscht, verschwinden auch ihre früheren Durchläufe und Antworten (Punkte bleiben).
- Es gibt noch kein ESLint/Checkstyle und keinen Lasttest (k6/Gatling).
- Starten zwei Browser-Tabs desselben Dozenten gleichzeitig eine Frage in derselben Session, kann die Regel
  „höchstens eine Frage gleichzeitig“ (/MK210/) im selben Augenblick umgangen werden. Abhilfe: Sperre auf der Session.
