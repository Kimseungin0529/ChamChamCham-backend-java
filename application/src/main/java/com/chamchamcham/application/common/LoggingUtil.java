package com.chamchamcham.application.common;

import mu.KLogger;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;

public final class LoggingUtil {
    private static final Base32 BASE32 = new Base32();

    private LoggingUtil() {}

    public static String generateEventId() {
        var buffer = ByteBuffer.allocate(8);
        buffer.putLong(System.nanoTime());
        return BASE32.encodeToString(buffer.array()).substring(2, 10);
    }

    public static String logUnexpectedError(KLogger logger, Exception ex) {
        return logUnexpectedError(logger, ex, null);
    }

    public static String logUnexpectedError(KLogger logger, Exception ex, @Nullable Object param) {
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(ex, "ex");
        var eventId = MDC.get("eventId");
        var traceId = MDC.get("traceId");
        var memberId = MDC.get("memberId");
        var clientIp = MDC.get("clientIp");
        var root = ExceptionUtils.getRootCause(ex);
        if (root == null) root = ex;
        var stack = ex.getStackTrace();
        var method = stack.length == 0 ? "Unknown"
            : stack[0].getClassName() + "." + stack[0].getMethodName() + "(L" + stack[0].getLineNumber() + ")";
        var logMsg = new StringBuilder("[Unexpected Error]\n")
            .append("eventId=").append(eventId).append(" traceId=").append(traceId)
            .append(" memberId=").append(memberId).append(" ip=").append(clientIp).append('\n')
            .append("method=").append(method).append('\n')
            .append("rootCause=").append(root).append('\n');
        if (param != null) logMsg.append("param=").append(param).append('\n');
        logger.error(logMsg.toString(), ex);
        return eventId == null ? "" : eventId;
    }

    public static String logBusinessError(KLogger logger, Throwable ex) {
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(ex, "ex");
        var eventId = MDC.get("eventId");
        logger.warn("[BusinessError][" + eventId + "] type="
            + kotlin.jvm.JvmClassMappingKt.getKotlinClass(ex.getClass()).getSimpleName());
        return eventId == null ? "" : eventId;
    }
}
