package com.chamchamcham.api.rag.client;

import com.chamchamcham.application.coaching.common.RagProperties;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.config.OpenClawProperties;
import com.chamchamcham.config.SpringAiRagConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;

class OpenClawChatModelTest {
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(OpenClawTestConfiguration.class)
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withBean(RagProperties.class, () -> new RagProperties(new RagProperties.Embedding(),
                    new RagProperties.Chat("rag-chat-model"), new RagProperties.Retrieval(), new RagProperties.Execution()))
            .withBean(OpenClawChatModel.class);

    @Test
    @DisplayName("RAG properties do not own HTTP transport timeout")
    void ragPropertiesDoNotOwnHttpTransportTimeout() throws Exception {
        assertThat(Arrays.stream(RagProperties.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)).doesNotContain("timeoutMillis");
    }

    @Test
    @DisplayName("all profiles configure OpenClaw transport timeouts")
    void allProfilesConfigureOpenClawTransportTimeouts() throws Exception {
        for (var profile : List.of("application-local.yml", "application-dev.yml", "application-prod.yml", "application-test.yml")) {
            var sources = new YamlPropertySourceLoader().load(profile, new ClassPathResource(profile));
            assertThat(sources).hasSize(1);
            var properties = sources.getFirst();
            assertThat(properties.getProperty("rag.timeout-millis"))
                    .describedAs("%s must not configure the removed RAG timeout", profile).isNull();
            assertThat(properties.getProperty("openclaw.connect-timeout-millis"))
                    .describedAs("%s OpenClaw connect timeout", profile).isEqualTo("${OPENCLAW_CONNECT_TIMEOUT_MILLIS:3000}");
            assertThat(properties.getProperty("openclaw.read-timeout-millis"))
                    .describedAs("%s OpenClaw read timeout", profile).isEqualTo("${OPENCLAW_READ_TIMEOUT_MILLIS:60000}");
        }
    }

    @Test
    @DisplayName("OpenClaw timeout defaults are positive and bounded")
    void openClawTimeoutDefaultsArePositiveAndBounded() {
        var properties = new OpenClawProperties();
        assertThat(properties.getConnectTimeoutMillis()).isEqualTo(3_000);
        assertThat(properties.getReadTimeoutMillis()).isEqualTo(60_000);
    }

