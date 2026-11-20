package edu.kit.pse.fragetool.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.kit.pse.fragetool.auth.Dozent;
import edu.kit.pse.fragetool.common.NotFoundException;
import edu.kit.pse.fragetool.common.ParticipantCountChangedEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

/** Namensvergabe beim Beitritt ohne Datenbank: /MK170/, /MK173/. */
class ParticipantServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-16T10:00:00Z");

    private final SessionRepository sessions = mock(SessionRepository.class);
    private final ParticipantRepository participants = mock(ParticipantRepository.class);
    private final DisplayNameGenerator names = mock(DisplayNameGenerator.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Session session = new Session(new Dozent("d@test.de", "hash", NOW), "VL", "BCDFGH", NOW);
    private ParticipantService service;

    @BeforeEach
    void setUp() {
        service = new ParticipantService(sessions, participants, names, events, Clock.fixed(NOW, ZoneOffset.UTC));
        when(sessions.findByJoinCode("BCDFGH")).thenReturn(Optional.of(session));
        when(participants.countBySession(session)).thenReturn(4L);
        when(participants.saveAndFlush(any(Participant.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    @DisplayName("/MK170/ Neuer Teilnehmer erhält einen Namen und ein zufälliges Token")
    void vergibtNamenUndToken() {
        when(names.generate(5)).thenReturn("K7X", "B4D");
        Participant first = service.join("BCDFGH");
        Participant second = service.join("BCDFGH");
        assertEquals("K7X", first.getDisplayName());
        assertEquals(0, first.getPoints());
        assertNotEquals(first.getToken(), second.getToken());
        assertEquals(43, first.getToken().length());
        verify(events, times(2)).publishEvent(new ParticipantCountChangedEvent("BCDFGH", 5));
    }

    @Test
    @DisplayName("/MK173/ Ein schon vergebener Name wird übersprungen")
    void ueberspringtVergebeneNamen() {
        when(names.generate(5)).thenReturn("K7X", "B4D");
        when(participants.existsBySessionAndDisplayName(session, "K7X")).thenReturn(true);
        assertEquals("B4D", service.join("BCDFGH").getDisplayName());
    }

    @Test
    @DisplayName("/MK173/ Vergibt die Datenbank denselben Namen gleichzeitig, wird mit neuem Namen wiederholt")
    void wiederholtNachGleichzeitigerVergabe() {
        when(names.generate(5)).thenReturn("K7X", "B4D");
        when(participants.saveAndFlush(any(Participant.class)))
                .thenThrow(new DataIntegrityViolationException("uq_participant_name"))
                .thenAnswer(call -> call.getArgument(0));
        assertEquals("B4D", service.join("BCDFGH").getDisplayName());
    }

    @Test
    @DisplayName("Gibt nach 20 vergeblichen Versuchen auf")
    void gibtNachZuVielenVersuchenAuf() {
        when(names.generate(anyLong())).thenReturn("K7X");
        when(participants.existsBySessionAndDisplayName(eq(session), anyString())).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.join("BCDFGH"));
        verify(participants, never()).saveAndFlush(any(Participant.class));
    }

    @Test
    @DisplayName("Unbekannter Beitrittscode und unbekanntes Token")
    void unbekannteCodes() {
        when(sessions.findByJoinCode("XXXXXX")).thenReturn(Optional.empty());
        when(participants.findByToken("falsch")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.join("XXXXXX"));
        assertThrows(NotFoundException.class, () -> service.requireByToken("falsch"));
    }
}
