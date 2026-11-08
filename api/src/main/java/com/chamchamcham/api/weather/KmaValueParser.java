package com.chamchamcham.api.weather;

/** 제공자 숫자의 nullable 파싱과 Kotlin roundToInt의 포화·NaN 의미를 유지한다. */
final class KmaValueParser {
    private KmaValueParser() {}
    static Double decimal(String value) {
        if (value == null) return null;
        try { return Double.valueOf(value); } catch (NumberFormatException exception) { return null; }
    }
    static Integer integer(String value) {
        if (value == null) return null;
        try { return Integer.valueOf(value); } catch (NumberFormatException exception) { return null; }
    }
    static Integer rounded(String value) {
        Double number=decimal(value);
        return number==null?null:roundToInt(number);
    }
    static int roundToInt(double value) {
        if (Double.isNaN(value)) throw new IllegalArgumentException("Cannot round NaN value.");
        if (value>Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (value<Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int)Math.round(value);
    }
}
