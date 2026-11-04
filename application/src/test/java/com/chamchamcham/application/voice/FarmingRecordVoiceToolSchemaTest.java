package com.chamchamcham.application.voice;

import com.chamchamcham.domain.farming.PesticideAmountUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class FarmingRecordVoiceToolSchemaTest {
    private final UUID farmId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID cropId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final Map<String, Object> schema = FarmingRecordVoiceToolSchema.INSTANCE.build(
        List.of(new FarmOption(farmId, "약초농장")), Map.of(farmId.toString(), List.of(new CropOption(cropId, "황기"))));

    @SuppressWarnings("unchecked")
    private Map<String, Object> parameters() { return (Map<String, Object>) schema.get("parameters"); }
    @SuppressWarnings("unchecked")
    private Map<String, Object> properties() { return (Map<String, Object>) parameters().get("properties"); }
    @SuppressWarnings("unchecked")
    private Map<String, Object> detailProperties(String name) {
        return (Map<String, Object>) ((Map<String, Object>) properties().get(name)).get("properties");
    }
    @Test @DisplayName("수확 상세에 isLastHarvest 필드가 boolean으로 존재한다")
    @SuppressWarnings("unchecked")
    void harvestLastFlag() {
        var flag = (Map<String, Object>) detailProperties("harvest").get("isLastHarvest");
        assertThat(flag).isNotNull();
        assertThat(flag.get("type")).isEqualTo("boolean");
        assertThat((String) flag.get("description")).contains("필수");
    }
    @Test @DisplayName("memo는 30~500자 요약을 직접 작성하라고 지시한다")
    @SuppressWarnings("unchecked")
    void memoDescription() {
        var memo = (Map<String, Object>) properties().get("memo");
        assertThat((String) memo.get("description")).contains("30", "500").contains("직접 작성");
    }
    @Test @DisplayName("농약명은 목록 기반 이름 교정 규칙을 안내한다")
    @SuppressWarnings("unchecked")
    void pesticideNameDescription() {
        var name = (Map<String, Object>) detailProperties("pestControl").get("pesticideName");
        assertThat((String) name.get("description")).contains("비슷한 이름");
    }
    @Test @DisplayName("농약량 단위는 도메인 enum과 일치한다")
    @SuppressWarnings("unchecked")
    void pesticideUnits() {
        var unit = (Map<String, Object>) detailProperties("pestControl").get("pesticideAmountUnit");
        assertThat(unit.get("enum")).isEqualTo(Arrays.stream(PesticideAmountUnit.values()).map(Enum::name).toList());
    }
    @Test @DisplayName("최상위 필수 필드는 변하지 않는다")
    void requiredFields() { assertThat(parameters().get("required")).isEqualTo(List.of("farmId", "cropId", "workType", "memo")); }
    @Test @DisplayName("상세 없는 유형은 필수 4개만으로 곧바로 호출하라고 안내한다")
    void noDetailDescription() { assertThat((String) schema.get("description")).contains("ETC", "곧바로 이 도구를 호출"); }
}
