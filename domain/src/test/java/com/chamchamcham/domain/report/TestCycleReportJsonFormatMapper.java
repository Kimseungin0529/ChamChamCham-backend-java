package com.chamchamcham.domain.report;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TestCycleReportJsonFormatMapper implements FormatMapper {
    private static final ConcurrentHashMap<String, CycleReportStatistics> STATISTICS_BY_KEY = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromString(CharSequence text, JavaType<T> javaType, WrapperOptions options) {
        if (javaType.getJavaTypeClass() == CycleReportStatistics.class) {
            String key = text.toString().split("\"key\":\"")[1].split("\"")[0];
            T value = (T) STATISTICS_BY_KEY.get(key);
            if (value == null) throw new IllegalArgumentException("Missing test statistics for key " + key);
            return value;
        }
        return javaType.fromString(text);
    }

    @Override
    public <T> String toString(T value, JavaType<T> javaType, WrapperOptions options) {
        if (value instanceof CycleReportStatistics statistics) {
            String key = UUID.randomUUID().toString();
            STATISTICS_BY_KEY.put(key, statistics);
            return "{\"key\":\"" + key + "\"}";
        }
        return javaType.toString(value);
    }
}
