package com.chamchamcham.application.config;

import com.chamchamcham.application.coaching.common.RagProperties;
import com.chamchamcham.application.policy.sync.PolicySyncAsyncRunner;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

class AsyncConfigTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class)).withUserConfiguration(AsyncConfig.class);

    @Test
    @DisplayName("coaching execution defaults are bounded")
    void coaching_execution_defaults_are_bounded() throws Exception {
        var execution = new RagProperties().getExecution();
        assertThat(execution.getCorePoolSize()).isEqualTo(2);
        assertThat(execution.getMaxPoolSize()).isEqualTo(4);
        assertThat(execution.getQueueCapacity()).isEqualTo(32);
        assertThat(execution.getPendingTimeoutMillis()).isEqualTo(240_000L);
        assertThat(execution.getScanIntervalMillis()).isEqualTo(60_000L);
    }

    @Test
    @DisplayName("coaching executor is bounded and discards saturated work for timeout recovery")
    void coaching_executor_is_bounded_and_discards_saturated_work_for_timeout_recovery() throws Exception {
        contextRunner.run(context -> {
        assertThat(context).hasBean("coachingTaskExecutor");
        var executor = context.getBean("coachingTaskExecutor", ThreadPoolTaskExecutor.class);
        assertThat(executor.getCorePoolSize()).isEqualTo(2);
        assertThat(executor.getMaxPoolSize()).isEqualTo(4);
        assertThat(executor.getQueueCapacity()).isEqualTo(32);
        assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler()).isInstanceOf(ThreadPoolExecutor.DiscardPolicy.class);
        });
    }

    @Test
    @DisplayName("unqualified async work keeps the Boot default executor")
    void unqualified_async_work_keeps_the_Boot_default_executor() throws Exception {
        contextRunner.run(context -> {
        assertThat(context).hasBean("applicationTaskExecutor");
        assertThat(context).hasBean("coachingTaskExecutor");
        var defaultExecutor = context.getBean("applicationTaskExecutor");
        var coachingExecutor = context.getBean("coachingTaskExecutor");
        var asyncConfigurer = context.getBean(AsyncConfigurer.class);
        assertThat(defaultExecutor).isNotSameAs(coachingExecutor);
        assertThat(asyncConfigurer.getAsyncExecutor()).isSameAs(defaultExecutor);
        });
        var policyRun = PolicySyncAsyncRunner.class.getDeclaredMethod("run", java.util.UUID.class);
        assertThat(policyRun.getAnnotation(org.springframework.scheduling.annotation.Async.class).value()).isEmpty();
    }
    @Test
    void configuredExecutionPropertiesBindIntoExecutor() {
        contextRunner.withPropertyValues("rag.execution.core-pool-size=3",
            "rag.execution.max-pool-size=6", "rag.execution.queue-capacity=17")
            .run(context -> {
                var properties = context.getBean(RagProperties.class);
                assertThat(properties.getExecution().getCorePoolSize()).isEqualTo(3);
                assertThat(properties.getExecution().getMaxPoolSize()).isEqualTo(6);
                assertThat(properties.getExecution().getQueueCapacity()).isEqualTo(17);
                var executor = context.getBean("coachingTaskExecutor", ThreadPoolTaskExecutor.class);
                assertThat(executor.getCorePoolSize()).isEqualTo(3);
                assertThat(executor.getMaxPoolSize()).isEqualTo(6);
                assertThat(executor.getQueueCapacity()).isEqualTo(17);
            });
    }
}
