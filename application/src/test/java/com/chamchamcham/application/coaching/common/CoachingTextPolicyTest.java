package com.chamchamcham.application.coaching.common;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContextKt;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

class CoachingTextPolicyTest {

    @Test
    @DisplayName("allows friendly Korean text with Korean units")
    void allows_friendly_Korean_text_with_Korean_units() throws Exception {
        assertThat(CoachingTextPolicy.INSTANCE.containsEnglishLetter("뿌리 쪽 흙을 살펴보고 물을 조금 주세요.")).isFalse();
        assertThat(CoachingTextPolicy.INSTANCE.containsEnglishLetter("수확량은 96킬로그램이었어요.")).isFalse();
    }

    @Test
    @DisplayName("rejects any English letter")
    void rejects_any_English_letter() throws Exception {
        java.util.Arrays.asList("WATERING으로 했어요.", "10kg을 썼어요.", "pH를 확인하세요.").forEach(text -> assertThat(CoachingTextPolicy.INSTANCE.INSTANCE.containsEnglishLetter(text)).isTrue());
    }

    @Test
    @DisplayName("allows Korean text including farming terms")
    void allows_Korean_text_including_farming_terms() throws Exception {
        java.util.Arrays.asList("관수", "시비", "방제", "병해충", "생육", "정식", "파종", "제초", "전정", "과습", "배수", "추대", "하엽", "토양", "수분", "살포", "살균제", "유기질").forEach(term -> assertThat(CoachingTextPolicy.INSTANCE.INSTANCE.containsEnglishLetter("" + term + "을 확인하세요.")).isFalse());
    }

    @Test
    @DisplayName("does not reject ambiguous everyday weak expression")
    void does_not_reject_ambiguous_everyday_weak_expression() throws Exception {
        assertThat(CoachingTextPolicy.INSTANCE.containsEnglishLetter("잎이 약해 보이면 먼저 살펴보세요.")).isFalse();
    }
}
