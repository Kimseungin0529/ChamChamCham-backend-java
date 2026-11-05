package com.chamchamcham.application.coaching.common;

public final class CoachingTextPolicy {
    private CoachingTextPolicy() {}
    private static final String PROMPT_INSTRUCTIONS = "농부에게 보여 줄 모든 문장은 친근한 존댓말로 작성한다.\n영어 알파벳, 내부 enum, 영어 필드명과 영어 단위를 쓰지 않는다.\n관수는 물 주기, 시비는 거름 주기처럼 일상에서 쓰는 말로 풀어 쓴다.\n공식 기술 문서의 어려운 표현을 그대로 복사하지 말고 뜻을 쉬운 말로 설명한다.\n농약 때문에 생긴 피해는 약해라고 줄이지 말고 약 때문에 생긴 피해라고 쓴다.";
    public static String getPromptInstructions() { return PROMPT_INSTRUCTIONS; }
    public static boolean containsEnglishLetter(String text) { return ENGLISH_LETTER.matcher(java.util.Objects.requireNonNull(text, "text")).find(); }
    private static final java.util.regex.Pattern ENGLISH_LETTER = java.util.regex.Pattern.compile("[A-Za-z]");
}
