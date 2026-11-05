package com.chamchamcham.application.policy.source.nongupez;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class JavaNetNongupEzHttpTransportTest {
    @Test
    @DisplayName("post sends JSON body and headers")
    void sendsJsonBodyAndHeaders() throws Exception {
        var captured = new AtomicReference<CapturedRequest>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/probe", exchange -> {
            var body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
            captured.set(new CapturedRequest(exchange.getRequestHeaders().getFirst("Content-Type"),
                exchange.getRequestHeaders().getFirst("Accept"), body));
            var response = "{\"ok\":true}".getBytes(UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) { output.write(response); }
        });
        server.start();
        try {
            var transport = new JavaNetNongupEzHttpTransport("http://127.0.0.1:" + server.getAddress().getPort(), 1000);
            var response = transport.post("/probe", Map.of("srchCnd", Map.of("srchBizYr", "2026")));
            assertThat(response).isEqualTo("{\"ok\":true}");
            assertThat(captured.get().contentType()).startsWith("application/json");
            assertThat(captured.get().accept()).isEqualTo("application/json");
            assertThat(captured.get().body()).contains("\"srchCnd\":{\"srchBizYr\":\"2026\"}");
        } finally { server.stop(0); }
    }
    private record CapturedRequest(String contentType, String accept, String body) {}
}
