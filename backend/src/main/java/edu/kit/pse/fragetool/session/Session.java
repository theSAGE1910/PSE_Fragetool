package edu.kit.pse.fragetool.session;

import edu.kit.pse.fragetool.auth.Dozent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** Eine Session, z. B. ein Vorlesungstermin. Der Beitrittscode steckt im Link und im QR-Code (/MK140/). */
@Entity
@Table(name = "session")
public class Session {

    public static final int MAX_NAME_LENGTH = 200;

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Dozent owner;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(nullable = false, unique = true, length = 16)
    private String joinCode;

    @Column(nullable = false)
    private Instant createdAt;

    /** Ob die Präsentationsansicht gerade das Scoreboard zeigt (/MK320/). */
    @Column(nullable = false)
    private boolean scoreboardVisible;

    protected Session() {
        // für JPA
    }

    public Session(Dozent owner, String name, String joinCode, Instant now) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.name = name;
        this.joinCode = joinCode;
        this.createdAt = now;
    }

    public boolean isOwnedBy(Dozent dozent) {
        return owner.getId().equals(dozent.getId());
    }

    public void setScoreboardVisible(boolean visible) {
        this.scoreboardVisible = visible;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isScoreboardVisible() {
        return scoreboardVisible;
    }
}
