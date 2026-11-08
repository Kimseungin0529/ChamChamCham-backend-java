package com.chamchamcham.config;

import jakarta.servlet.FilterChain;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.junit.jupiter.api.DisplayName;


@ExtendWith(OutputCaptureExtension.class)
class MDCLoggingFilterTest {

    private final MDCLoggingFilter filter = new MDCLoggingFilter();

    @Test
    @DisplayName("thrown chain logs status 500 at error level rethrows and removes own mdc keys")
    void thrownChainLogsStatus500AtErrorLevelRethrowsAndRemovesOwnMdcKeys(CapturedOutput output) throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/test/boom");
        request.setRemoteAddr("127.0.0.1");
        var response = new MockHttpServletResponse();
        var failure = new IllegalStateException("boom");
        FilterChain chain = (req, res) -> { throw failure; };

        MDC.put("unrelated", "kept");
        try {
            assertThatThrownBy(() -> {
                filter.doFilter(request, response, chain);
            }).isSameAs(failure);

            var logs = output.getOut() + output.getErr();
            assertThat(logs)
                .contains("[REQ START]")
                .contains("[REQ END]")
                .contains("status=500")
                .contains("ERROR");

            assertThat(MDC.get("traceId")).isNull();
            assertThat(MDC.get("eventId")).isNull();
            assertThat(MDC.get("clientIp")).isNull();
            assertThat(MDC.get("memberId")).isNull();
            assertThat(MDC.get("unrelated")).isEqualTo("kept");
        } finally {
            MDC.remove("unrelated");
        }
    }

    @Test
    @DisplayName("thrown chain after error response logs existing status at warn level and rethrows")
    void thrownChainAfterErrorResponseLogsExistingStatusAtWarnLevelAndRethrows(CapturedOutput output) throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/test/forbidden");
        request.setRemoteAddr("127.0.0.1");
        var response = new MockHttpServletResponse();
        var failure = new IllegalArgumentException("forbidden");
        FilterChain chain = (req, servletResponse) -> {
            ((MockHttpServletResponse) servletResponse).setStatus(403);
            throw failure;
        };

        assertThatThrownBy(() -> {
            filter.doFilter(request, response, chain);
        }).isSameAs(failure);

        var logs = output.getOut() + output.getErr();
        assertThat(logs)
            .contains("[REQ START]")
            .contains("[REQ END]")
            .contains("status=403")
            .contains("WARN")
            .doesNotContain("status=500")
            .doesNotContain("ERROR");
    }

    @Test
    @DisplayName("trusted proxy parsing preserves Unicode trim digits IPv6 and header priority")
    void trustedProxyParsingPreservesLiteralSemantics() throws Exception {
        String[][] cases = {
            {"127.0.0.1", "\u00a0٢٠٣.٠.١١٣.٧\u00a0, ignored", "198.51.100.9", "٢٠٣.٠.١١٣.٧"},
            {"fc00::1", "[2001:db8::1]", "198.51.100.9", "[2001:db8::1]"},
            {"127.0.0.1", "unknown, 203.0.113.7", "\u00a0198.51.100.9\u00a0", "198.51.100.9"},
            {"198.51.100.10", "203.0.113.7", "198.51.100.9", "198.51.100.10"}
        };
        for (String[] values : cases) {
            var request = new MockHttpServletRequest("GET", "/api/v1/test/me");
            request.setRemoteAddr(values[0]);
            request.addHeader("X-Forwarded-For", values[1]);
            request.addHeader("Proxy-Client-IP", values[2]);
            filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
                assertThat(MDC.get("clientIp")).isEqualTo(values[3]);
                assertThat(MDC.get("traceId")).isNotBlank();
            });
            assertThat(MDC.get("clientIp")).isNull();
        }
    }

}
