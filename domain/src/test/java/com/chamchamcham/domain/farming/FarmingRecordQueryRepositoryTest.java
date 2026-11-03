package com.chamchamcham.domain.farming;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.media.UploadedMedia;
import com.chamchamcham.domain.media.UploadedMediaStatus;
import com.chamchamcham.domain.media.UploadedMediaType;
import com.chamchamcham.domain.media.UploadedMediaUsageType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.pesticide.Pesticide;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import com.chamchamcham.domain.member.MemberStatus;
import com.chamchamcham.domain.farm.FarmDataSource;

@DataJpaTest
@ActiveProfiles("test")
@Import(FarmingRecordQueryRepositoryImpl.class)
class FarmingRecordQueryRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private FarmingRecordQueryRepository queryRepository;

    private Member member;
    private Member otherMember;
    private Farm farm;
    private Crop hwanggiCrop;
    private Crop ginsengCrop;

    private UUID memberId() { return Objects.requireNonNull(member.getId()); }
    private UUID hwanggiCropId() { return Objects.requireNonNull(hwanggiCrop.getId()); }
    private UUID ginsengCropId() { return Objects.requireNonNull(ginsengCrop.getId()); }

    private final LocalDateTime now = LocalDateTime.of(2026, 6, 12, 7, 0);

    @BeforeEach
    void setUp() {
        member = persist(new Member(null, "member@example.com", null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null), now);
        otherMember = persist(new Member(null, "other@example.com", null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null), now);
        farm = persist(new Farm(null, member, "약초농장", "서울시 강남구", null, null, null, null, null, null, false, new ArrayList<>(), new FarmDataSource(null, null, null, null)), now);
        hwanggiCrop = persist(crop("황기", 422), now);
        ginsengCrop = persist(crop("인삼", 107), now);
    }

    @Test
    void searchReturnsOwnActiveRecordsLatestFirstWithCursor() {
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 10, 0), "memo", false);
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 8, 0), "memo", false);
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 11, 0), "memo", true);
        entityManager.flush();
        entityManager.clear();

        var firstPage = queryRepository.search(condition(List.of(), List.of(), null, null, null, List.of(), List.of(), null, 2));
        var secondPage = queryRepository.search(
            condition(List.of(), List.of(), null, null, null, List.of(), List.of(), new FarmingRecordQueryRepository.Cursor(firstPage.rows().getLast().record().getWorkedAt(), Objects.requireNonNull(firstPage.rows().getLast().record().getId())), 2)
        );

        assertThat(firstPage.rows().stream().map(it -> it.record().getWorkedAt()).toList()).containsExactly(
            LocalDateTime.of(2026, 6, 12, 10, 0),
            LocalDateTime.of(2026, 6, 12, 9, 0)
        );
        assertThat(secondPage.rows().stream().map(it -> it.record().getWorkedAt()).toList()).containsExactly(LocalDateTime.of(2026, 6, 12, 8, 0));
    }

    @Test
    void searchFiltersByCrop() {
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistRecord(member, ginsengCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(condition(List.of(hwanggiCropId()), List.of(), null, null, null, List.of(), List.of(), null, 20));

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().record().getCrop().getId()).isEqualTo(hwanggiCropId());
    }

    @Test
    void searchFiltersByWorkType() {
        persistRecord(member, hwanggiCrop, WorkType.PLANTING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistRecord(member, hwanggiCrop, WorkType.HARVEST, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(condition(List.of(), List.of(WorkType.HARVEST), null, null, null, List.of(), List.of(), null, 20));

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().record().getWorkType()).isEqualTo(WorkType.HARVEST);
    }

    @Test
    void searchFiltersByMultipleCropIdsAndMultipleWorkTypes() {
        persistRecord(member, hwanggiCrop, WorkType.PLANTING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistRecord(member, ginsengCrop, WorkType.HARVEST, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistRecord(member, ginsengCrop, WorkType.WATERING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(
            condition(List.of(hwanggiCropId(), ginsengCropId()), List.of(WorkType.PLANTING, WorkType.HARVEST), null, null, null, List.of(), List.of(), null, 20)
        );

        assertThat(result.rows()).hasSize(2);
        assertThat(result.rows().stream().map(it -> it.record().getWorkType()).toList()).containsExactlyInAnyOrder(WorkType.PLANTING, WorkType.HARVEST);
    }

    @Test
    void searchFiltersByWorkedAtRange() {
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 5, 1, 9, 0), "memo", false);
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 15, 9, 0), "memo", false);
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 7, 1, 9, 0), "memo", false);
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(
            condition(List.of(), List.of(), LocalDateTime.of(2026, 6, 1, 0, 0), LocalDateTime.of(2026, 7, 1, 0, 0), null, List.of(), List.of(), null, 20)
        );

        assertThat(result.rows().stream().map(it -> it.record().getWorkedAt()).toList()).containsExactly(LocalDateTime.of(2026, 6, 15, 9, 0));
    }

    @Test
    void searchExcludesOtherMembersRecords() {
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistRecord(otherMember, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(condition(List.of(), List.of(), null, null, null, List.of(), List.of(), null, 20));

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().record().getMember().getId()).isEqualTo(memberId());
    }

    @Test
    void searchReturnsThumbnailFromFirstMediaByDisplayOrder() {
        var record = persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistMedia(record, 1, "https://example.test/2.jpg");
        persistMedia(record, 0, "https://example.test/1.jpg");
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(condition(List.of(), List.of(), null, null, null, List.of(), List.of(), null, 20));

        assertThat(result.rows().getFirst().thumbnailUrl()).isEqualTo("https://example.test/1.jpg");
    }

    @Test
    void searchMatchesKeywordAgainstCropName() {
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistRecord(member, ginsengCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(condition(List.of(), List.of(), null, null, "황기", List.of(), List.of(), null, 20));

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().record().getCrop().getId()).isEqualTo(hwanggiCropId());
    }

    @Test
    void searchMatchesKeywordAgainstMemo() {
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "진딧물이 많이 보였다", false);
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "특이사항 없음", false);
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(condition(List.of(), List.of(), null, null, "진딧물", List.of(), List.of(), null, 20));

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().record().getMemo()).isEqualTo("진딧물이 많이 보였다");
    }

    @Test
    void searchMatchesKeywordAgainstFertilizingMaterialName() {
        var record = persistRecord(member, hwanggiCrop, WorkType.FERTILIZING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistFertilizingDetail(record, "유박비료");
        var other = persistRecord(member, hwanggiCrop, WorkType.FERTILIZING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistFertilizingDetail(other, "복합비료");
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(condition(List.of(), List.of(), null, null, "유박", List.of(), List.of(), null, 20));

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().record().getId()).isEqualTo(record.getId());
    }

    @Test
    void searchMatchesKeywordAgainstPestControlPesticideName() {
        var record = persistRecord(member, hwanggiCrop, WorkType.PEST_CONTROL, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistPestControlDetail(record, "친환경약제", "친환경약제");
        var other = persistRecord(member, hwanggiCrop, WorkType.PEST_CONTROL, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistPestControlDetail(other, "일반약제", "일반약제");
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(condition(List.of(), List.of(), null, null, "친환경", List.of(), List.of(), null, 20));

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().record().getId()).isEqualTo(record.getId());
    }

    @Test
    void searchMatchesMatchedWorkTypes() {
        persistRecord(member, hwanggiCrop, WorkType.HARVEST, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistRecord(member, hwanggiCrop, WorkType.WATERING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(condition(List.of(), List.of(), null, null, null, List.of(WorkType.HARVEST), List.of(), null, 20));

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().record().getWorkType()).isEqualTo(WorkType.HARVEST);
    }

    @Test
    void searchMatchesMatchedHarvestParts() {
        var leafCrop = persist(crop("깻잎", 900), now);
        var harvestRecord = persistRecord(member, leafCrop, WorkType.HARVEST, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistHarvestDetail(harvestRecord, CropUsePartCategory.LEAF);
        var otherHarvest = persistRecord(member, hwanggiCrop, WorkType.HARVEST, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistHarvestDetail(otherHarvest, CropUsePartCategory.ROOT_BARK);
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(condition(List.of(), List.of(), null, null, null, List.of(), List.of(CropUsePartCategory.LEAF), null, 20));

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().record().getId()).isEqualTo(harvestRecord.getId());
    }

    @Test
    void searchCombinesKeywordWithStructuralFilters() {
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "황기 관수 완료", false);
        persistRecord(member, ginsengCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "관수 완료", false);
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(condition(List.of(hwanggiCropId()), List.of(), null, null, "관수", List.of(), List.of(), null, 20));

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().record().getCrop().getId()).isEqualTo(hwanggiCropId());
    }

    @Test
    void searchExposesIrrigationMethodForWateringRecordsAndLeavesOtherSummaryFieldsNull() {
        var record = persistRecord(member, hwanggiCrop, WorkType.WATERING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistWateringDetail(record, IrrigationMethod.DRIP);
        entityManager.flush();
        entityManager.clear();

        var row = queryRepository.search(condition(List.of(), List.of(), null, null, null, List.of(), List.of(), null, 20)).rows().getFirst();

        assertThat(row.irrigationMethod()).isEqualTo(IrrigationMethod.DRIP);
        assertThat(row.harvestAmount()).isNull();
        assertThat(row.pesticideName()).isNull();
        assertThat(row.weedingMethod()).isNull();
        assertThat(row.plantingMethod()).isNull();
        assertThat(row.materialName()).isNull();
    }

    @Test
    void searchExposesHarvestAmountForHarvestRecordsAndLeavesOtherSummaryFieldsNull() {
        var record = persistRecord(member, hwanggiCrop, WorkType.HARVEST, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistHarvestDetail(record, CropUsePartCategory.LEAF);
        entityManager.flush();
        entityManager.clear();

        var row = queryRepository.search(condition(List.of(), List.of(), null, null, null, List.of(), List.of(), null, 20)).rows().getFirst();

        assertThat(row.harvestAmount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(row.irrigationMethod()).isNull();
        assertThat(row.pesticideName()).isNull();
        assertThat(row.weedingMethod()).isNull();
        assertThat(row.plantingMethod()).isNull();
        assertThat(row.materialName()).isNull();
    }

    @Test
    void searchExposesPesticideItemNameForPestControlRecordsAndLeavesOtherSummaryFieldsNull() {
        var record = persistRecord(member, hwanggiCrop, WorkType.PEST_CONTROL, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistPestControlDetail(record, "만코제브 수화제", "빔");
        entityManager.flush();
        entityManager.clear();

        var row = queryRepository.search(condition(List.of(), List.of(), null, null, null, List.of(), List.of(), null, 20)).rows().getFirst();

        assertThat(row.pesticideName()).isEqualTo("만코제브 수화제");
        assertThat(row.irrigationMethod()).isNull();
        assertThat(row.harvestAmount()).isNull();
        assertThat(row.weedingMethod()).isNull();
        assertThat(row.plantingMethod()).isNull();
        assertThat(row.materialName()).isNull();
    }

    @Test
    void searchExposesWeedingMethodForWeedingRecordsAndLeavesOtherSummaryFieldsNull() {
        var record = persistRecord(member, hwanggiCrop, WorkType.WEEDING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistWeedingDetail(record, WeedingMethod.HAND);
        entityManager.flush();
        entityManager.clear();

        var row = queryRepository.search(condition(List.of(), List.of(), null, null, null, List.of(), List.of(), null, 20)).rows().getFirst();

        assertThat(row.weedingMethod()).isEqualTo(WeedingMethod.HAND);
        assertThat(row.irrigationMethod()).isNull();
        assertThat(row.harvestAmount()).isNull();
        assertThat(row.pesticideName()).isNull();
        assertThat(row.plantingMethod()).isNull();
        assertThat(row.materialName()).isNull();
    }

    @Test
    void searchExposesPlantingMethodForPlantingRecordsAndLeavesOtherSummaryFieldsNull() {
        var record = persistRecord(member, hwanggiCrop, WorkType.PLANTING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistPlantingDetail(record, PlantingMethod.SEEDLING);
        entityManager.flush();
        entityManager.clear();

        var row = queryRepository.search(condition(List.of(), List.of(), null, null, null, List.of(), List.of(), null, 20)).rows().getFirst();

        assertThat(row.plantingMethod()).isEqualTo(PlantingMethod.SEEDLING);
        assertThat(row.irrigationMethod()).isNull();
        assertThat(row.harvestAmount()).isNull();
        assertThat(row.pesticideName()).isNull();
        assertThat(row.weedingMethod()).isNull();
        assertThat(row.materialName()).isNull();
    }

    @Test
    void searchExposesMaterialNameForFertilizingRecordsAndLeavesOtherSummaryFieldsNull() {
        var record = persistRecord(member, hwanggiCrop, WorkType.FERTILIZING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistFertilizingDetail(record, "유박비료");
        entityManager.flush();
        entityManager.clear();

        var row = queryRepository.search(condition(List.of(), List.of(), null, null, null, List.of(), List.of(), null, 20)).rows().getFirst();

        assertThat(row.materialName()).isEqualTo("유박비료");
        assertThat(row.irrigationMethod()).isNull();
        assertThat(row.harvestAmount()).isNull();
        assertThat(row.pesticideName()).isNull();
        assertThat(row.weedingMethod()).isNull();
        assertThat(row.plantingMethod()).isNull();
    }

    @Test
    void searchReturnsNullSummaryFieldsWhenRecordHasNoMatchingDetail() {
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        entityManager.flush();
        entityManager.clear();

        var row = queryRepository.search(condition(List.of(), List.of(), null, null, null, List.of(), List.of(), null, 20)).rows().getFirst();

        assertThat(row.irrigationMethod()).isNull();
        assertThat(row.harvestAmount()).isNull();
        assertThat(row.pesticideName()).isNull();
        assertThat(row.weedingMethod()).isNull();
        assertThat(row.plantingMethod()).isNull();
        assertThat(row.materialName()).isNull();
    }

    @Test
    void searchReturnsEmptyWhenKeywordMatchesNothing() {
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "특이사항 없음", false);
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(condition(List.of(), List.of(), null, null, "존재하지않는키워드", List.of(), List.of(), null, 20));

        assertThat(result.rows()).isEmpty();
    }

    @Test
    void countReturnsTotalMatchingRecordsIgnoringCursorAndSize() {
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistRecord(member, ginsengCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        entityManager.flush();
        entityManager.clear();

        var total = queryRepository.count(condition(List.of(hwanggiCropId()), List.of(), null, null, null, List.of(), List.of(), null, 1));

        assertThat(total).isEqualTo(2);
    }

    @Test
    void countAppliesKeywordAndStructuralFiltersSameAsSearch() {
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "황기 관수 완료", false);
        persistRecord(member, ginsengCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "관수 완료", false);
        entityManager.flush();
        entityManager.clear();

        var total = queryRepository.count(condition(List.of(hwanggiCropId()), List.of(), null, null, "관수", List.of(), List.of(), null, 20));

        assertThat(total).isEqualTo(1);
    }

    @Test
    void countExcludesOtherMembersAndDeletedRecords() {
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistRecord(otherMember, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", false);
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, LocalDateTime.of(2026, 6, 12, 9, 0), "memo", true);
        entityManager.flush();
        entityManager.clear();

        var total = queryRepository.count(condition(List.of(), List.of(), null, null, null, List.of(), List.of(), null, 20));

        assertThat(total).isEqualTo(1);
    }

    @Test
    void keywordTrimsUnicodeSpaceCharactersBeforeMatching() {
        persistRecord(member, hwanggiCrop, WorkType.PRUNING, now, "황기 관수", false);
        entityManager.flush();
        entityManager.clear();
        var filter = condition(List.of(), List.of(), null, null, "\u00a0황기\u00a0", List.of(), List.of(), null, 20);
        assertThat(queryRepository.search(filter).rows()).hasSize(1);
        assertThat(queryRepository.count(filter)).isEqualTo(1);
    }

    private FarmingRecord persistRecord(Member owner, Crop crop, WorkType workType, LocalDateTime workedAt, String memo, boolean isDeleted) {
        var record = new FarmingRecord(null, owner, farm, crop, workType, workedAt, "맑음", 20, memo, EntryMode.MANUAL, isDeleted);
        return persist(record, now);
    }

    private void persistMedia(FarmingRecord record, int displayOrder, String url) {
        var media = persist(
            new UploadedMedia(null, member, UploadedMediaType.IMAGE, UploadedMediaUsageType.FARMING_RECORD, url, "farming/" + UUID.randomUUID(), UploadedMediaStatus.ATTACHED),
            now
        );
        persist(new FarmingRecordMedia(null, record, media, displayOrder), now);
    }

    private void persistPlantingDetail(FarmingRecord record, PlantingMethod plantingMethod) {
        persist(
            new PlantingRecord(null, record, plantingMethod, null, null, null, null, null),
            now
        );
    }

    private void persistWateringDetail(FarmingRecord record, IrrigationMethod irrigationMethod) {
        persist(
            new WateringRecord(null, record, null, irrigationMethod),
            now
        );
    }

    private void persistWeedingDetail(FarmingRecord record, WeedingMethod weedingMethod) {
        persist(
            new WeedingRecord(null, record, weedingMethod),
            now
        );
    }

    private void persistFertilizingDetail(FarmingRecord record, String materialName) {
        persist(
            new FertilizingRecord(null, record, materialName, BigDecimal.TEN, FertilizerAmountUnit.G, null),
            now
        );
    }

    private void persistPestControlDetail(FarmingRecord record, String pesticideName, String brandName) {
        var pesticide = persist(
            new Pesticide(null, pesticideName, brandName, null, null, null, null, null, null),
            now
        );
        persist(
            new PestControlRecord(null, record, pesticide, BigDecimal.ONE, PesticideAmountUnit.ML, BigDecimal.TEN, SprayAmountUnit.ML, null),
            now
        );
    }

    private void persistHarvestDetail(FarmingRecord record, CropUsePartCategory medicinalPart) {
        persist(
            new HarvestRecord(null, record, BigDecimal.TEN, medicinalPart, HarvestSource.CULTIVATED, 1, false),
            now
        );
    }

    private Crop crop(String name, int externalNo) {
        return new Crop(null, externalNo, name, CropUsePartCategory.ROOT_BARK);
    }

    private FarmingRecordQueryRepository.SearchCondition condition(List<UUID> cropIds, List<WorkType> workTypes, LocalDateTime workedAtFrom, LocalDateTime workedAtTo, String keyword, List<WorkType> matchedWorkTypes, List<CropUsePartCategory> matchedParts, FarmingRecordQueryRepository.Cursor cursor, int size) {
        return new FarmingRecordQueryRepository.SearchCondition(memberId(), cropIds, workTypes, workedAtFrom, workedAtTo, keyword, matchedWorkTypes, matchedParts, cursor, size);
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
class FarmingRecordQueryRepositoryTestApplication {}
