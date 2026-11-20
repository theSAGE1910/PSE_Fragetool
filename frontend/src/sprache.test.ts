/// <reference types="node" />
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

// /MK370/: Die Benutzeroberfläche ist auf Deutsch.
// Ergänzt die Sichtprüfung /P80/: sucht in allen Ansichten nach typischen englischen Bedienwörtern.
// Fachbegriffe aus dem Pflichtenheft wie "Session", "Scoreboard" oder "Single Choice" sind erlaubt.

const ROOT = fileURLToPath(new URL("..", import.meta.url));
const ENGLISH_UI_WORDS = [
  "Submit", "Save", "Cancel", "Delete", "Edit", "Loading", "Login", "Log in", "Logout", "Log out",
  "Sign in", "Sign up", "Register", "Password", "Back", "Next", "Start", "Stop", "Error", "Question",
  "Answer", "Settings", "Close", "Search", "Wait", "Time", "Correct", "Wrong", "Points", "Rank",
];

function sourceFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((name: string) => {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) {
      return name === "test" ? [] : sourceFiles(path);
    }
    return path.endsWith(".tsx") && !path.endsWith(".test.tsx") ? [path] : [];
  });
}

/** Sichtbare Texte: Text zwischen JSX-Tags sowie placeholder-, aria-label-, alt- und title-Attribute. */
function visibleTexts(source: string): string[] {
  const texts: string[] = [];
  for (const match of source.matchAll(/>([^<>{}]*[A-Za-zÄÖÜäöüß][^<>{}]*)</g)) {
    texts.push((match[1] ?? "").trim());
  }
  for (const match of source.matchAll(/(?:placeholder|aria-label|alt|title)="([^"]+)"/g)) {
    texts.push(match[1] ?? "");
  }
  return texts.filter((text) => text.length > 0);
}

describe("Sprache der Oberfläche", () => {
  it("/MK370/ das HTML-Dokument ist als deutsch ausgezeichnet", () => {
    expect(readFileSync(join(ROOT, "index.html"), "utf8")).toContain('<html lang="de">');
  });

  it("/MK370/ sichtbare Texte enthalten keine englischen Bedienwörter", () => {
    const findings: string[] = [];
    const files = sourceFiles(join(ROOT, "src"));
    expect(files.length).toBeGreaterThan(5);
    for (const file of files) {
      for (const text of visibleTexts(readFileSync(file, "utf8"))) {
        for (const word of ENGLISH_UI_WORDS) {
          if (new RegExp(`(^|[^A-Za-zÄÖÜäöüß])${word}([^A-Za-zÄÖÜäöüß]|$)`).test(text)) {
            findings.push(`${file}: "${text}" enthält "${word}"`);
          }
        }
      }
    }
    expect(findings).toEqual([]);
  });
});
