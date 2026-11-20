package edu.kit.pse.fragetool.scoreboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.kit.pse.fragetool.support.ApiTestSupport;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

/** Scoreboard und eigener Rang: /MK320/, /MK330/, /MK335/ (Testfall /T190/). */
class ScoreboardAnzeigeTest extends ApiTestSupport {

    @Test
    @DisplayName("/MK320/, /MK330/, /MK335/ Top 10 mit gleichen Rängen, alphabetisch; jeder sieht seinen Rang")
    void scoreboard() throws Exception {
        MockHttpSession dozent = newDozent();
        String question = createQuestion(dozent, F1);
        String wrong = read(question, "$.options[0].id");
        String correct = read(question, "$.options[1].id");
        String session = createSession(dozent, "Rechnernetze VL 05");
        String sessionId = read(session, "$.id");
        String code = read(session, "$.joinCode");

        List<Joined> right = new ArrayList<>();
        List<Joined> wrongOnes = new ArrayList<>();
        String runId = start(dozent, sessionId, read(question, "$.id"));
        for (int i = 0; i < 12; i++) {
            Joined participant = join(code);
            boolean answersCorrectly = i < 5;
            answerOption(runId, participant.token(), answersCorrectly ? correct : wrong)
                    .andExpect(status().isCreated());
            (answersCorrectly ? right : wrongOnes).add(participant);
        }
        close(dozent, runId);
        right.sort(Comparator.comparing(Joined::displayName));
        wrongOnes.sort(Comparator.comparing(Joined::displayName));

        // zunächst ausgeblendet
        stateRequest(code)
                .andExpect(jsonPath("$.scoreboardVisible").value(false))
                .andExpect(jsonPath("$.scoreboard.length()").value(0));

        setScoreboardVisible(dozent, sessionId, true).andExpect(status().isNoContent());
        String state = state(code);
        assertEquals(true, read(state, "$.scoreboardVisible"));
        assertEquals(10, (int) read(state, "$.scoreboard.length()"));
        for (int i = 0; i < 5; i++) {
            assertEquals(1, (int) read(state, "$.scoreboard[" + i + "].rank"));
            assertEquals(1, (int) read(state, "$.scoreboard[" + i + "].points"));
            assertEquals(right.get(i).displayName(), read(state, "$.scoreboard[" + i + "].displayName"));
        }
        for (int i = 5; i < 10; i++) {
            assertEquals(6, (int) read(state, "$.scoreboard[" + i + "].rank"));
            assertEquals(0, (int) read(state, "$.scoreboard[" + i + "].points"));
            assertEquals(wrongOnes.get(i - 5).displayName(), read(state, "$.scoreboard[" + i + "].displayName"));
        }

        // jeder Teilnehmer sieht Punkte und Rang, auch außerhalb der Top 10
        for (Joined participant : right) {
            meRequest(participant.token())
                    .andExpect(jsonPath("$.rank").value(1))
                    .andExpect(jsonPath("$.points").value(1))
                    .andExpect(jsonPath("$.participantCount").value(12));
        }
        meRequest(wrongOnes.get(6).token()).andExpect(jsonPath("$.rank").value(6));

        setScoreboardVisible(dozent, sessionId, false).andExpect(status().isNoContent());
        stateRequest(code).andExpect(jsonPath("$.scoreboard.length()").value(0));

        // Beim Start der nächsten Frage wird das Scoreboard ausgeblendet
        setScoreboardVisible(dozent, sessionId, true).andExpect(status().isNoContent());
        start(dozent, sessionId, read(question, "$.id"));
        stateRequest(code).andExpect(jsonPath("$.scoreboardVisible").value(false));
    }
}
