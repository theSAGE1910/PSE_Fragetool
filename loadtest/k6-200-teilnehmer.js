// Lasttest /P60/ für /MK390/: 200 gleichzeitige, aktive WebSocket-Verbindungen in einer Session.
//
// Voraussetzung: Anwendung läuft (docker compose up --build), k6 ist installiert (https://k6.io).
// Start:         k6 run loadtest/k6-200-teilnehmer.js
// Andere Adresse: k6 run -e BASE_URL=http://192.168.0.10:8080 loadtest/k6-200-teilnehmer.js
//
// Ablauf: setup() legt Dozent, Frage F1 und Session an. 200 virtuelle Teilnehmer treten bei,
// öffnen je eine STOMP-Verbindung und bleiben 5 Minuten verbunden. Der virtuelle Dozent startet
// in dieser Zeit zweimal F1 und beendet sie wieder. Jeder Teilnehmer antwortet auf jede Frage.

import http from "k6/http";
import ws from "k6/ws";
import { check, sleep } from "k6";
import { Counter, Trend } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const WS_URL = BASE_URL.replace(/^http/, "ws") + "/ws";
const PARTICIPANTS = Number(__ENV.PARTICIPANTS || 200);
const HOLD_SECONDS = Number(__ENV.HOLD_SECONDS || 300);
const ROUNDS = 2;
const JSON_HEADERS = { "Content-Type": "application/json" };

const connected = new Counter("teilnehmer_verbunden");
const stateMessages = new Counter("benachrichtigungen_empfangen");
const answersSaved = new Counter("antworten_gespeichert");
const answersRejected = new Counter("antworten_abgelehnt");
const disconnectsEarly = new Counter("verbindungsabbrueche");
// Zeit vom Start der Frage (Serverzeit) bis die Frage beim Teilnehmer geladen ist.
// Aussagekräftig, wenn k6 und Server auf demselben Rechner laufen (gleiche Uhr).
const notifyDelay = new Trend("start_bis_frage_beim_teilnehmer_ms", true);

export const options = {
  scenarios: {
    teilnehmer: {
      executor: "per-vu-iterations",
      exec: "teilnehmer",
      vus: PARTICIPANTS,
      iterations: 1,
      maxDuration: `${HOLD_SECONDS + 120}s`,
    },
    dozent: {
      executor: "per-vu-iterations",
      exec: "dozent",
      vus: 1,
      iterations: 1,
      startTime: "30s", // erst starten, wenn alle Teilnehmer verbunden sind
      maxDuration: `${HOLD_SECONDS + 60}s`,
    },
  },
  thresholds: {
    teilnehmer_verbunden: [`count>=${PARTICIPANTS}`],
    verbindungsabbrueche: ["count==0"],
    antworten_gespeichert: [`count>=${PARTICIPANTS * ROUNDS}`],
    http_req_failed: ["rate<0.01"],
    checks: ["rate>0.99"],
  },
};

/** STOMP-Frame: Befehl, Kopfzeilen, Leerzeile, Inhalt, Nullbyte. */
function frame(command, headers, body = "") {
  const lines = Object.entries(headers).map(([key, value]) => `${key}:${value}`);
  return `${command}\n${lines.join("\n")}\n\n${body}\u0000`;
}

function dozentLogin() {
  const email = `last-${Date.now()}-${Math.floor(Math.random() * 1e6)}@test.de`;
  const credentials = JSON.stringify({ email, password: "Sicher123!" });
  check(http.post(`${BASE_URL}/api/auth/register`, credentials, { headers: JSON_HEADERS }), {
    "Dozent registriert": (r) => r.status === 201,
  });
  const login = http.post(`${BASE_URL}/api/auth/login`, credentials, { headers: JSON_HEADERS });
  check(login, { "Dozent angemeldet": (r) => r.status === 200 });
  return login.cookies.JSESSIONID[0].value;
}

function dozentHeaders(cookie) {
  return { headers: { ...JSON_HEADERS, Cookie: `JSESSIONID=${cookie}` } };
}

