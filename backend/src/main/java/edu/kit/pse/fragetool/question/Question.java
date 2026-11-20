package edu.kit.pse.fragetool.question;

import edu.kit.pse.fragetool.auth.Dozent;
import edu.kit.pse.fragetool.common.ValidationException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Eine Frage des Fragenkatalogs oder eine Schnellstart-Frage.
 *
 * <p>Jeder Fragetyp ist eine Unterklasse und entscheidet selbst, wann eine Antwort
 * korrekt ist ({@link #evaluate}) und wie die Antwortverteilung aussieht
 * ({@link #distribution}). Dadurch braucht der übrige Code kein {@code switch}
 * über den Fragetyp.
 */
@Entity
@Table(name = "question")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", length = 32)
public abstract class Question {

    public static final int MIN_TIME_LIMIT_SECONDS = 10;
    public static final int MAX_TIME_LIMIT_SECONDS = 300;
    public static final int DEFAULT_TIME_LIMIT_SECONDS = 45;
    public static final int MAX_TEXT_LENGTH = 2000;

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Dozent owner;

    @Column(nullable = false, length = MAX_TEXT_LENGTH)
    private String text;

    @Column(nullable = false)
    private int timeLimitSeconds;

    /** Schnellstart-Fragen erscheinen nicht im Katalog und werden nicht bewertet (/MK200/). */
    @Column(nullable = false)
    private boolean quickStart;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_tag", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "tag", nullable = false, length = 100)
    private Set<String> tags = new TreeSet<>();

    /** ID des angehängten Bildes (/MK155/), {@code null} ohne Bild. */
    private UUID imageId;

    protected Question() {
        // für JPA
    }

    protected Question(Dozent owner, String text, int timeLimitSeconds, boolean quickStart) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.text = text;
        this.timeLimitSeconds = timeLimitSeconds;
        this.quickStart = quickStart;
    }

    public abstract QuestionType getType();

    /** Ob dieser Fragetyp grundsätzlich eine korrekte Lösung hat. */
    protected abstract boolean hasSolution();

    /** Ob die Antwort korrekt ist. Nur für bewertbare Fragen definiert. */
    public abstract boolean evaluate(AnswerContent answer);

    /** Prüft, ob die Antwort zur Frage passt. Wirft {@link ValidationException}, wenn nicht. */
    public abstract void validateAnswer(AnswerContent answer);

    /** Antwortverteilung über alle abgegebenen Antworten (/MK290/). */
    public abstract Distribution distribution(Collection<AnswerContent> answers);

    /** Lesbare Lösung für die Anzeige nach Fragenende (/MK270/), sonst {@code null}. */
    public abstract String solutionText();

    /** Übernimmt die typabhängigen Angaben. Unterklassen verwenden nur, was sie brauchen. */
    public abstract void configure(List<OptionSpec> options, BigDecimal correctValue, BigDecimal tolerance);

    /** Prüft die typabhängigen Regeln (/MK70/, /MK80/). */
    protected abstract void validateDetails();

    /** Bewertbar sind nur Katalogfragen mit Lösung (/MK90/, /MK200/, /MK310/). */
    public boolean isGradable() {
        return hasSolution() && !quickStart;
    }

    /** Prüft alle Regeln der Frage. Wirft {@link ValidationException} mit deutscher Meldung. */
    public void validate() {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Der Fragetext darf nicht leer sein.");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new ValidationException("Der Fragetext ist zu lang.");
        }
        if (timeLimitSeconds < MIN_TIME_LIMIT_SECONDS || timeLimitSeconds > MAX_TIME_LIMIT_SECONDS) {
            throw new ValidationException("Zeitlimit muss zwischen 10 und 300 Sekunden liegen.");
        }
        validateDetails();
    }

    public void update(String newText, int newTimeLimitSeconds, Collection<String> newTags) {
        this.text = newText == null ? null : newText.strip();
        this.timeLimitSeconds = newTimeLimitSeconds;
        this.tags.clear();
        if (newTags != null) {
            for (String tag : newTags) {
                if (tag != null && !tag.isBlank()) {
                    // Locale.ROOT: sonst wird "IP" auf Rechnern mit türkischer Sprache zu "ıp"
                    this.tags.add(tag.strip().toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    public void attachImage(UUID newImageId) {
        this.imageId = newImageId;
    }

    public void removeImage() {
        this.imageId = null;
    }

    public UUID getImageId() {
        return imageId;
    }

    /** Öffentliche Adresse des Bildes dieser Frage, {@code null} ohne Bild. */
    public String getImageUrl() {
        return imageId == null ? null : imageUrl(imageId);
    }

    public static String imageUrl(UUID imageId) {
        return "/api/public/images/" + imageId;
    }

    public boolean isOwnedBy(Dozent dozent) {
        return owner.getId().equals(dozent.getId());
    }

    /** Antwortmöglichkeiten; leer bei Fragetypen ohne Auswahl. */
    public List<AnswerOption> getOptions() {
        return List.of();
    }

    /** Korrekter Wert einer numerischen Frage, sonst {@code null}. */
    public BigDecimal getCorrectValue() {
        return null;
    }

    /** Toleranz einer numerischen Frage, sonst {@code null}. */
    public BigDecimal getTolerance() {
        return null;
    }

    public UUID getId() {
        return id;
    }

    public Dozent getOwner() {
        return owner;
    }

    public String getText() {
        return text;
    }

    public int getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public boolean isQuickStart() {
        return quickStart;
    }

    public Set<String> getTags() {
        return Set.copyOf(tags);
    }

    /** Zahl ohne überflüssige Nachkommastellen, z. B. "100" statt "100.0000". */
    protected static String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