    @Test
    @DisplayName("call stops waiting when OpenClaw exceeds the configured read timeout")
    void callStopsWaitingWhenOpenClawExceedsTheConfiguredReadTimeout() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            try { Thread.sleep(1_000); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            byte[] body = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"late\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            contextRunner.withPropertyValues("openclaw.base-url=http://127.0.0.1:" + server.getAddress().getPort(),
                    "openclaw.api-key=test-key", "openclaw.agent-id=agent-1", "openclaw.read-timeout-millis=100")
                    .run(context -> {
                        var model = context.getBean(OpenClawChatModel.class);
                        long started = System.nanoTime();
                        assertThatThrownBy(() -> model.call(new Prompt(new UserMessage("안녕"))))
                                .isInstanceOfSatisfying(BusinessException.class,
                                        failure -> assertThat(failure.getErrorCode()).isEqualTo(ErrorCode.RAG_CHAT_UNAVAILABLE));
                        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isLessThan(800);
                    });
        } finally { server.stop(0); }
    }

    @Test
    @DisplayName("call sends auth and agent headers to OpenClaw")
    void callSendsAuthAndAgentHeadersToOpenClaw() throws Exception {
        var authorization = new AtomicReference<String>();
        var agentId = new AtomicReference<String>();
        var requestBody = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            agentId.set(exchange.getRequestHeaders().getFirst("x-openclaw-agent-id"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"summary\\\":\\\"ok\\\"}\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            contextRunner.withPropertyValues("openclaw.base-url=http://127.0.0.1:" + server.getAddress().getPort(),
                    "openclaw.api-key=test-key", "openclaw.agent-id=agent-1")
                    .run(context -> assertThat(context.getBean(OpenClawChatModel.class)
                            .call(new Prompt(new UserMessage("안녕"))).getResult().getOutput().getText()).contains("summary"));
            assertThat(authorization.get()).isEqualTo("Bearer test-key");
            assertThat(agentId.get()).isEqualTo("agent-1");
            assertThat(objectMapper.readTree(requestBody.get())).isEqualTo(objectMapper.readTree(
                    "{\"model\":\"rag-chat-model\",\"messages\":[{\"role\":\"user\",\"content\":\"안녕\"}],\"stream\":false}"));
        } finally { server.stop(0); }
    }

    @Test
    @DisplayName("entity appends JSON schema instructions for OpenClaw")
    void entityAppendsJsonSchemaInstructionsForOpenClaw() throws Exception {
        var requestBody = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"summary\\\":\\\"ok\\\"}\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            contextRunner.withPropertyValues("openclaw.base-url=http://127.0.0.1:" + server.getAddress().getPort(),
                    "openclaw.api-key=test-key", "openclaw.agent-id=agent-1")
                    .run(context -> {
                        var model = context.getBean(OpenClawChatModel.class);
                        var chatClient = new SpringAiRagConfig().chatClient(model);
                        assertThat(chatClient.prompt().user("안녕").call().entity(NativeStructuredResponse.class))
                                .isEqualTo(new NativeStructuredResponse("ok"));
                    });
            var json = objectMapper.readTree(requestBody.get());
            assertThat(json.has("response_format")).isFalse();
            assertThat(json.at("/messages/0/content").asText()).contains("안녕", "JSON Schema", "summary", "string");
        } finally { server.stop(0); }
    }

    @Test
    void configurationBindingKeepsDefaultsAndExplicitOverrides() {
        contextRunner.run(context -> {
            var properties = context.getBean(OpenClawProperties.class);
            assertThat(properties.getBaseUrl()).isEqualTo("http://127.0.0.1:18789");
            assertThat(properties.getApiKey()).isEmpty();
            assertThat(properties.getAgentId()).isEqualTo("agri-rag-coach");
            assertThat(properties.getConnectTimeoutMillis()).isEqualTo(3_000);
            assertThat(properties.getReadTimeoutMillis()).isEqualTo(60_000);
        });
        contextRunner.withPropertyValues("openclaw.base-url=http://127.0.0.1:12345/", "openclaw.api-key=test-key",
                "openclaw.agent-id=agent-override", "openclaw.connect-timeout-millis=123", "openclaw.read-timeout-millis=456")
                .run(context -> {
                    var properties = context.getBean(OpenClawProperties.class);
                    assertThat(properties.getBaseUrl()).isEqualTo("http://127.0.0.1:12345/");
                    assertThat(properties.getApiKey()).isEqualTo("test-key");
                    assertThat(properties.getAgentId()).isEqualTo("agent-override");
                    assertThat(properties.getConnectTimeoutMillis()).isEqualTo(123);
                    assertThat(properties.getReadTimeoutMillis()).isEqualTo(456);
                });
    }

    @Test
    void providerDtoDeserializationKeepsMissingAndExplicitNullContracts() throws Exception {
        assertThat(objectMapper.readValue("{}", OpenClawCompletionResponse.class).getChoices()).isEmpty();
        assertThat(objectMapper.readValue("{}", OpenClawCompletionChoice.class).getMessage()).isNull();
        assertThat(objectMapper.readValue("{\"message\":null}", OpenClawCompletionChoice.class).getMessage()).isNull();
        assertThatThrownBy(() -> objectMapper.readValue("{\"choices\":null}", OpenClawCompletionResponse.class))
                .isInstanceOf(com.fasterxml.jackson.databind.JsonMappingException.class);
        for (String json : List.of("{}", "{\"role\":\"assistant\"}", "{\"role\":null,\"content\":\"text\"}",
                "{\"role\":\"assistant\",\"content\":null}")) {
            assertThatThrownBy(() -> objectMapper.readValue(json, OpenClawCompletionMessage.class))
                    .isInstanceOf(com.fasterxml.jackson.databind.JsonMappingException.class);
        }
        for (String stream : List.of("", ",\"stream\":null", ",\"stream\":false")) {
            assertThat(objectMapper.readValue("{\"model\":\"test\",\"messages\":[]" + stream + "}",
                    OpenClawCompletionRequest.class).getStream()).isFalse();
        }
    }

    @Test
    void missingProviderContentBecomesChatUnavailable() throws Exception {
        var responseBody = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            contextRunner.withPropertyValues("openclaw.base-url=http://127.0.0.1:" + server.getAddress().getPort(),
                    "openclaw.api-key=test-key", "openclaw.agent-id=agent-1").run(context -> {
                for (String body : List.of("null", "{}", "{\"choices\":[]}", "{\"choices\":[null]}",
                        "{\"choices\":[{}]}", "{\"choices\":[{\"message\":null}]}")) {
                    responseBody.set(body);
                    assertThatThrownBy(() -> context.getBean(OpenClawChatModel.class).call(new Prompt(new UserMessage("질문"))))
                            .as("provider response %s", body)
                            .isInstanceOfSatisfying(BusinessException.class,
                                    failure -> assertThat(failure.getErrorCode()).isEqualTo(ErrorCode.RAG_CHAT_UNAVAILABLE));
                }
            });
        } finally { server.stop(0); }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(OpenClawProperties.class)
    static class OpenClawTestConfiguration { }

    public record NativeStructuredResponse(String summary) { }
}
