package com.chamchamcham.application.voice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class VoiceMigrationContractTest {
    private final UUID farm = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID otherFarm = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private final UUID crop = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final LocalDateTime now = LocalDateTime.of(2026, 7, 17, 9, 0, 0, 123000000);
    private ObjectMapper mapper() { return new ObjectMapper().registerModule(new KotlinModule.Builder().build()).registerModule(new JavaTimeModule()); }
    private String hash(String text) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
    @Test
    void originalPromptAndSchemaFingerprints() throws Exception {
        var farms = List.of(new FarmOption(farm, "약초농장"), new FarmOption(otherFarm, "둘째\n농장"));
        var crops = new LinkedHashMap<String, List<CropOption>>();
        crops.put(farm.toString(), List.of(new CropOption(crop, "황기"), new CropOption(crop, "중복")));
        crops.put(otherFarm.toString(), List.of());
        var pesticides = List.of(new VoicePesticideOption("코니도(바이엘)", List.of("진딧물", "총채벌레")), new VoicePesticideOption("목록\n이름", List.of()));
        assertThat(hash(VoiceSessionInstructions.build(List.of(), Map.of(), List.of(), now, 20,
            330))).isEqualTo("176f75f70222d19a7ab462eee62eb2dcc0e81ccda20f8fe9775617a96e4b3bc7");
        assertThat(hash(VoiceSessionInstructions.build(farms, crops, pesticides, now, 10,
            300))).isEqualTo("ed4cd7583ff41af6338b3807c91c6a50e4beaa19355a0bcd8c22e0d785772ee8");
        assertThat(hash(mapper().writeValueAsString(FarmingRecordVoiceToolSchema.build(List.of(),
            Map.of())))).isEqualTo("0b14333fd6ac30163af70910ff8acc6ed9cbb939861c0f4d616d84574edba035");
        assertThat(hash(mapper().writeValueAsString(FarmingRecordVoiceToolSchema.build(farms,
            crops)))).isEqualTo("77ddb45d00e5129eb5131a9d4a35a98f83400c4dacd04684e32b3c7308c70769");
    }
    @Test
    @SuppressWarnings("unchecked")
    void schemaKeepsOrderedMutableMapsAndFixedSizeRequiredFields() {
        var schema = FarmingRecordVoiceToolSchema.build(List.of(new FarmOption(farm, "농장")), Map.of());
        assertThat(schema.keySet()).containsExactly("type", "name", "description", "parameters");
        schema.put("probe", null);
        assertThat(schema).containsKey("probe");
        var parameters = (Map<String, Object>) schema.get("parameters");
        var required = (List<String>) parameters.get("required");
        required.set(0, "changed");
        assertThatThrownBy(() -> required.add("extra")).isInstanceOf(UnsupportedOperationException.class);
        var properties = (Map<String, Object>) parameters.get("properties");
        var farmProperty = (Map<String, Object>) properties.get("farmId");
        var ids = (List<String>) farmProperty.get("enum");
        ids.add("extra");
        assertThat(ids).containsExactly(farm.toString(), "extra");
        var weeding = (Map<String, Object>) properties.get("weeding");
        var weedingProperties = (Map<String, Object>) weeding.get("properties");
        assertThatThrownBy(() -> weedingProperties.put("extra", null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void candidateMissingFieldsOrderAndMutability() {
        var missing = VoiceRecordCandidateAnalyzer.missingFields(new VoiceRecordCandidate(null,null,null,null,null,null,null,null,null,null,null,null,null));
        assertThat(missing).containsExactly("farmId", "cropId", "workType", "workedAt");
        missing.add("test");
        assertThat(missing.removeLast()).isEqualTo("test");
    }
    @Test
    void jsonDefaultsAndExplicitNulls() throws Exception {
        var mapper = mapper();
        assertThat(mapper.readValue("{}", VoiceRecordCandidate.class)).isEqualTo(new VoiceRecordCandidate(null,null,null,null,null,null,null,null,null,
            null,null,null,null));
        var bound = new org.springframework.boot.context.properties.bind.Binder(
            new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(Map.of("app.voice-session.max-rounds", "7")))
            .bindOrCreate("app.voice-session", org.springframework.boot.context.properties.bind.Bindable.of(VoiceSessionProperties.class));
        assertThat(bound.getMaxRounds()).isEqualTo(7);
        assertThat(bound.getMaxDurationSeconds()).isEqualTo(330);
        var defaults = mapper.readValue("{}", VoiceSessionProperties.class);
        assertThat(defaults.getMaxRounds()).isEqualTo(20);
        assertThat(defaults.getMaxDurationSeconds()).isEqualTo(330);
        var nulls = mapper.readValue("{\"maxRounds\":null,\"maxDurationSeconds\":null}", VoiceSessionProperties.class);
        assertThat(nulls.getMaxRounds()).isZero();
        assertThat(nulls.getMaxDurationSeconds()).isZero();
    }
}
