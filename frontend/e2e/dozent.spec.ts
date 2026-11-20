import { expect, test } from "@playwright/test";
import { Dozent, F1, PASSWORD, uniqueEmail } from "./backend";

// Dozentenansicht in Chrome und Firefox am Desktop (/MK350/), Texte auf Deutsch (/MK370/).

test("@dozent /MK10/, /MK20/ registrieren, anmelden und abmelden (/T10/, /T20/)", async ({ page }) => {
  const email = uniqueEmail();
  await page.goto("/login");
  await expect(page.getByRole("heading", { name: "Anmelden" })).toBeVisible();
  await page.getByRole("button", { name: "Noch kein Konto? Registrieren" }).click();
  await page.getByLabel("E-Mail-Adresse").fill(email);
  await page.getByLabel("Passwort").fill(PASSWORD);
  await page.getByRole("button", { name: "Konto erstellen" }).click();
  await expect(page.getByRole("heading", { name: "Sessions" })).toBeVisible();
  await expect(page.getByText("Noch keine Session.")).toBeVisible();

  await page.getByRole("button", { name: "Abmelden" }).click();
  await expect(page.getByRole("heading", { name: "Anmelden" })).toBeVisible();
  await page.goto("/dozent");
  await expect(page.getByRole("heading", { name: "Anmelden" })).toBeVisible();

  await page.getByLabel("E-Mail-Adresse").fill(email);
  await page.getByLabel("Passwort").fill("falsch123");
  await page.getByRole("button", { name: "Anmelden" }).click();
  await expect(page.getByRole("alert")).toHaveText("E-Mail oder Passwort falsch.");
  await page.getByLabel("Passwort").fill(PASSWORD);
  await page.getByRole("button", { name: "Anmelden" }).click();
  await expect(page.getByRole("heading", { name: "Sessions" })).toBeVisible();
});

test("@dozent /MK130/ bis /MK150/, /MK190/, /MK260/ Session anlegen, Frage starten und beenden", async ({
  page, context, baseURL,
}) => {
  const dozent = await Dozent.create(baseURL!);
  await dozent.question(F1);
  // Die Sitzung des API-Dozenten in den Browser übernehmen
  await context.addCookies((await dozent.api.storageState()).cookies);

  await page.goto("/dozent");
  await page.getByLabel("Name der neuen Session").fill("Rechnernetze VL 05");
  await page.getByRole("button", { name: "Neue Session" }).click();
  await expect(page.getByText("Rechnernetze VL 05")).toBeVisible();
  await page.getByRole("link", { name: "Öffnen" }).click();

  // Präsentationsansicht mit QR-Code in einem zweiten Tab
  const [presentation] = await Promise.all([
    context.waitForEvent("page"),
    page.getByRole("link", { name: "Präsentationsansicht" }).click(),
  ]);
  await expect(presentation.getByRole("heading", { name: "Jetzt beitreten" })).toBeVisible();
  await expect(presentation.locator("svg").first()).toBeVisible();
  await expect(presentation.getByText(/\/join\/[A-Z0-9]{6}$/).first()).toBeVisible();

  await page.getByRole("button", { name: "Starten" }).click();
  await expect(page.getByRole("heading", { name: "Laufende Frage" })).toBeVisible();
  await expect(presentation.getByRole("heading", { name: F1.text })).toBeVisible({ timeout: 3_000 });
  await expect(presentation.getByRole("timer")).toBeVisible();

  await page.getByRole("button", { name: "Beenden" }).click();
  await expect(presentation.getByText("Lösung:")).toBeVisible({ timeout: 3_000 });
  await expect(page.getByRole("heading", { name: "Letztes Ergebnis" })).toBeVisible();
  await dozent.dispose();
});
