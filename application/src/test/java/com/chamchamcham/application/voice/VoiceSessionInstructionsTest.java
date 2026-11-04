package com.chamchamcham.application.voice;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.HarvestSource;
import org.junit.jupiter.api.DisplayName;

class VoiceSessionInstructionsTest {
    private final UUID farmId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID cropId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final LocalDateTime now = LocalDateTime.of(2026, 7, 17, 9, 0);
    private String build(List<VoicePesticideOption> pesticides) {
        return VoiceSessionInstructions.INSTANCE.build(java.util.Arrays.asList(new FarmOption(farmId, "약초농장")), Map.ofEntries(Map.entry(farmId.toString(), java.util.Arrays.asList(new CropOption(cropId, "황기")))), pesticides, now, 10, 300);
    }

    @Test
    @DisplayName("첫 안내에서 필수·선택 항목과 기본값을 먼저 알려주도록 지시한다")
    void 첫_안내에서_필수_선택_항목과_기본값을_먼저_알려주도록_지시한다() throws Exception {
        var instructions = build(List.of());
        assertThat(instructions).contains("한 번에 말해");
        assertThat(instructions).contains("현재 시각", "자동으로 입력");
    }

    @Test
    @DisplayName("대화 한도와 스스로 시간을 관리하는 규칙을 포함한다")
    void 대화_한도와_스스로_시간을_관리하는_규칙을_포함한다() throws Exception {
        var instructions = build(List.of());
        assertThat(instructions).contains("10번", "5분");
        assertThat(instructions).contains("스스로 시간을 관리");
        assertThat(instructions).doesNotContain("시간이 얼마 남지 않았습니다");
    }

    @Test
    @DisplayName("질문 전에 확인된 것과 남은 것을 요약하도록 지시한다")
    void 질문_전에_확인된_것과_남은_것을_요약하도록_지시한다() throws Exception {
        var instructions = build(List.of());
        assertThat(instructions).contains("지금까지 확인된 건");
        assertThat(instructions).contains("남은 건");
    }

    @Test
    @DisplayName("마무리 시 남은 항목은 이후 화면에서 채울 수 있다고 안내하도록 지시한다")
    void 마무리_시_남은_항목은_이후_화면에서_채울_수_있다고_안내하도록_지시한다() throws Exception {
        var instructions = build(List.of());
        assertThat(instructions).contains("이후 화면에서 직접 채울 수 있다");
    }

    @Test
    @DisplayName("농약량 단위와 마지막 수확 여부를 필수로 안내한다")
    void 농약량_단위와_마지막_수확_여부를_필수로_안내한다() throws Exception {
        var instructions = build(List.of());
        assertThat(instructions).contains("농약량과 그 단위");
        assertThat(instructions).contains("마지막 수확");
    }

    @Test
    @DisplayName("memo는 30~500자 요약을 직접 작성하라고 지시한다")
    void memo는_30_500자_요약을_직접_작성하라고_지시한다() throws Exception {
        var instructions = build(List.of());
        assertThat(instructions).contains("30자 이상 500자 이내");
        assertThat(instructions).doesNotContain("간단히 요약");
    }

    @Test
    @DisplayName("농약 목록이 있으면 이름과 대상 병해충을 포함한다")
    void 농약_목록이_있으면_이름과_대상_병해충을_포함한다() throws Exception {
        var instructions = build(java.util.Arrays.asList(new VoicePesticideOption("코니도(바이엘)", java.util.Arrays.asList("진딧물", "총채벌레"))));
        assertThat(instructions).contains("코니도(바이엘)");
        assertThat(instructions).contains("진딧물, 총채벌레");
    }

    @Test
    @DisplayName("농약 목록이 비어 있으면 농약 목록 섹션을 생략한다")
    void 농약_목록이_비어_있으면_농약_목록_섹션을_생략한다() throws Exception {
        assertThat(build(List.of())).doesNotContain("등록 농약 목록");
    }

    @Test
    @DisplayName("농지가 없으면 세션 중단 안내 문구를 유지한다")
    void 농지가_없으면_세션_중단_안내_문구를_유지한다() throws Exception {
        var instructions = VoiceSessionInstructions.INSTANCE.build(List.of(), Map.of(), List.of(), now, 10, 300);
        assertThat(instructions).contains("등록된 농지가 없습니다");
    }

    @Test
    @DisplayName("필수값을 다 모아도 사용자에게 완료를 확인한 뒤 저장하도록 지시한다")
    void 필수값을_다_모아도_사용자에게_완료를_확인한_뒤_저장하도록_지시한다() throws Exception {
        var instructions = build(List.of());
        assertThat(instructions).contains("더 기록하거나 덧붙일 내용");
        assertThat(instructions).contains("사용자가 마쳤다고");
    }

    @Test
    @DisplayName("저장 전 확인 화면에서 검토·수정 후 저장됨을 안내하도록 지시한다")
    void 저장_전_확인_화면에서_검토_수정_후_저장됨을_안내하도록_지시한다() throws Exception {
        var instructions = build(List.of());
        assertThat(instructions).contains("확인 화면에서 검토");
        assertThat(instructions).contains("자동 저장되는 것이 아님");
    }

    @Test
    @DisplayName("작업 유형을 특정할 수 없으면 ETC로 분류하도록 지시한다")
    void 작업_유형을_특정할_수_없으면_ETC로_분류하도록_지시한다() throws Exception {
        var instructions = build(List.of());
        assertThat(instructions).contains("특정할 수 없으면");
        assertThat(instructions).contains("ETC(기타)로 분류");
    }

    @Test
    @DisplayName("ETC로 분류해도 농지·작물·memo를 채워 save_farming_record를 호출하도록 지시한다")
    void ETC로_분류해도_농지_작물_memo를_채워_save_farming_record를_호출하도록_지시한다() throws Exception {
        var instructions = build(List.of());
        assertThat(instructions).contains("ETC로 분류했더라도");
        assertThat(instructions).contains("save_farming_record를 반드시 호출");
    }

    @Test
    @DisplayName("상세 없는 유형은 상세를 기다리지 말고 곧바로 저장하도록 지시한다")
    void 상세_없는_유형은_상세를_기다리지_말고_곧바로_저장하도록_지시한다() throws Exception {
        var instructions = build(List.of());
        assertThat(instructions).contains("반드시 필요한 상세가 없습니다");
        assertThat(instructions).contains("대화를 계속 끌지 마세요");
    }

    @Test
    @DisplayName("못 처리한 예외를 사용자에게 인지시키는 규칙을 포함한다")
    void 못_처리한_예외를_사용자에게_인지시키는_규칙을_포함한다() throws Exception {
        var instructions = build(List.of());
        assertThat(instructions).contains("확인 화면에서 정확한 농약");
        assertThat(instructions).contains("한 기록에는 한 작업만");
    }
}
