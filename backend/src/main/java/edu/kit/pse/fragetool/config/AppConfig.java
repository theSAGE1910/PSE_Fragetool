package edu.kit.pse.fragetool.config;

import edu.kit.pse.fragetool.question.ImageProcessor;
import edu.kit.pse.fragetool.run.ScoringStrategy;
import edu.kit.pse.fragetool.run.StandardScoring;
import edu.kit.pse.fragetool.session.ConsonantCodeGenerator;
import edu.kit.pse.fragetool.session.DisplayNameLengthPolicy;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Stellt die austauschbaren Bausteine bereit.
 * Tests können hier eine feste Uhr oder einen festen Zufallsgenerator einsetzen.
 */
@Configuration
public class AppConfig {

    /** Einzige Zeitquelle des Systems (/MK250/, /MK430/). */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** Erzeugt Beitrittscodes (/MK110/) und Anzeigenamen (/MK171/); einzige Bean beider Typen. */
    @Bean
    public ConsonantCodeGenerator consonantCodeGenerator() {
        return new ConsonantCodeGenerator(new SecureRandom(), new DisplayNameLengthPolicy());
    }

    @Bean
    public ImageProcessor imageProcessor() {
        return new ImageProcessor();
    }

    @Bean
    public ScoringStrategy scoringStrategy() {
        return new StandardScoring();
    }
}
