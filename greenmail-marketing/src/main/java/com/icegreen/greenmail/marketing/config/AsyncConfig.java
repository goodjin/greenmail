package com.icegreen.greenmail.marketing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("taskExecutor") // Default name used by @Async if none specified
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // Adjust as needed
        executor.setMaxPoolSize(10); // Adjust as needed
        executor.setQueueCapacity(25); // Adjust as needed
        executor.setThreadNamePrefix("AsyncEmailSender-");
        executor.initialize();
        return executor;
    }
}
