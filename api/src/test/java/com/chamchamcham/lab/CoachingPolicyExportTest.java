package com.chamchamcham.lab;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CoachingPolicyExportTest {
    @Test void serializesRealDetailAndOnlySuppliedEvidenceWithoutStartingSpring() throws Exception {
        var mapper = new ObjectMapper().findAndRegisterModules();
        var input = mapper.readTree("""
            {"context":{"member":{"memberId":"00000000-0000-0000-0000-000000000001"},
             "farm":{"farmId":"00000000-0000-0000-0000-000000000002","name":"합성","roadAddress":"가상"},
             "crop":{"cropId":"00000000-0000-0000-0000-000000000003","name":"참당귀","usePartCategory":"ROOT_BARK"},
             "record":{"recordId":"00000000-0000-0000-0000-000000000004","sourceRevision":1,
              "workedAt":"2026-08-25T10:00:00","workType":"HARVEST","memo":"잎만 수확했고 뿌리는 캐지 않았습니다.",
              "recordedWeatherCondition":"맑음","recordedTemperatureC":22,"photoCount":0,
              "detail":{"type":"HARVEST","harvestAmount":null,"amountUnknown":true,"medicinalPart":"LEAF",
               "harvestSource":"CULTIVATED","growthPeriod":4,"growthPeriodUnit":"MONTH","isLastHarvest":false}}},
             "evidence":[{"id":"leaf-doc","title":"잎 수확","page":null,"content":"잎 수확용 문단"}]}
            """);
        var result = CoachingPolicyExport.exportCase(mapper, input);
        assertThat(result.path("prompt").path("user").asText()).contains("수확한 부위: 잎", "잎만 수확했고", "잎 수확용 문단");
        assertThat(result.path("allowedCitationIds").toString()).contains("leaf-doc", "record:00000000-0000-0000-0000-000000000004");
        assertThat(result.path("documentCitationIds").size()).isEqualTo(1);
        assertThat(result.path("prompt").path("user").asText()).doesNotContain("수확량: 0", "weather:current");
    }
}
