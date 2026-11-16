package edu.kit.pse.fragetool.live;

import edu.kit.pse.fragetool.common.AnswerCountChangedEvent;
import edu.kit.pse.fragetool.common.ParticipantCountChangedEvent;
import edu.kit.pse.fragetool.common.SessionChangedEvent;
import java.time.Clock;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sendet Änderungen per WebSocket an die Ansichten einer Session (/MK220/, /MK400/).
 *
 * <p>Die Services kennen WebSocket nicht. Sie veröffentlichen nur Ereignisse (Beobachter-Muster).
 * Gesendet wird erst nach dem Commit der Transaktion. So finden die Ansichten beim
 * anschließenden Nachladen des Zustands garantiert die neuen Daten vor.
 */
@Component
public class LiveSessionPublisher {

    private final SimpMessagingTemplate messaging;
    private final Clock clock;

    public LiveSessionPublisher(SimpMessagingTemplate messaging, Clock clock) {
        this.messaging = messaging;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSessionChanged(SessionChangedEvent event) {
        send(event.joinCode(), LiveMessage.STATE_CHANGED, null);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAnswerCountChanged(AnswerCountChangedEvent event) {
        send(event.joinCode(), LiveMessage.ANSWER_COUNT, event.answerCount());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onParticipantCountChanged(ParticipantCountChangedEvent event) {
        send(event.joinCode(), LiveMessage.PARTICIPANT_COUNT, event.participantCount());
    }

    public static String topic(String joinCode) {
        return "/topic/session/" + joinCode;
    }

    private void send(String joinCode, String type, Long value) {
        messaging.convertAndSend(topic(joinCode), new LiveMessage(type, value, clock.instant()));
    }
}
