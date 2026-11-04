package com.chamchamcham.application.voice;

import com.chamchamcham.application.farming.FarmingRecordCommand;
import com.chamchamcham.domain.farming.FertilizerAmountUnit;
import com.chamchamcham.domain.farming.PlantingMethod;
import com.chamchamcham.domain.farming.WorkType;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.*;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.HarvestSource;
import org.junit.jupiter.api.DisplayName;

class VoiceRecordCandidateAnalyzerTest {

    @Test
    @DisplayName("모든 필드가 비어있으면 기본 필수값이 전부 누락으로 보고된다")
    void 모든_필드가_비어있으면_기본_필수값이_전부_누락으로_보고된다() throws Exception {
        var missing = VoiceRecordCandidateAnalyzer.INSTANCE.missingFields(new VoiceRecordCandidate(null, null, null, null, null, null, null, null, null, null, null, null, null));
        assertThat(missing).containsExactlyInAnyOrder("farmId", "cropId", "workType", "workedAt");
    }

    @Test
    @DisplayName("상세정보가 필요없는 workType은 detail 누락으로 보고하지 않는다")
    void 상세정보가_필요없는_workType은_detail_누락으로_보고하지_않는다() throws Exception {
        var candidate = baseCandidate(WorkType.WATERING);
        var missing = VoiceRecordCandidateAnalyzer.INSTANCE.missingFields(candidate);
        assertThat(missing).doesNotContain("detail");
    }

    @Test
    @DisplayName("기타 workType은 detail 누락으로 보고하지 않는다")
    void 기타_workType은_detail_누락으로_보고하지_않는다() throws Exception {
        var candidate = baseCandidate(WorkType.ETC);
        var missing = VoiceRecordCandidateAnalyzer.INSTANCE.missingFields(candidate);
        assertThat(missing).doesNotContain("detail");
    }

    @Test
    @DisplayName("시비는 상세정보가 없으면 detail 누락으로 보고한다")
    void 시비는_상세정보가_없으면_detail_누락으로_보고한다() throws Exception {
        var candidate = baseCandidate(WorkType.FERTILIZING);
        var missing = VoiceRecordCandidateAnalyzer.INSTANCE.missingFields(candidate);
        assertThat(missing).contains("detail");
    }

    @Test
    @DisplayName("시비는 상세정보가 채워지면 detail이 누락되지 않는다")
    void 시비는_상세정보가_채워지면_detail이_누락되지_않는다() throws Exception {
        var candidate = new VoiceRecordCandidate(UUID.randomUUID(), UUID.randomUUID(), WorkType.FERTILIZING, java.time.LocalDateTime.now(), null, null, "메모", null, null, new FarmingRecordCommand.FertilizingDetail("유박비료", new java.math.BigDecimal("10"), FertilizerAmountUnit.G, null), null, null, null);
        var missing = VoiceRecordCandidateAnalyzer.INSTANCE.missingFields(candidate);
        assertThat(missing).doesNotContain("detail");
    }

    @Test
    @DisplayName("씨앗 심기는 상세정보가 없으면 detail 누락으로 보고한다")
    void 씨앗_심기는_상세정보가_없으면_detail_누락으로_보고한다() throws Exception {
        var candidate = baseCandidate(WorkType.PLANTING);
        var missing = VoiceRecordCandidateAnalyzer.INSTANCE.missingFields(candidate);
        assertThat(missing).contains("detail");
    }

    @Test
    @DisplayName("씨앗 심기는 씨앗량이 없으면 detail 누락으로 보고한다")
    void 씨앗_심기는_씨앗량이_없으면_detail_누락으로_보고한다() throws Exception {
        var candidate = new VoiceRecordCandidate(UUID.randomUUID(), UUID.randomUUID(), WorkType.PLANTING, java.time.LocalDateTime.now(), null, null, "메모", new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEED, null, null, null, null, null), null, null, null, null, null);
        var missing = VoiceRecordCandidateAnalyzer.INSTANCE.missingFields(candidate);
        assertThat(missing).contains("detail");
    }

    @Test
    @DisplayName("씨앗 심기는 씨앗량이 채워지면 detail이 누락되지 않는다")
    void 씨앗_심기는_씨앗량이_채워지면_detail이_누락되지_않는다() throws Exception {
        var candidate = new VoiceRecordCandidate(UUID.randomUUID(), UUID.randomUUID(), WorkType.PLANTING, java.time.LocalDateTime.now(), null, null, "메모", new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEED, new java.math.BigDecimal("10"), null, null, null, null), null, null, null, null, null);
        var missing = VoiceRecordCandidateAnalyzer.INSTANCE.missingFields(candidate);
        assertThat(missing).doesNotContain("detail");
    }

    @Test
    @DisplayName("모종 심기는 모종수가 없으면 detail 누락으로 보고한다")
    void 모종_심기는_모종수가_없으면_detail_누락으로_보고한다() throws Exception {
        var candidate = new VoiceRecordCandidate(UUID.randomUUID(), UUID.randomUUID(), WorkType.PLANTING, java.time.LocalDateTime.now(), null, null, "메모", new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEEDLING, null, null, null, null, null), null, null, null, null, null);
        var missing = VoiceRecordCandidateAnalyzer.INSTANCE.missingFields(candidate);
        assertThat(missing).contains("detail");
    }

    @Test
    @DisplayName("모종 심기는 모종수가 채워지면 detail이 누락되지 않는다")
    void 모종_심기는_모종수가_채워지면_detail이_누락되지_않는다() throws Exception {
        var candidate = new VoiceRecordCandidate(UUID.randomUUID(), UUID.randomUUID(), WorkType.PLANTING, java.time.LocalDateTime.now(), null, null, "메모", new FarmingRecordCommand.PlantingDetail(PlantingMethod.SEEDLING, null, null, 10, null, null), null, null, null, null, null);
        var missing = VoiceRecordCandidateAnalyzer.INSTANCE.missingFields(candidate);
        assertThat(missing).doesNotContain("detail");
    }
    private VoiceRecordCandidate baseCandidate(WorkType workType) {
        return new VoiceRecordCandidate(UUID.randomUUID(), UUID.randomUUID(), workType, java.time.LocalDateTime.now(), null, null, "메모", null, null, null, null, null, null);
    }
}
