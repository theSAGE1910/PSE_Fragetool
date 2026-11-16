package edu.kit.pse.fragetool.question;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.pse.fragetool.common.ValidationException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Prüft /MK156/, /MK440/ und /MK441/. */
class ImageProcessorTest {

    private final ImageProcessor processor = new ImageProcessor();

    private static byte[] image(int width, int height, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, out);
        return out.toByteArray();
    }

    @Test
    void verkleinertBreiteBilderAuf1200PixelUndErhaeltDasSeitenverhaeltnis() throws IOException {
        ImageProcessor.ProcessedImage result = processor.process(image(3000, 2000, "png"));
        assertEquals(1200, result.width());
        assertEquals(800, result.height());
        assertEquals("image/png", result.contentType());
        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(result.data()));
        assertEquals(1200, stored.getWidth());
        assertEquals(800, stored.getHeight());
    }

    @Test
    void laesstKleineBilderUnveraendert() throws IOException {
        byte[] original = image(1200, 900, "jpeg");
        ImageProcessor.ProcessedImage result = processor.process(original);
        assertEquals(1200, result.width());
        assertEquals("image/jpeg", result.contentType());
        assertTrue(result.data() == original);
    }

    @Test
    void verkleinertAuchJpeg() throws IOException {
        ImageProcessor.ProcessedImage result = processor.process(image(2400, 600, "jpeg"));
        assertEquals(1200, result.width());
        assertEquals(300, result.height());
    }

    @Test
    void lehntAndereFormateAb() throws IOException {
        byte[] gif = image(10, 10, "gif");
        assertThrows(ValidationException.class, () -> processor.process(gif));
        assertThrows(ValidationException.class, () -> processor.process("kein Bild".getBytes()));
        assertThrows(ValidationException.class, () -> processor.process(new byte[0]));
    }

    @Test
    void lehntDateienUeber5MegabyteAb() {
        byte[] tooLarge = new byte[ImageProcessor.MAX_BYTES + 1];
        tooLarge[0] = (byte) 0x89;
        tooLarge[1] = 'P';
        tooLarge[2] = 'N';
        tooLarge[3] = 'G';
        assertThrows(ValidationException.class, () -> processor.process(tooLarge));
    }

    @Test
    void lehntBeschaedigteBilderAb() {
        byte[] broken = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4, 5};
        assertThrows(ValidationException.class, () -> processor.process(broken));
    }
}
