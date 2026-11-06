package com.chamchamcham.config;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import static org.assertj.core.api.Assertions.assertThat;

class RagExecutionProfileConfigurationTest {
    @Test
    @DisplayName("all profiles expose bounded coaching execution settings")
    void allProfilesExposeBoundedCoachingExecutionSettings() throws Exception {
        for (String profile : List.of("application-local.yml", "application-dev.yml", "application-prod.yml", "application-test.yml")) {
            var sources = new YamlPropertySourceLoader().load(profile, new ClassPathResource(profile));
            assertThat(sources).hasSize(1);
            var properties = sources.getFirst();
            assertThat(properties.getProperty("rag.execution.core-pool-size"))
                    .describedAs("%s coaching core pool size", profile).isEqualTo("${RAG_EXECUTOR_CORE_SIZE:2}");
            assertThat(properties.getProperty("rag.execution.max-pool-size"))
                    .describedAs("%s coaching max pool size", profile).isEqualTo("${RAG_EXECUTOR_MAX_SIZE:4}");
            assertThat(properties.getProperty("rag.execution.queue-capacity"))
                    .describedAs("%s coaching queue capacity", profile).isEqualTo("${RAG_EXECUTOR_QUEUE_CAPACITY:32}");
            assertThat(properties.getProperty("rag.execution.pending-timeout-millis"))
                    .describedAs("%s coaching pending timeout", profile).isEqualTo("${RAG_PENDING_TIMEOUT_MILLIS:240000}");
            assertThat(properties.getProperty("rag.execution.scan-interval-millis"))
                    .describedAs("%s coaching scan interval", profile).isEqualTo("${RAG_PENDING_SCAN_INTERVAL_MILLIS:60000}");
        }
    }
}
