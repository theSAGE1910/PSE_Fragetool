package edu.kit.pse.fragetool.question;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.kit.pse.fragetool.support.ApiTestSupport;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

/** Bilder an Fragen: /MK155/, /MK156/, /MK440/, /MK441/ (Testfälle /T45/, /T125/). */
class FragenbildTest extends ApiTestSupport {

    @Test
    @DisplayName("/MK155/, /MK441/ PNG mit 3000 × 2000 px wird auf 1200 × 800 px verkleinert und Teilnehmern gezeigt")
    void grossesPngWirdVerkleinert() throws Exception {
        MockHttpSession dozent = newDozent();
        String questionId = read(createQuestion(dozent, F1), "$.id");

        String imageUrl = read(upload(dozent, questionId, image(3000, 2000, "png"), "b1.png")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.imageUrl");

        byte[] stored = mvc.perform(get(imageUrl))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andReturn().getResponse().getContentAsByteArray();
        BufferedImage read = ImageIO.read(new ByteArrayInputStream(stored));
        assertEquals(1200, read.getWidth());
        assertEquals(800, read.getHeight());

        mvc.perform(get("/api/questions/" + questionId).session(dozent))
                .andExpect(jsonPath("$.imageUrl").value(imageUrl));

        // Das Bild erscheint bei der laufenden Frage in Präsentations- und Teilnehmeransicht
        String session = createSession(dozent, "Rechnernetze VL 05");
        start(dozent, read(session, "$.id"), questionId);
        stateRequest(read(session, "$.joinCode")).andExpect(jsonPath("$.run.question.imageUrl").value(imageUrl));
    }

    @Test
    @DisplayName("/MK440/ JPEG wird angenommen, schmale Bilder bleiben unverändert")
    void jpegWirdAngenommen() throws Exception {
        MockHttpSession dozent = newDozent();
        String questionId = read(createQuestion(dozent, F1), "$.id");
        String imageUrl = read(upload(dozent, questionId, image(800, 600, "jpg"), "foto.jpg")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.imageUrl");
        byte[] stored = mvc.perform(get(imageUrl))
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andReturn().getResponse().getContentAsByteArray();
        BufferedImage read = ImageIO.read(new ByteArrayInputStream(stored));
        assertEquals(800, read.getWidth());
        assertEquals(600, read.getHeight());
    }

    @Test
    @DisplayName("/MK156/, /MK440/ Andere Formate und Dateien über 5 MB werden abgelehnt; das alte Bild bleibt")
    void ungueltigeDateienWerdenAbgelehnt() throws Exception {
        MockHttpSession dozent = newDozent();
        String questionId = read(createQuestion(dozent, F1), "$.id");
        String imageUrl = read(upload(dozent, questionId, image(400, 300, "png"), "ok.png")
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.imageUrl");

        upload(dozent, questionId, image(400, 300, "gif"), "bild.gif")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nur Bilder im Format PNG oder JPEG sind erlaubt."));
        upload(dozent, questionId, "kein Bild".getBytes(StandardCharsets.UTF_8), "video.mp4")
                .andExpect(status().isBadRequest());

        byte[] tooLarge = new byte[ImageProcessor.MAX_BYTES + 1];
        byte[] pngSignature = {(byte) 0x89, 'P', 'N', 'G'};
        System.arraycopy(pngSignature, 0, tooLarge, 0, pngSignature.length);
        upload(dozent, questionId, tooLarge, "gross.png")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Die Datei ist zu groß (höchstens 5 MB)."));

        mvc.perform(get("/api/questions/" + questionId).session(dozent))
                .andExpect(jsonPath("$.imageUrl").value(imageUrl));
        mvc.perform(get(imageUrl)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("/MK155/ Bild ersetzen und entfernen")
    void bildErsetzenUndEntfernen() throws Exception {
        MockHttpSession dozent = newDozent();
        String questionId = read(createQuestion(dozent, F1), "$.id");
        String first = read(upload(dozent, questionId, image(400, 300, "png"), "a.png")
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.imageUrl");
        String second = read(upload(dozent, questionId, image(500, 300, "png"), "b.png")
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.imageUrl");
        assertNotEquals(first, second);
        mvc.perform(get(first)).andExpect(status().isNotFound());
        mvc.perform(get(second)).andExpect(status().isOk());

        mvc.perform(delete("/api/questions/" + questionId + "/image").session(dozent))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/questions/" + questionId).session(dozent))
                .andExpect(jsonPath("$.imageUrl").doesNotExist());
        mvc.perform(get(second)).andExpect(status().isNotFound());
    }

    private ResultActions upload(MockHttpSession dozent, String questionId, byte[] bytes, String filename)
            throws Exception {
        return mvc.perform(multipart("/api/questions/" + questionId + "/image")
                .file(new MockMultipartFile("file", filename, "application/octet-stream", bytes))
                .session(dozent));
    }

    /** Erzeugt ein einfarbiges Testbild im angegebenen Format. */
    private static byte[] image(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(30, 90, 160));
        g.fillRect(0, 0, width, height);
        g.setColor(Color.WHITE);
        g.fillRect(width / 4, height / 4, width / 2, height / 2);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }
}
