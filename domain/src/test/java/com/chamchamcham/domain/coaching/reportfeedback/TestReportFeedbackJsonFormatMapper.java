package com.chamchamcham.domain.coaching.reportfeedback;

import com.chamchamcham.domain.report.CycleReportStatistics;
import java.util.List;
import java.util.Map;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;

public class TestReportFeedbackJsonFormatMapper implements FormatMapper {
    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromString(CharSequence value, JavaType<T> javaType, WrapperOptions options) {
        if (javaType.getJavaTypeClass() == CycleReportStatistics.class) return (T) CycleReportStatistics.empty();
        if (Map.class.isAssignableFrom(javaType.getJavaTypeClass())) return (T) Map.of();
        if (List.class.isAssignableFrom(javaType.getJavaTypeClass())) return (T) List.of();
        return javaType.fromString(value);
    }
    @Override
    public <T> String toString(T value, JavaType<T> javaType, WrapperOptions options) {
        if (value instanceof Map || value instanceof CycleReportStatistics) return "{}";
        if (value instanceof List) return "[]";
        return javaType.toString(value);
    }
}
