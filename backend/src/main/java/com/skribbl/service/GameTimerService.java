package com.skribbl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Per-room scheduled tasks (choose-timeout, round-end, hint reveals, inter-round gap).
 * Every phase transition calls {@link #cancelAll(String)} first, so a timer from a
 * previous turn can never fire into the next one.
 */
@Service
public class GameTimerService {

    private static final Logger log = LoggerFactory.getLogger(GameTimerService.class);

    private final TaskScheduler scheduler;
    private final Map<String, List<ScheduledFuture<?>>> timers = new ConcurrentHashMap<>();

    public GameTimerService(@Qualifier("gameTaskScheduler") TaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void schedule(String code, int delaySeconds, Runnable task) {
        ScheduledFuture<?> future = scheduler.schedule(guard(task), Instant.now().plusSeconds(delaySeconds));
        timers.computeIfAbsent(code, k -> Collections.synchronizedList(new ArrayList<>())).add(future);
    }

    public void cancelAll(String code) {
        List<ScheduledFuture<?>> list = timers.remove(code);
        if (list != null) {
            for (ScheduledFuture<?> f : list) {
                f.cancel(false);
            }
        }
    }

    private Runnable guard(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("Scheduled game task failed", e);
            }
        };
    }
}
