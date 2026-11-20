import { expect, type Page, test } from "@playwright/test";
import { Dozent, F1 } from "./backend";

// Teilnehmeransicht auf dem Smartphone: 360 px Chrome und iPhone Safari (/MK340/).

const NAME = /^[BCDFGHJKLMNPQRSTVWXZ2-9]{3,}$/;

async function expectNoHorizontalScroll(page: Page) {
  const fits = await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 1);
  expect(fits, "Seite ist breiter als der Bildschirm").toBe(true);
}

test("@teilnehmer /MK160/ bis /MK280/ Beitritt, Antwort und Ergebnis ohne Neuladen (/T100/, /T120/)", async ({
  page, baseURL,
}) => {
  const dozent = await Dozent.create(baseURL!);
  const question = await dozent.question(F1);
  const session = await dozent.session();

  // /MK160/, /MK170/: Beitritt über den Link, Name wird vergeben
  await page.goto(`/join/${session.joinCode}`);
  const name = page.locator(".participant-header strong.mono");
  await expect(name).toHaveText(NAME);
  const displayName = await name.textContent();
  await expect(page.getByText("Warte auf die nächste Frage.")).toBeVisible();
  await expectNoHorizontalScroll(page);

  // /MK220/, /MK230/: Frage erscheint ohne Neuladen, mit Restzeit
  const runId = await dozent.start(session.id, question.id);
  await expect(page.getByRole("heading", { name: F1.text })).toBeVisible({ timeout: 3_000 });
  await expect(page.getByRole("timer")).toBeVisible();
  await expectNoHorizontalScroll(page);

  // /MK240/, /MK245/: Antwort wird erst nach Bestätigung als gespeichert angezeigt
  await page.getByRole("button", { name: "Schicht 3" }).click();
  await page.getByRole("button", { name: "Absenden" }).click();
  await expect(page.getByText("Antwort gespeichert.")).toBeVisible();

  // /MK270/, /MK280/: nach dem Ende Ergebnis und Lösung
  await dozent.close(runId);
  await expect(page.getByText("Richtig")).toBeVisible({ timeout: 3_000 });
  await expect(page.getByText("Lösung:")).toBeVisible();
  await expect(page.getByText("Rang 1 von 1")).toBeVisible();

  // /MK180/: nach dem Neuladen gleicher Name und Punktestand
  await page.reload();
  await expect(name).toHaveText(displayName!);
  await expect(page.locator(".participant-score strong")).toHaveText("1");
  await dozent.dispose();
});

test("@teilnehmer /MK410/ erkennt einen Verbindungsabbruch und verbindet sich selbst neu (/T115/)", async ({
  page, baseURL,
}) => {
  const dozent = await Dozent.create(baseURL!);
  const question = await dozent.question(F1);
  const session = await dozent.session();

  // Die WebSocket-Verbindung läuft über einen Test-Proxy, den der Test trennen kann.
  const sockets: { close: () => Promise<void> }[] = [];
  await page.routeWebSocket(/\/ws$/, (ws) => {
    ws.connectToServer();
    sockets.push(ws);
  });

  await page.goto(`/join/${session.joinCode}`);
  await expect(page.getByText("Warte auf die nächste Frage.")).toBeVisible();
  await expect.poll(() => sockets.length).toBe(1);

  await sockets[0]!.close();
  await expect(page.getByText("Verbindung unterbrochen. Verbinde neu …")).toBeVisible();

  // Während der Unterbrechung startet der Dozent eine Frage
  await dozent.start(session.id, question.id);
  await expect.poll(() => sockets.length, { timeout: 5_000 }).toBe(2);
  await expect(page.getByText("Verbindung unterbrochen. Verbinde neu …")).toBeHidden();
  await expect(page.getByRole("heading", { name: F1.text })).toBeVisible({ timeout: 3_000 });
  await dozent.dispose();
});
