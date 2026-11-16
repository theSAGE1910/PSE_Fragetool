package edu.kit.pse.fragetool.question;

import edu.kit.pse.fragetool.common.ValidationException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Prüft und verkleinert hochgeladene Bilder (/MK156/, /MK440/, /MK441/).
 *
 * <p>Erlaubt sind PNG und JPEG bis 5 MB. Das Format wird am Dateiinhalt erkannt, nicht am
 * Dateinamen. Bilder mit mehr als 1200 Pixeln Breite werden auf 1200 Pixel verkleinert;
 * das Seitenverhältnis bleibt erhalten. So laden auch 200 Smartphones das Bild schnell.
 */
public class ImageProcessor {

    public static final int MAX_BYTES = 5 * 1024 * 1024;
    public static final int MAX_WIDTH = 1200;

    /** Ergebnis der Verarbeitung. */
    public record ProcessedImage(String contentType, byte[] data, int width, int height) {
    }

    public ProcessedImage process(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ValidationException("Die Datei ist leer.");
        }
        if (bytes.length > MAX_BYTES) {
            throw new ValidationException("Die Datei ist zu groß (höchstens 5 MB).");
        }
        String format = detectFormat(bytes);
        BufferedImage original = read(bytes);
        if (original.getWidth() <= MAX_WIDTH) {
            return new ProcessedImage("image/" + format, bytes, original.getWidth(), original.getHeight());
        }
        int height = Math.max(1, Math.round(original.getHeight() * (MAX_WIDTH / (float) original.getWidth())));
        BufferedImage scaled = scale(original, MAX_WIDTH, height, format.equals("png"));
        return new ProcessedImage("image/" + format, write(scaled, format), MAX_WIDTH, height);
    }

    private static String detectFormat(byte[] bytes) {
        if (startsWith(bytes, 0x89, 'P', 'N', 'G')) {
            return "png";
        }
        if (startsWith(bytes, 0xFF, 0xD8, 0xFF)) {
            return "jpeg";
        }
        throw new ValidationException("Nur Bilder im Format PNG oder JPEG sind erlaubt.");
    }

    private static boolean startsWith(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((bytes[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static BufferedImage read(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new ValidationException("Die Datei ist kein gültiges Bild.");
            }
            return image;
        } catch (IOException e) {
            throw new ValidationException("Die Datei ist kein gültiges Bild.");
        }
    }

    private static BufferedImage scale(BufferedImage original, int width, int height, boolean keepTransparency) {
        int type = keepTransparency ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage scaled = new BufferedImage(width, height, type);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(original, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    private static byte[] write(BufferedImage image, String format) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(image, format, out)) {
                throw new IllegalStateException("Kein Bildschreiber für " + format);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Bild konnte nicht geschrieben werden.", e);
        }
    }
}
