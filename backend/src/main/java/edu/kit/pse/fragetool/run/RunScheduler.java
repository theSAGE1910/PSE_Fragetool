package edu.kit.pse.fragetool.run;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

/**
 * Zeitgeber: beendet jede Frage automatisch zu ihrem Endzeitpunkt (/MK250/).
 * Nach einem Neustart des Servers werden noch laufende Fragen erneut eingeplant.
 */
@Component
public class RunScheduler implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(RunScheduler.class);
    private static final int TIMER_THREADS = 2;

    private final RunCloser closer;
    private final QuestionRunRepository runs;
    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    private final Map<UUID, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public RunScheduler(RunCloser closer, QuestionRunRepository runs) {
        this.closer = closer;
        this.runs = runs;
        scheduler.setPoolSize(TIMER_THREADS);
        scheduler.setThreadNamePrefix("run-timer-");
        scheduler.initialize();
    }

    public void schedule(UUID runId, Instant endsAt) {
        tasks.put(runId, scheduler.schedule(() -> closeSafely(runId), endsAt));
    }

    public void cancel(UUID runId) {
        ScheduledFuture<?> task = tasks.remove(runId);
        if (task != null) {
            task.cancel(false);
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        for (QuestionRun run : runs.findByStatus(RunStatus.RUNNING)) {
            schedule(run.getId(), run.getEndsAt());
        }
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdown();
    }

    private void closeSafely(UUID runId) {
        tasks.remove(runId);
        try {
            closer.close(runId);
        } catch (RuntimeException e) {
            LOG.error("Frage {} konnte nicht automatisch beendet werden.", runId, e);
        }
    }
}
