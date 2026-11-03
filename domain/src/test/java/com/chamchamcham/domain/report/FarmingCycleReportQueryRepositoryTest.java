package com.chamchamcham.domain.report;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Objects;
import com.chamchamcham.domain.member.MemberStatus;
import com.chamchamcham.domain.farm.FarmDataSource;

@DataJpaTest
@ActiveProfiles("test")
@Import(FarmingCycleReportQueryRepositoryImpl.class)
@TestPropertySource(
    properties = {
        "spring.jpa.properties.hibernate.type.json_format_mapper=com.chamchamcham.domain.report.TestCycleReportJsonFormatMapper"
    }
)
class FarmingCycleReportQueryRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private FarmingCycleReportQueryRepository repository;
    @Autowired
    private FarmingCycleReportRepository reportRepository;

    private Member member;
    private Member otherMember;
    private Farm farm;
    private Crop crop;

    private UUID memberId() { return Objects.requireNonNull(member.getId()); }
    private UUID farmId() { return Objects.requireNonNull(farm.getId()); }
    private UUID cropId() { return Objects.requireNonNull(crop.getId()); }

    private final LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 9, 0);

    @BeforeEach
    void setUp() {
        member = persist(new Member(null, "member@example.com", null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null), baseTime);
        otherMember = persist(new Member(null, "other@example.com", null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null), baseTime);
        farm = persist(new Farm(null, member, "약초농장", "서울시 강남구", null, null, null, null, null, null, false, new ArrayList<>(), new FarmDataSource(null, null, null, null)), baseTime);
        crop = persist(
            new Crop(null, 422, "황기", CropUsePartCategory.ROOT_BARK),
            baseTime);
    }

    @Test
    void completedPageExcludesActiveSupersededAndOtherMemberReports() {
        persistCompleted(member, null, this.crop, day(30), CycleReportStatistics.empty());
        persistCompleted(member, null, this.crop, day(20), CycleReportStatistics.empty());
        persistActive(day(50), CycleReportStatistics.empty());
        persistSuperseded(day(10));
        persistCompleted(otherMember, null, this.crop, day(10), CycleReportStatistics.empty());
        entityManager.flush();
        entityManager.clear();

        var result = repository.searchCompleted(condition(this.farmId(), this.cropId(), null, 10));

        assertThat(result.rows().stream().map(it -> it.getEndsAt()).toList())
            .containsExactly(day(30), day(20));
    }

    @Test
    void reportCanBeLockedOnlyByItsIdAndOwningMember() {
        var report = persistCompleted(member, null, this.crop, day(30), CycleReportStatistics.empty());
        entityManager.flush();
        entityManager.clear();

        var locked = reportRepository.findByIdAndMemberIdForUpdate(Objects.requireNonNull(report.getId()), memberId());
        var outsideOwner = reportRepository.findByIdAndMemberIdForUpdate(
            Objects.requireNonNull(report.getId()),
            Objects.requireNonNull(otherMember.getId()));

        assertThat(locked.getId()).isEqualTo(report.getId());
        assertThat(outsideOwner).isNull();
    }

    @Test
    void completedListAcceptsMemberWideFarmOnlyCropOnlyAndCombinedFilters() {
        var otherFarm = persist(
            new Farm(null, member, "다른 약초농장", "서울시 마포구", null, null, null, null, null, null, false, new ArrayList<>(), new FarmDataSource(null, null, null, null)),
            baseTime);
        var otherCrop = persist(
            new Crop(null, 423, "감초", CropUsePartCategory.ROOT_BARK),
            baseTime);
        var newestAcrossScopes = persistCompleted(member, farm, crop, day(40), CycleReportStatistics.empty());
        var olderAcrossScopes = persistCompleted(member, otherFarm, otherCrop, day(30), CycleReportStatistics.empty());
        entityManager.flush();
        entityManager.clear();

        var all = repository.searchCompleted(condition(null, null, null, 20));
        assertThat(all.rows().stream().map(it -> it.getId()).toList())
            .containsExactly(newestAcrossScopes.getId(), olderAcrossScopes.getId());

        var byFarm = repository.searchCompleted(condition(farmId(), null, null, 20));
        assertThat(byFarm.rows().stream().map(it -> it.getId()).toList()).containsExactly(newestAcrossScopes.getId());
        assertThat(byFarm.rows()).allMatch(it -> it.getFarm().getId().equals(farmId()));

        var byCrop = repository.searchCompleted(condition(null, cropId(), null, 20));
        assertThat(byCrop.rows().stream().map(it -> it.getId()).toList()).containsExactly(newestAcrossScopes.getId());
        assertThat(byCrop.rows()).allMatch(it -> it.getCrop().getId().equals(cropId()));

        var combined = repository.searchCompleted(condition(farmId(), cropId(), null, 20));
        assertThat(combined.rows().stream().map(it -> it.getId()).toList()).containsExactly(newestAcrossScopes.getId());

        var multiple = repository.searchCompleted(
            new FarmingCycleReportQueryRepository.SearchCondition(memberId(), Set.of(farmId(), Objects.requireNonNull(otherFarm.getId())), Set.of(cropId(), Objects.requireNonNull(otherCrop.getId())), null, 20));
        assertThat(multiple.rows().stream().map(it -> it.getId()).toList())
            .containsExactly(newestAcrossScopes.getId(), olderAcrossScopes.getId());
    }

    @Test
    void completedCursorResolvesEqualEndTimesWithFinalHarvestId() {
        for (int index = 0; index < 3; index++) {
            persistCompleted(member, null, this.crop, day(30), CycleReportStatistics.empty());
        }
        entityManager.flush();
        entityManager.clear();

        var ordered = repository.searchCompleted(condition(this.farmId(), this.cropId(), null, 3)).rows();
        var cursorReport = ordered.get(1);
        var expectedReport = ordered.get(2);

        var result = repository.searchCompleted(
            condition(this.farmId(), this.cropId(), new FarmingCycleReportQueryRepository.Cursor(day(30), Objects.requireNonNull(cursorReport.getFinalHarvestRecord().getId())), 10));

        assertThat(result.rows().stream().map(it -> it.getId()).toList())
            .containsExactly(expectedReport.getId());
    }

    @Test
    void previousCompletedReportUsesTheSameBoundaryOrdering() {
        for (int index = 0; index < 2; index++) {
            persistCompleted(member, null, this.crop, day(30), CycleReportStatistics.empty());
        }
        entityManager.flush();
        entityManager.clear();

        var ordered = repository.searchCompleted(condition(this.farmId(), this.cropId(), null, 2)).rows();
        var selected = ordered.get(0);
        var previous = ordered.get(1);

        assertThat(
            repository.findPreviousCompleted(
                memberId(),
                farmId(),
                cropId(),
                Objects.requireNonNull(selected.getEndsAt()),
                Objects.requireNonNull(selected.getFinalHarvestRecord().getId())).getId()).isEqualTo(previous.getId());
    }

    @Test
    void workItemProjectionIncludesActiveAndCompletedPositiveStatisticsAndOrdersReportCards() {
        var otherFarm = persist(
            new Farm(null, member, "다른 약초농장", "서울시 마포구", null, null, null, null, null, null, false, new ArrayList<>(), new FarmDataSource(null, null, null, null)),
            baseTime);
        var otherCrop = persist(
            new Crop(null, 423, "감초", CropUsePartCategory.ROOT_BARK),
            baseTime);
        var latest = persistCompleted(member, null, this.crop, day(40), new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(2, null, day(38).toLocalDate(), 0, null, 0, null, List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), new HarvestStatistics(1, null, day(40).toLocalDate(), 0, null, 0, null, List.of(), null, null, null, new Coverage(0, 0), null, null, List.of(), null, null, List.of()), CommonOnlyStatistics.empty()));
        var sameEndOtherScope = persistCompleted(member, otherFarm, otherCrop, day(40), new CycleReportStatistics(new PlantingStatistics(3, null, day(15).toLocalDate(), 0, null, 0, null, List.of(), null, List.of(), List.of()), WateringStatistics.empty(), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), new CommonOnlyStatistics(0, null, day(16).toLocalDate(), 0, null, 0, null, List.of(), null)));
        persistCompleted(otherMember, null, this.crop, day(50), new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(9, null, day(50).toLocalDate(), 0, null, 0, null, List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty()));
        var active = persistActive(day(10), new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(4, null, day(20).toLocalDate(), 0, null, 0, null, List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty()));
        persistSuperseded(day(60));
        entityManager.flush();
        entityManager.clear();

        var all = repository.searchWorkItems(
            workCondition(null, null, null, List.of(), null, 20)).rows();

        assertThat(all.stream().map(it -> Map.entry(it.reportId(), it.workType())).toList())
            .containsExactly(
                Map.entry(Objects.requireNonNull(latest.getId()), WorkType.HARVEST),
                Map.entry(Objects.requireNonNull(latest.getId()), WorkType.WATERING),
                Map.entry(Objects.requireNonNull(active.getId()), WorkType.WATERING),
                Map.entry(Objects.requireNonNull(sameEndOtherScope.getId()), WorkType.PLANTING));
        var activeItem = all.stream().filter(it -> it.reportId().equals(active.getId())).findFirst().orElseThrow();
        assertThat(activeItem.status()).isEqualTo(FarmingCycleReportStatus.ACTIVE);
        assertThat(activeItem.endsAt()).isNull();
        assertThat(activeItem.finalHarvestRecordId()).isNull();
        assertThat(all.stream().map(it -> it.recordCount()).toList()).containsExactlyInAnyOrder(2, 1, 3, 4);
        assertThat(all.stream().map(it -> it.lastWorkedOn()).toList())
            .containsExactlyInAnyOrder(
                day(38).toLocalDate(),
                day(40).toLocalDate(),
                day(15).toLocalDate(),
                day(20).toLocalDate());

        assertThat(repository.searchWorkItems(workCondition(farmId(), null, null, List.of(), null, 20)).rows().stream().map(it -> it.reportId()).toList())
            .containsOnly(latest.getId(), active.getId());
        assertThat(repository.searchWorkItems(workCondition(null, cropId(), null, List.of(), null, 20)).rows().stream().map(it -> it.reportId()).toList())
            .containsOnly(latest.getId(), active.getId());
        assertThat(
            repository.searchWorkItems(
                workCondition(null, null, null, List.of(WorkType.PLANTING, WorkType.WATERING), null, 20)).rows().stream().map(it -> Map.entry(it.reportId(), it.workType())).toList()).containsExactly(
            Map.entry(Objects.requireNonNull(latest.getId()), WorkType.WATERING),
            Map.entry(Objects.requireNonNull(active.getId()), WorkType.WATERING),
            Map.entry(Objects.requireNonNull(sameEndOtherScope.getId()), WorkType.PLANTING));

        var multiple = repository.searchWorkItems(
            new FarmingCycleReportQueryRepository.WorkItemSearchCondition(memberId(), Set.of(farmId(), Objects.requireNonNull(otherFarm.getId())), Set.of(cropId(), Objects.requireNonNull(otherCrop.getId())), List.of(), null, 20)).rows();
        assertThat(multiple.stream().map(it -> it.reportId()).toList())
            .containsOnly(latest.getId(), active.getId(), sameEndOtherScope.getId());
    }

    @Test
    void workItemsWithoutALastWorkedDateAreOrderedLast() {
        var recentWithoutWorkedOn = persistCompleted(member, null, this.crop, day(70), new CycleReportStatistics(PlantingStatistics.empty(), WateringStatistics.empty(), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), new CommonOnlyStatistics(1, null, null, 0, null, 0, null, List.of(), null)));
        var olderWithWorkedOn = persistCompleted(member, null, this.crop, day(30), new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(1, null, day(20).toLocalDate(), 0, null, 0, null, List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty()));
        entityManager.flush();
        entityManager.clear();

        var items = repository.searchWorkItems(workCondition(this.farmId(), this.cropId(), null, List.of(), null, 20)).rows();

        assertThat(items.stream().map(it -> Map.entry(it.reportId(), it.workType())).toList()).containsExactly(
            Map.entry(Objects.requireNonNull(olderWithWorkedOn.getId()), WorkType.WATERING),
            Map.entry(Objects.requireNonNull(recentWithoutWorkedOn.getId()), WorkType.ETC));
    }

    @Test
    void workItemsOnTheSameDateKeepActiveStatusFirst() {
        var tiedActive = persistActive(day(5), new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(1, null, day(25).toLocalDate(), 0, null, 0, null, List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty()));
        var tiedCompleted = persistCompleted(member, null, this.crop, day(30), new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(1, null, day(25).toLocalDate(), 0, null, 0, null, List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty()));
        entityManager.flush();
        entityManager.clear();

        var items = repository.searchWorkItems(workCondition(this.farmId(), this.cropId(), null, List.of(), null, 20)).rows();

        assertThat(items.stream().map(it -> it.reportId()).toList()).containsExactly(
            Objects.requireNonNull(tiedActive.getId()),
            Objects.requireNonNull(tiedCompleted.getId()));
    }

    @Test
    void workItemCursorCrossesActiveWorkTypesAndCompletedReportsWithoutOverlap() {
        var active = persistActive(day(40), new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(2, null, day(41).toLocalDate(), 0, null, 0, null, List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), new HarvestStatistics(1, null, day(42).toLocalDate(), 0, null, 0, null, List.of(), null, null, null, new Coverage(0, 0), null, null, List.of(), null, null, List.of()), CommonOnlyStatistics.empty()));
        var completed = persistCompleted(member, null, this.crop, day(30), new CycleReportStatistics(PlantingStatistics.empty(), new WateringStatistics(1, null, day(20).toLocalDate(), 0, null, 0, null, List.of(), null, List.of(), List.of()), FertilizingStatistics.empty(), PestControlStatistics.empty(), WeedingStatistics.empty(), CommonOnlyStatistics.empty(), HarvestStatistics.empty(), CommonOnlyStatistics.empty()));
        entityManager.flush();
        entityManager.clear();

        var firstPage = repository.searchWorkItems(workCondition(this.farmId(), this.cropId(), null, List.of(), null, 1)).rows();
        var first = firstPage.getFirst();
        var secondPage = repository.searchWorkItems(
            workCondition(this.farmId(), this.cropId(), null, List.of(), new FarmingCycleReportQueryRepository.WorkItemCursor(first.lastWorkedOn(), first.status(), first.getSortAt(), first.reportId(), first.workType()), 20)).rows();

        assertThat(first.reportId()).isEqualTo(active.getId());
        assertThat(first.workType()).isEqualTo(WorkType.HARVEST);
        assertThat(secondPage.stream().map(it -> Map.entry(it.reportId(), it.workType())).toList())
            .containsExactly(
                Map.entry(Objects.requireNonNull(active.getId()), WorkType.WATERING),
                Map.entry(Objects.requireNonNull(completed.getId()), WorkType.WATERING));
        assertThat(java.util.stream.Stream.concat(firstPage.stream(), secondPage.stream()).map(it -> Map.entry(it.reportId(), it.workType())).toList()).doesNotHaveDuplicates();
    }

    private FarmingCycleReport persistActive(LocalDateTime startsAt, CycleReportStatistics statistics) {
        return persistReport(member, null, this.crop, new FarmingCycleReportProjection(FarmingCycleReportStatus.ACTIVE, startsAt, null, FarmingCycleStartBasis.AFTER_PREVIOUS_FINAL_HARVEST, null, 1, statistics));
    }

    private FarmingCycleReport persistCompleted(Member owner, Farm farm, Crop crop, LocalDateTime endedAt, CycleReportStatistics statistics) {
        var reportFarm = farm != null ? farm : owner == member ? this.farm : persist(
            new Farm(null, owner, "다른 농장", "서울시 서초구", null, null, null, null, null, null, false, new ArrayList<>(), new FarmDataSource(null, null, null, null)),
            baseTime);
        var finalHarvest = persistFinalHarvest(owner, reportFarm, crop, endedAt);
        return persistReport(owner, reportFarm, crop, new FarmingCycleReportProjection(FarmingCycleReportStatus.COMPLETED, endedAt.minusDays(10), endedAt, FarmingCycleStartBasis.FIRST_RECORD, finalHarvest, 1, statistics));
    }

    private FarmingCycleReport persistSuperseded(LocalDateTime endedAt) {
        var report = persistCompleted(member, null, this.crop, endedAt, CycleReportStatistics.empty());
        report.supersede();
        return report;
    }

    private FarmingCycleReport persistReport(Member owner, Farm farm, Crop crop, FarmingCycleReportProjection projection) {
        var reportFarm = farm != null ? farm : owner == member ? this.farm : persist(
            new Farm(null, owner, "다른 농장", "서울시 서초구", null, null, null, null, null, null, false, new ArrayList<>(), new FarmDataSource(null, null, null, null)),
            baseTime);
        return persist(FarmingCycleReport.create(owner, reportFarm, crop, projection), baseTime);
    }

    private FarmingRecord persistFinalHarvest(Member owner, Farm farm, Crop crop, LocalDateTime workedAt) {
        var recordFarm = farm != null ? farm : owner == member ? this.farm : persist(
            new Farm(null, owner, "다른 수확 농장", "서울시 송파구", null, null, null, null, null, null, false, new ArrayList<>(), new FarmDataSource(null, null, null, null)),
            baseTime);
        return persist(
            new FarmingRecord(null, owner, recordFarm, crop, WorkType.HARVEST, workedAt, "맑음", 20, "최종 수확", com.chamchamcham.domain.farming.EntryMode.MANUAL, false),
            baseTime);
    }

    private FarmingCycleReportQueryRepository.SearchCondition condition(UUID farmId, UUID cropId, FarmingCycleReportQueryRepository.Cursor cursor, int size) {
        return new FarmingCycleReportQueryRepository.SearchCondition(memberId(), farmId == null ? Set.of() : Set.of(farmId), cropId == null ? Set.of() : Set.of(cropId), cursor, size);
    }

    private FarmingCycleReportQueryRepository.WorkItemSearchCondition workCondition(UUID farmId, UUID cropId, WorkType workType, List<WorkType> workTypes, FarmingCycleReportQueryRepository.WorkItemCursor cursor, int size) {
        return new FarmingCycleReportQueryRepository.WorkItemSearchCondition(memberId(), farmId == null ? Set.of() : Set.of(farmId), cropId == null ? Set.of() : Set.of(cropId), workTypes, cursor, size);
    }

    private LocalDateTime day(long day) {
        return baseTime.plusDays(day);
    }

    private <T extends BaseTimeEntity> T persist(T entity, LocalDateTime createdAt) {
        setTimestamps(entity, createdAt);
        return entityManager.persistAndFlush(entity);
    }

    private void setTimestamps(BaseTimeEntity entity, LocalDateTime createdAt) {
        try {
            for (String name : List.of("createdAt", "updatedAt")) {
                var field = BaseTimeEntity.class.getDeclaredField(name);
                field.setAccessible(true);
                field.set(entity, createdAt);
            }
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

}

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.chamchamcham.domain")
@EnableJpaRepositories(basePackages = "com.chamchamcham.domain")
class FarmingCycleReportQueryRepositoryTestApplication {}
