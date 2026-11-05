package com.chamchamcham.application.config;

import com.chamchamcham.application.coaching.common.RagProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.Objects;

@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(RagProperties.class)
public class AsyncConfig {
    public static final String COACHING_TASK_EXECUTOR = "coachingTaskExecutor";
    @Bean(name = COACHING_TASK_EXECUTOR, defaultCandidate = false)
    public ThreadPoolTaskExecutor coachingTaskExecutor(RagProperties properties) {
        Objects.requireNonNull(properties, "properties");
        var execution = properties.getExecution();
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(execution.getCorePoolSize());
        executor.setMaxPoolSize(execution.getMaxPoolSize());
        executor.setQueueCapacity(execution.getQueueCapacity());
        executor.setThreadNamePrefix("coaching-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        return executor;
    }
}
