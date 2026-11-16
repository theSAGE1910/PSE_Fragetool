package edu.kit.pse.fragetool.question;

import edu.kit.pse.fragetool.auth.DozentService;
import java.io.IOException;
import java.security.Principal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Hochladen (nur Dozent) und Ausliefern (öffentlich) von Fragenbildern.
 * Die Adresse eines Bildes enthält eine zufällige ID und ändert sich bei jedem Ersetzen.
 * Browser dürfen das Bild deshalb lange zwischenspeichern.
 */
@RestController
public class ImageController {

    private final ImageService images;
    private final DozentService dozents;

    public ImageController(ImageService images, DozentService dozents) {
        this.images = images;
        this.dozents = dozents;
    }

    @PostMapping(path = "/api/questions/{questionId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(Principal principal, @PathVariable UUID questionId,
            @RequestParam("file") MultipartFile file) throws IOException {
        UUID imageId = images.attach(dozents.require(principal), questionId, file.getBytes());
        return Map.of("imageUrl", Question.imageUrl(imageId));
    }

    @DeleteMapping("/api/questions/{questionId}/image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(Principal principal, @PathVariable UUID questionId) {
        images.remove(dozents.require(principal), questionId);
    }

    @GetMapping("/api/public/images/{imageId}")
    public ResponseEntity<byte[]> download(@PathVariable UUID imageId) {
        QuestionImage image = images.load(imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(image.getData());
    }
}
