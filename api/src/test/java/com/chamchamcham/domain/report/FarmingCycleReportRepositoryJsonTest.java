package com.chamchamcham.domain.report;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

@DataJpaTest(
    properties = {
        "spring.datasource.url=jdbc:h2:mem:report-json-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
}
)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FarmingCycleReportRepositoryJsonTest {
    @Autowired private TestEntityManager entityManager;
    @Autowired private FarmingCycleReportRepository repository;
    private Member member;
    private Member otherMember;
    private Farm farm;
    private Crop crop;
    private FarmingRecord finalHarvest;
    private UUID otherMemberId() { return java.util.Objects.requireNonNull(otherMember.getId()); }
    private final LocalDateTime workedAt = LocalDateTime.of(2026, 6, 15, 8, 30);
    @BeforeEach
    void setUp() throws Exception {
        member = persist(new Member("member@example.com", null));
        otherMember = persist(new Member("other@example.com", null));
        farm = persist(new Farm(member, "약초농장", "강원도 평창군"));
        crop = persist(new Crop(422, "황기", CropUsePartCategory.ROOT_BARK));
        finalHarvest = persist(new FarmingRecord(null, member, farm, crop, WorkType.HARVEST, workedAt, "맑음", 24, "최종 수확", EntryMode.MANUAL, false));
    }

    @Test
    @DisplayName("typed statistics round trip through production Jackson JSON mapping")
    void typed_statistics_round_trip_through_production_Jackson_JSON_mapping() throws Exception {
        var original = completedReport(statisticsWithEveryWorkType());
        var id = java.util.Objects.requireNonNull(repository.saveAndFlush(original).getId());
        entityManager.clear();
        var loaded = repository.findById(id).orElseThrow();
        assertThat(loaded.getStatistics()).isEqualTo(original.getStatistics());
        assertThat(loaded.getSourceRevision()).isEqualTo(original.getSourceRevision());
    }

    @Test
    @DisplayName("statistics JSON saved before distribution fields remains readable")
    void statistics_JSON_saved_before_distribution_fields_remains_readable() throws Exception {
        var statistics = JsonMapper.builder().addModule(new KotlinModule.Builder().build()).build().readValue("""
{"planting":{"recordCount":1},"harvest":{"recordCount":1}}""", CycleReportStatistics.class);
        assertThat(statistics.getPlanting().getRecordCount()).isEqualTo(1);
        assertThat(statistics.getPlanting().getPlantingMethodDistribution()).isEmpty();
        assertThat(statistics.getHarvest().getRecordCount()).isEqualTo(1);
        assertThat(statistics.getHarvest().getGrowthPeriodDistribution()).isEmpty();
    }

    @Test
    @DisplayName("statistics JSON rejects explicit null for non nullable default fields")
    void statistics_JSON_rejects_explicit_null_for_non_nullable_default_fields() throws Exception {
        var mapper = JsonMapper.builder().addModule(new KotlinModule.Builder().build()).build();
        for (var json : java.util.Arrays.asList("""
{"planting":null}""", """
{"planting":{"weatherDistribution":null}}""", """
{"harvest":{"amountCoverage":null}}""")) {
            assertThatThrownBy(() -> {
                    mapper.readValue(json, CycleReportStatistics.class);
            }).isInstanceOf(com.fasterxml.jackson.databind.JsonMappingException.class);
        };
    }

    @Test
    @DisplayName("statistics JSON preserves empty arrays and nullable fields")
    void statistics_JSON_preserves_empty_arrays_and_nullable_fields() throws Exception {
        var statistics = JsonMapper.builder().addModule(new KotlinModule.Builder().build()).build().readValue("""
{"planting":{"weatherDistribution":[],"firstWorkedOn":null},"harvest":{"totalAmountKg":null}}""", CycleReportStatistics.class);
        assertThat(statistics.getPlanting().getWeatherDistribution()).isEmpty();
        assertThat(statistics.getPlanting().getFirstWorkedOn()).isNull();
        assertThat(statistics.getHarvest().getTotalAmountKg()).isNull();
        assertThat(statistics.getWatering()).isEqualTo(WateringStatistics.empty());
    }

    @Test
    @DisplayName("member scoped lookup does not expose another members report")
    void member_scoped_lookup_does_not_expose_another_members_report() throws Exception {
        var report = repository.saveAndFlush(completedReport(CycleReportStatistics.empty()));
        assertThat(repository.findByIdAndMember_Id(java.util.Objects.requireNonNull(report.getId()), otherMemberId()))
            .isNull();
    }

    private FarmingCycleReport completedReport(CycleReportStatistics statistics) throws Exception {
        return withTimestamps(FarmingCycleReport.create(member,
            farm,
            crop,
            new FarmingCycleReportProjection(FarmingCycleReportStatus.COMPLETED,
            workedAt.minusMonths(6),
            workedAt,
            FarmingCycleStartBasis.AFTER_PREVIOUS_FINAL_HARVEST,
            finalHarvest,
            1,
            statistics)),
            workedAt);
    }

    private CycleReportStatistics statisticsWithEveryWorkType() {
        return new CycleReportStatistics(new PlantingStatistics(1,
            LocalDate.of(2026,
            1,
            1),
            LocalDate.of(2026,
            1,
            1),
            1,
            null,
            1,
            null,
            java.util.Collections.emptyList(),
            null,
            java.util.Arrays.asList(distribution("SEED",
            "씨앗 심기",
            1)),
            java.util.Arrays.asList(new PropagationStatistics("SEED",
            "종자",
            1,
            new BigDecimal("100"),
            new BigDecimal("10"),
            "g",
            new Coverage(1,
            1)))),
            new WateringStatistics(2,
            null,
            null,
            0,
            null,
            0,
            null,
            java.util.Collections.emptyList(),
            null,
            java.util.Arrays.asList(distribution("ENOUGH",
            "충분",
            2)),
            java.util.Arrays.asList(distribution("SPRINKLE",
            "살수",
            2))),
            new FertilizingStatistics(3,
            null,
            null,
            0,
            null,
            0,
            null,
            java.util.Collections.emptyList(),
            null,
            new BigDecimal("3"),
            new BigDecimal("1"),
            new Coverage(3,
            3),
            java.util.Collections.emptyList(),
            java.util.Arrays.asList(new MaterialCategoryStatistics("ORGANIC_FERTILIZER",
            "유기질비료",
            3,
            new BigDecimal("100"),
            new BigDecimal("3"),
            new BigDecimal("100"))),
            java.util.Arrays.asList(distribution("SOIL",
            "토양시비",
            3)),
            java.util.Arrays.asList(new CategoryMethodStatistics("ORGANIC_FERTILIZER",
            "유기질비료",
            "SOIL",
            "토양시비",
            3,
            new BigDecimal("100")))),
            new PestControlStatistics(4,
            null,
            null,
            0,
            null,
            0,
            null,
            java.util.Arrays.asList(distribution("SUNNY",
            "맑음",
            4)),
            null,
            java.util.Arrays.asList(distribution("FUNGICIDE",
            "살균제",
            4)),
            java.util.Arrays.asList(new AmountByUnit("ml",
            new BigDecimal("40"),
            new Coverage(4,
            4))),
            java.util.Arrays.asList(new CategoryAmountByUnit("FUNGICIDE",
            "살균제",
            "ml",
            4,
            new BigDecimal("40"),
            new Coverage(4,
            4))),
            new BigDecimal("20"),
            new Coverage(4,
            4),
            java.util.Arrays.asList(new TargetCount("진딧물",
            4))),
            new WeedingStatistics(5,
            null,
            null,
            0,
            null,
            0,
            null,
            java.util.Collections.emptyList(),
            null,
            java.util.Arrays.asList(distribution("HAND",
            "손제초",
            5))),
            new CommonOnlyStatistics(6,
            null,
            null,
            0,
            null,
            0,
            null,
            java.util.Collections.emptyList(),
            null),
            new HarvestStatistics(7,
            null,
            null,
            0,
            null,
            0,
            null,
            java.util.Collections.emptyList(),
            null,
            new BigDecimal("70"),
            new BigDecimal("10"),
            new Coverage(7,
            7),
            LocalDate.of(2026,
            6,
            1),
            LocalDate.of(2026,
            6,
            15),
            java.util.Arrays.asList(new HarvestPartStatistics("ROOT_BARK",
            "뿌리·껍질",
            7,
            new BigDecimal("100"),
            new BigDecimal("70"),
            new BigDecimal("100"),
            new Coverage(7,
            7))),
            6,
            new GrowthPeriodRange(5,
            7),
            java.util.Arrays.asList(distribution("6",
            "6개월",
            7))),
            new CommonOnlyStatistics(8,
            null,
            null,
            0,
            null,
            0,
            null,
            java.util.Collections.emptyList(),
            null));
    }

    private CountDistribution distribution(String code, String label, int count) {
        return new CountDistribution(code, label, count, new BigDecimal("100"));
    }

    private <T extends BaseTimeEntity> T persist(T entity) throws Exception {
        setTimestamps(entity, workedAt);
        return entityManager.persistAndFlush(entity);
    }

    private void setTimestamps(BaseTimeEntity entity, LocalDateTime createdAt) throws Exception {
        for (var name : List.of("createdAt", "updatedAt")) {
            var field = BaseTimeEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(entity, createdAt);
        }
    }

    private <T extends BaseTimeEntity> T withTimestamps(T entity, LocalDateTime createdAt) throws Exception {
        setTimestamps(entity, createdAt);
        return entity;
    }
}
