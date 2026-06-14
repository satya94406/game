package com.skribbl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class AppConfig {

    /**
     * Drives round countdowns, hint reveals, and the gap between rounds.
     * {@code removeOnCancelPolicy} keeps cancelled timers from piling up when a
     * round ends early (everyone guessed) or the drawer disconnects.
     */
    @Bean
    public TaskScheduler gameTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("game-timer-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        return scheduler;
    }
}
