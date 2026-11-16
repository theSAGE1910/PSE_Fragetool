package edu.kit.pse.fragetool.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Ein anonymer Teilnehmer einer Session.
 *
 * <p>Gespeichert werden nur Anzeigename, Token zur Wiedererkennung, Punkte und
 * Beitrittszeitpunkt (/MK360/). Das Token liegt im Browser des Teilnehmers (/MK180/).
 */
@Entity
@Table(name = "participant",
        uniqueConstraints = @UniqueConstraint(name = "uq_participant_name", columnNames = {"session_id", "display_name"}))
public class Participant {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Session session;

    @Column(name = "display_name", nullable = false, length = 16)
    private String displayName;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(nullable = false)
    private int points;

    @Column(nullable = false)
    private Instant joinedAt;

    protected Participant() {
        // für JPA
    }

    public Participant(Session session, String displayName, String token, Instant now) {
        this.id = UUID.randomUUID();
        this.session = session;
        this.displayName = displayName;
        this.token = token;
        this.points = 0;
        this.joinedAt = now;
    }

    public void addPoints(int additional) {
        if (additional < 0) {
            throw new IllegalArgumentException("Punkte dürfen nicht negativ sein.");
        }
        this.points += additional;
    }

    public UUID getId() {
        return id;
    }

    public Session getSession() {
        return session;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getToken() {
        return token;
    }

    public int getPoints() {
        return points;
    }
}
