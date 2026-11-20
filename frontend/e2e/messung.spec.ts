import { type Browser, expect, type Page, test } from "@playwright/test";
import { Dozent, F1 } from "./backend";

// Zeitmessungen der Prüfverfahren /P40/ und /P50/. Ergebnisse stehen in der Konsole und im HTML-Bericht.

const PARTICIPANTS = Number(process.env.P50_PARTICIPANTS ?? 10);
const CYCLES = Number(process.env.P50_CYCLES ?? 50);
const RECONNECTS = Number(process.env.P40_REPEATS ?? 20);

function quantile(values: number[], q: number): number {
  const sorted = [...values].sort((a, b) => a - b);
  return sorted[Math.min(sorted.length - 1, Math.ceil(q * sorted.length) - 1)] ?? NaN;
}

/** Liefert den Zeitpunkt (Date.now im Browser), an dem ein passendes Element erscheint. */
function whenVisible(page: Page, selector: string, text: string): Promise<number> {
  return page.evaluate(
    ([sel, txt]) =>
      new Promise<number>((resolve) => {
        const matches = () => [...document.querySelectorAll(sel!)].some((el) => el.textContent?.includes(txt!));
        if (matches()) {
          resolve(Date.now());
          return;
        }
        const observer = new MutationObserver(() => {
          if (matches()) {
            observer.disconnect();
            resolve(Date.now());
          }
        });
        observer.observe(document.body, { childList: true, subtree: true, characterData: true });
      }),
    [selector, text],
  );
}

async function openParticipants(browser: Browser, joinCode: string, count: number): Promise<Page[]> {
  const pages: Page[] = [];
  for (let i = 0; i < count; i++) {
    const context = await browser.newContext({ viewport: { width: 360, height: 740 } });
    const page = await context.newPage();
    await page.goto(`/join/${joinCode}`);
    await expect(page.getByText("Warte auf die nächste Frage.")).toBeVisible();
    pages.push(page);
  }
  return pages;
}

test("@messung /P50/ /MK400/ Start und Ende erreichen die Teilnehmer in 95 % der Fälle innerhalb 1 s", async ({
  browser, baseURL,
}) => {
  const dozent = await Dozent.create(baseURL!);
  const question = await dozent.question(F1);
  const session = await dozent.session();
  const pages = await openParticipants(browser, session.joinCode, PARTICIPANTS);

  const delays: number[] = [];
  for (let cycle = 0; cycle < CYCLES; cycle++) {
    // laufende Frage: das Antwortformular mit "Absenden" erscheint
    let shown = pages.map((page) => whenVisible(page, "button", "Absenden"));
    let before = Date.now();
    const runId = await dozent.start(session.id, question.id);
    delays.push(...(await Promise.all(shown)).map((t) => t - before));

    // beendete Frage: ohne eigene Antwort erscheint "Keine Antwort."
    shown = pages.map((page) => whenVisible(page, ".banner", "Keine Antwort"));
    before = Date.now();
    await dozent.close(runId);
    delays.push(...(await Promise.all(shown)).map((t) => t - before));
  }

  const p95 = quantile(delays, 0.95);
  const max = Math.max(...delays);
  console.log(`/P50/: ${delays.length} Messungen, 95-%-Quantil ${p95} ms, Maximum ${max} ms`);
  test.info().annotations.push({ type: "P50", description: `p95=${p95} ms, max=${max} ms, n=${delays.length}` });
  expect(p95).toBeLessThanOrEqual(1000);
  expect(max).toBeLessThanOrEqual(3000);
  await dozent.dispose();
});

test("@messung /P40/ /MK410/ nach einem Abbruch beginnt die Wiederverbindung innerhalb von 5 s", async ({
  page, baseURL,
}) => {
  const dozent = await Dozent.create(baseURL!);
  const session = await dozent.session();
  const opened: number[] = [];
  const sockets: { close: () => Promise<void> }[] = [];
  await page.routeWebSocket(/\/ws$/, (ws) => {
    opened.push(Date.now());
    ws.connectToServer();
    sockets.push(ws);
  });
  await page.goto(`/join/${session.joinCode}`);
  await expect.poll(() => sockets.length).toBe(1);

  const delays: number[] = [];
  for (let i = 0; i < RECONNECTS; i++) {
    const count = sockets.length;
    const closedAt = Date.now();
    await sockets[count - 1]!.close();
    await expect.poll(() => sockets.length, { timeout: 10_000 }).toBe(count + 1);
    delays.push(opened[count]! - closedAt);
    await expect(page.getByText("Verbindung unterbrochen. Verbinde neu …")).toBeHidden();
  }

  console.log(`/P40/: ${delays.length} Wiederverbindungen, längste nach ${Math.max(...delays)} ms`);
  test.info().annotations.push({ type: "P40", description: `max=${Math.max(...delays)} ms, n=${delays.length}` });
  expect(Math.max(...delays)).toBeLessThanOrEqual(5000);
  await dozent.dispose();
});
