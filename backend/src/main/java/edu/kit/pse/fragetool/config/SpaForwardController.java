package edu.kit.pse.fragetool.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Leitet die Seiten der React-Anwendung auf index.html um.
 * So funktionieren Links wie /join/ABC123 auch beim direkten Aufruf und nach dem Neuladen.
 */
@Controller
public class SpaForwardController {

    @GetMapping({"/login", "/dozent", "/dozent/**", "/present/**", "/join/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