export function setup() {
  const cookie = dozentLogin();
  const question = http.post(`${BASE_URL}/api/questions`, JSON.stringify({
    type: "SINGLE_CHOICE",
    text: "Auf welcher OSI-Schicht arbeitet IP?",
    timeLimitSeconds: 60,
    options: [
      { text: "Schicht 2", correct: false },
      { text: "Schicht 3", correct: true },
      { text: "Schicht 4", correct: false },
    ],
  }), dozentHeaders(cookie));
  const session = http.post(`${BASE_URL}/api/sessions`, JSON.stringify({ name: "Lasttest" }),
    dozentHeaders(cookie));
  check(question, { "Frage angelegt": (r) => r.status === 201 });
  check(session, { "Session angelegt": (r) => r.status === 201 });
  return { cookie, questionId: question.json("id"), sessionId: session.json("id"), joinCode: session.json("joinCode") };
}

/** Virtueller Dozent: startet die Frage zweimal und beendet sie nach 30 Sekunden. */
export function dozent(data) {
  for (let round = 0; round < ROUNDS; round++) {
    const start = http.post(`${BASE_URL}/api/sessions/${data.sessionId}/runs`,
      JSON.stringify({ questionId: data.questionId }), dozentHeaders(data.cookie));
    check(start, { "Frage gestartet": (r) => r.status === 201 });
    sleep(30);
    http.post(`${BASE_URL}/api/runs/${start.json("runId")}/close`, null, dozentHeaders(data.cookie));
    sleep(30);
  }
}

/** Virtueller Teilnehmer: beitreten, WebSocket offen halten, auf jede Frage antworten. */
export function teilnehmer(data) {
  const join = http.post(`${BASE_URL}/api/public/sessions/${data.joinCode}/participants`);
  check(join, { "beigetreten": (r) => r.status === 201 });
  const token = join.json("token");
  const tokenHeaders = { headers: { ...JSON_HEADERS, "X-Participant-Token": token } };
  const answered = {};
  let closedByTest = false;

  const response = ws.connect(WS_URL, {}, (socket) => {
    socket.on("open", () => {
      socket.send(frame("CONNECT", { "accept-version": "1.2", host: "localhost", "heart-beat": "0,0" }));
    });

    socket.on("message", (raw) => {
      const command = raw.split("\n")[0].trim();
      if (command === "CONNECTED") {
        connected.add(1);
        socket.send(frame("SUBSCRIBE", { id: "sub-0", destination: `/topic/session/${data.joinCode}` }));
        return;
      }
      if (command !== "MESSAGE") {
        return;
      }
      const body = raw.substring(raw.indexOf("\n\n") + 2).replace(/\u0000$/, "");
      const message = JSON.parse(body);
      if (message.type !== "STATE_CHANGED") {
        return;
      }
      stateMessages.add(1);
      // wie die echte Ansicht: Zustand neu laden und auf eine laufende Frage antworten
      const state = http.get(`${BASE_URL}/api/public/sessions/${data.joinCode}/state`);
      const run = state.json("run");
      if (run && run.status === "RUNNING" && !answered[run.id]) {
        answered[run.id] = true;
        notifyDelay.add(Date.now() - Date.parse(run.startedAt));
        const optionId = run.question.options[Math.floor(Math.random() * run.question.options.length)].id;
        const answer = http.post(`${BASE_URL}/api/public/runs/${run.id}/answers`,
          JSON.stringify({ optionIds: [optionId] }), tokenHeaders);
        if (answer.status === 201) {
          answersSaved.add(1);
        } else {
          answersRejected.add(1);
        }
      }
    });

    socket.on("close", () => {
      if (!closedByTest) {
        disconnectsEarly.add(1);
      }
    });

    socket.setTimeout(() => {
      closedByTest = true;
      socket.send(frame("DISCONNECT", {}));
      socket.close();
    }, HOLD_SECONDS * 1000);
  });
  check(response, { "WebSocket verbunden (101)": (r) => r && r.status === 101 });
}

export function teardown(data) {
  const state = http.get(`${BASE_URL}/api/public/sessions/${data.joinCode}/state`);
  console.log(`Teilnehmer in der Session: ${state.json("participantCount")}, `
    + `Antworten auf die letzte Frage: ${state.json("run.answerCount")}`);
}
