import { type FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { questions as questionApi } from "../api/dozent";
import { errorMessage } from "../api/http";
import { type OptionDraft, QUESTION_TYPE_LABELS, type QuestionRequest, type QuestionType } from "../api/types";

const MIN_OPTIONS = 2;
const MAX_OPTIONS = 6;
const DEFAULT_TIME_LIMIT = 45;

/** Erstellen und Bearbeiten einer Frage (/MK50/ bis /MK110/). Die Regeln prüft der Server. */
export function QuestionEditorPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [type, setType] = useState<QuestionType>("SINGLE_CHOICE");
  const [text, setText] = useState("");
  const [timeLimit, setTimeLimit] = useState(DEFAULT_TIME_LIMIT);
  const [tags, setTags] = useState("");
  const [options, setOptions] = useState<OptionDraft[]>([
    { text: "", correct: true },
    { text: "", correct: false },
  ]);
  const [correctValue, setCorrectValue] = useState("");
  const [tolerance, setTolerance] = useState("0");
  const [error, setError] = useState<string | null>(null);
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [newImage, setNewImage] = useState<File | null>(null);
  const [removeImage, setRemoveImage] = useState(false);

  useEffect(() => {
    if (!id) {
      return;
    }
    questionApi.get(id).then(
      (q) => {
        setType(q.type);
        setText(q.text);
        setTimeLimit(q.timeLimitSeconds);
        setTags(q.tags.join(", "));
        if (q.options.length > 0) {
          setOptions(q.options.map((o) => ({ text: o.text, correct: o.correct })));
        }
        setCorrectValue(q.correctValue === null ? "" : String(q.correctValue));
        setTolerance(q.tolerance === null ? "0" : String(q.tolerance));
        setImageUrl(q.imageUrl);
      },
      (e) => setError(errorMessage(e)),
    );
  }, [id]);

  const isChoice = type === "SINGLE_CHOICE" || type === "MULTIPLE_CHOICE";

  function changeType(next: QuestionType) {
    setType(next);
    if (next === "SINGLE_CHOICE") {
      // Bei Single Choice bleibt nur die erste markierte Antwort korrekt.
      const first = options.findIndex((o) => o.correct);
      setOptions(options.map((o, i) => ({ ...o, correct: i === Math.max(first, 0) })));
    }
  }

  function markCorrect(index: number, checked: boolean) {
    setOptions(
      options.map((o, i) =>
        type === "SINGLE_CHOICE" ? { ...o, correct: i === index } : i === index ? { ...o, correct: checked } : o,
      ),
    );
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    const request: QuestionRequest = {
      type,
      text,
      timeLimitSeconds: timeLimit,
      tags: tags.split(",").map((t) => t.trim()).filter((t) => t.length > 0),
      options: isChoice ? options : [],
      correctValue: type === "NUMERIC" && correctValue !== "" ? Number(correctValue.replace(",", ".")) : null,
      tolerance: type === "NUMERIC" ? Number(tolerance.replace(",", ".") || "0") : null,
    };
    try {
      const saved = id ? await questionApi.update(id, request) : await questionApi.create(request);
      // Das Bild braucht die ID der Frage und wird deshalb nach dem Speichern übertragen.
      if (newImage) {
        await questionApi.uploadImage(saved.id, newImage);
      } else if (removeImage && imageUrl) {
        await questionApi.removeImage(saved.id);
      }
      navigate("/dozent");
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  return (
    <main className="page">
      <header className="topbar">
        <h1>{id ? "Frage bearbeiten" : "Frage erstellen"}</h1>
        <Link className="button" to="/dozent">Zurück</Link>
      </header>
      <form onSubmit={submit} className="card form">
        <label>
          Fragetyp
          <select value={type} onChange={(e) => changeType(e.target.value as QuestionType)} disabled={Boolean(id)}>
            {(Object.keys(QUESTION_TYPE_LABELS) as QuestionType[]).map((t) => (
              <option key={t} value={t}>{QUESTION_TYPE_LABELS[t]}</option>
            ))}
          </select>
        </label>
        <label>
          Fragetext
          <textarea value={text} onChange={(e) => setText(e.target.value)} rows={3} required maxLength={2000} />
        </label>

        {isChoice && (
          <fieldset>
            <legend>Antwortmöglichkeiten ({type === "SINGLE_CHOICE" ? "genau eine" : "mindestens eine"} korrekt)</legend>
            {options.map((option, index) => (
              <div key={index} className="option-row">
                <input
                  type={type === "SINGLE_CHOICE" ? "radio" : "checkbox"}
                  name="correct"
                  checked={option.correct}
                  onChange={(e) => markCorrect(index, e.target.checked)}
                  aria-label={`Antwort ${index + 1} ist korrekt`}
                />
                <input
                  value={option.text}
                  onChange={(e) => setOptions(options.map((o, i) => (i === index ? { ...o, text: e.target.value } : o)))}
                  placeholder={`Antwort ${index + 1}`}
                  aria-label={`Text der Antwort ${index + 1}`}
                  required
                  maxLength={500}
                />
                <button
                  type="button"
                  onClick={() => setOptions(options.filter((_, i) => i !== index))}
                  disabled={options.length <= MIN_OPTIONS}
                  aria-label={`Antwort ${index + 1} entfernen`}
                >
                  ✕
                </button>
              </div>
            ))}
            <button
              type="button"
              onClick={() => setOptions([...options, { text: "", correct: false }])}
              disabled={options.length >= MAX_OPTIONS}
            >
              Antwort hinzufügen
            </button>
          </fieldset>
        )}

        {type === "NUMERIC" && (
          <div className="inline-form">
            <label className="grow">
              Korrekter Wert
              <input value={correctValue} onChange={(e) => setCorrectValue(e.target.value)} inputMode="decimal" required />
            </label>
            <label className="grow">
              Toleranz (±)
              <input value={tolerance} onChange={(e) => setTolerance(e.target.value)} inputMode="decimal" />
            </label>
          </div>
        )}

        {type === "FREE_TEXT" && <p className="hint">Freitextfragen haben keine Lösung und werden nicht bewertet.</p>}

        <div className="inline-form">
          <label className="grow">
            Zeitlimit in Sekunden (10–300)
            <input
              type="number"
              value={timeLimit}
              onChange={(e) => setTimeLimit(Number(e.target.value))}
              min={10}
              max={300}
              required
            />
          </label>
          <label className="grow">
            Tags (durch Komma getrennt)
            <input value={tags} onChange={(e) => setTags(e.target.value)} placeholder="ip, schicht" />
          </label>
        </div>

        <fieldset>
          <legend>Bild (optional, PNG oder JPEG, höchstens 5 MB)</legend>
          {imageUrl && !removeImage && !newImage && <img className="question-image small" src={imageUrl} alt="Bild zur Frage" />}
          <input
            type="file"
            accept="image/png,image/jpeg"
            aria-label="Bilddatei auswählen"
            onChange={(e) => {
              setNewImage(e.target.files?.[0] ?? null);
              setRemoveImage(false);
            }}
          />
          {imageUrl && !newImage && (
            <label className="checkbox">
              <input type="checkbox" checked={removeImage} onChange={(e) => setRemoveImage(e.target.checked)} />
              Bild entfernen
            </label>
          )}
        </fieldset>

        {error && <p className="error" role="alert">{error}</p>}
        <button type="submit" className="primary">Speichern</button>
      </form>
    </main>
  );
}
