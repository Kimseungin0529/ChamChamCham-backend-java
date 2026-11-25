package com.chamchamcham.api.rag.client;

import com.chamchamcham.application.coaching.common.RagProperties;
import com.chamchamcham.config.OpenClawProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThat;

class LocalChatSelectionTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
        .withUserConfiguration(OpenClawChatModel.class)
        .withBean(RestClient.Builder.class, RestClient::builder)
        .withBean(OpenClawProperties.class, OpenClawProperties::new)
        .withBean(RagProperties.class, RagProperties::new);

    @Test
    void local_lab_does_not_route_chat_to_the_openclaw_gateway() {
        context.withPropertyValues("spring.profiles.active=rag-api-lab")
            .run(app -> assertThat(app).doesNotHaveBean(OpenClawChatModel.class));
    }

    @Test
    void normal_application_keeps_its_gateway() {
        context.withPropertyValues("spring.profiles.active=test")
            .run(app -> assertThat(app).hasSingleBean(OpenClawChatModel.class));
    }
}
