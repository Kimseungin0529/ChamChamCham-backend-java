package com.chamchamcham.config;

import com.chamchamcham.application.common.LoggingUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MDCLoggingFilter extends OncePerRequestFilter {
    private final Logger log = LoggerFactory.getLogger(MDCLoggingFilter.class);
    private static final String GUEST_MEMBER_ID = "GUEST";
    private static final List<String> MDC_KEYS = List.of("traceId", "eventId", "clientIp", "memberId");
    private static final Pattern IPV6_LITERAL_CHARS = Pattern.compile("^[0-9A-Fa-f:.%]+$");
    private static final List<String> CLIENT_IP_HEADERS = List.of("X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var path = request.getRequestURI();
        return !path.startsWith("/api/") || path.startsWith("/v3/api-docs/") || path.startsWith("/swagger-ui/") || path.equals("/swagger-ui.html");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        var clientIp = clientIp(request);
        Throwable thrown = null;
        try {
            MDC.put("traceId", UUID.randomUUID().toString());
            MDC.put("eventId", LoggingUtil.generateEventId());
            MDC.put("clientIp", clientIp);
            refreshMemberId();
            log.info("[REQ START] method={} path={} client={}", request.getMethod(), request.getRequestURI(), clientIp);
            chain.doFilter(request, response);
        } catch (Throwable ex) {
            thrown = ex;
            throw ex;
        } finally {
            refreshMemberId();
            long durationMs = System.currentTimeMillis() - startTime;
            int status = thrown != null && response.getStatus() < 400 ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : response.getStatus();
            var message = "[REQ END] method={} path={} status={} durationMs={} client={}";
            if (thrown != null && response.getStatus() < 400) {
                log.error(message, request.getMethod(), request.getRequestURI(), status, durationMs, clientIp);
            } else if (status >= 400) {
                log.warn(message, request.getMethod(), request.getRequestURI(), status, durationMs, clientIp);
            } else {
                log.info(message, request.getMethod(), request.getRequestURI(), status, durationMs, clientIp);
            }
            MDC_KEYS.forEach(MDC::remove);
        }
    }

    private void refreshMemberId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String memberId = GUEST_MEMBER_ID;
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            var principal = authentication.getPrincipal();
            if (principal != null) {
                var value = principal.toString();
                if (!value.equals("anonymousUser") && !trim(value).isEmpty()) memberId = value;
            }
        }
        MDC.put("memberId", memberId);
    }

    private String clientIp(HttpServletRequest request) {
        var remoteAddr = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddr)) return remoteAddr;
        for (String name : CLIENT_IP_HEADERS) {
            var header = request.getHeader(name);
            if (header == null) continue;
            var value = firstHeaderValue(header);
            if (value != null && parseIpLiteral(value) != null) return value;
        }
        return remoteAddr;
    }

    @Nullable
    private String firstHeaderValue(String header) {
        var value = trim(header.split(",", -1)[0]);
        return value.isEmpty() || value.equalsIgnoreCase("unknown") ? null : value;
    }

    private boolean isTrustedProxy(@Nullable String remoteAddr) {
        var address = parseIpLiteral(remoteAddr);
        return address != null && (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
            || (address.getAddress()[0] & 0xFE) == 0xFC);
    }

    @Nullable
    private InetAddress parseIpLiteral(@Nullable String value) {
        if (value == null) return null;
        var candidate = trim(value);
        if (candidate.startsWith("[") && candidate.endsWith("]")) candidate = candidate.substring(1, candidate.length() - 1);
        if (trim(candidate).isEmpty() || candidate.equalsIgnoreCase("unknown")) return null;
        try {
            var ipv4 = parseIpv4Literal(candidate);
            if (ipv4 != null) return ipv4;
            if (!candidate.contains(":") || !IPV6_LITERAL_CHARS.matcher(candidate).matches()) return null;
            return InetAddress.getByName(candidate);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    @Nullable
    private InetAddress parseIpv4Literal(String candidate) throws UnknownHostException {
        var parts = candidate.split("\\.", -1);
        if (parts.length != 4) return null;
        var bytes = new byte[4];
        for (int i = 0; i < parts.length; i++) {
            var part = parts[i];
            if (part.isEmpty() || !part.chars().allMatch(Character::isDigit)) return null;
            final int value;
            try {
                value = Integer.parseInt(part);
            } catch (NumberFormatException e) {
                return null;
            }
            if (value < 0 || value > 255) return null;
            bytes[i] = (byte) value;
        }
        return InetAddress.getByAddress(bytes);
    }

    private static String trim(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && whitespace(value.charAt(start))) start++;
        while (end > start && whitespace(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }

    private static boolean whitespace(char value) {
        return Character.isWhitespace(value) || Character.isSpaceChar(value);
    }
}
