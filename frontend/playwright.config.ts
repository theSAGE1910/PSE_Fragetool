import { defineConfig, devices } from "@playwright/test";

/**
 * Browsertests gegen die laufende Anwendung (Prüfverfahren /P40/, /P50/, /P80/).
 *
 * Vorher die Anwendung starten, z. B. im Projektordner:  docker compose up --build
 * Dann im Ordner frontend:                                npm run e2e
 * Andere Adresse:                                        BASE_URL=http://192.168.0.10:8080 npm run e2e
 *
 * Die Projekte entsprechen den Mindest-Testumgebungen aus /MK340/ und /MK350/:
 *   dozent-chrome, dozent-firefox   Dozentenansicht am Desktop
 *   teilnehmer-360px                Teilnehmeransicht, Chrome (Android) mit 360 px Breite
 *   teilnehmer-iphone               Teilnehmeransicht, Safari (WebKit) auf dem iPhone
 *   messung                         Zeitmessungen /P40/ und /P50/ (nur Chromium)
 */
export default defineConfig({
  testDir: "./e2e",
  timeout: 60_000,
  fullyParallel: false,
  workers: 1,
  reporter: [["list"], ["html", { open: "never", outputFolder: "e2e-report" }]],
  use: {
    baseURL: process.env.BASE_URL ?? "http://localhost:8080",
    locale: "de-DE",
    trace: "retain-on-failure",
  },
  projects: [
    { name: "dozent-chrome", grep: /@dozent/, use: { ...devices["Desktop Chrome"] } },
    { name: "dozent-firefox", grep: /@dozent/, use: { ...devices["Desktop Firefox"] } },
    {
      name: "teilnehmer-360px",
      grep: /@teilnehmer/,
      use: { ...devices["Galaxy S9+"], viewport: { width: 360, height: 740 } },
    },
    { name: "teilnehmer-iphone", grep: /@teilnehmer/, use: { ...devices["iPhone 13"] } },
    { name: "messung", grep: /@messung/, timeout: 15 * 60_000, use: { ...devices["Desktop Chrome"] } },
  ],
});
