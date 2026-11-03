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
import com.chamchamcham.domain.pesticide.Pest;
import com.chamchamcham.domain.pesticide.Pesticide;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
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
@Import(FarmingCycleReportSourceRepositoryImpl.class)
class FarmingCycleReportSourceRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private FarmingCycleReportSourceRepository repository;

    private Member member;
    private Member otherMember;
    private Farm farm;
    private Farm otherFarm;
    private Crop crop;
    private Crop otherCrop;

    private final LocalDateTime now = LocalDateTime.of(2026, 7, 1, 8, 0);

    @BeforeEach
    void setUp() {
        member = persist(new Member(null, "member@example.com", null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null), now);
        otherMember = persist(new Member(null, "other@example.com", null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null), now);
        farm = persist(new Farm(null, member, "약초농장", "서울시 강남구", null, null, null, null, null, null, false, new ArrayList<>(), new FarmDataSource(null, null, null, null)), now);
        otherFarm = persist(new Farm(null, member, "다른 농장", "서울시 서초구", null, null, null, null, null, null, false, new ArrayList<>(), new FarmDataSource(null, null, null, null)), now);
        crop = persist(new Crop(null, 422, "황기", CropUsePartCategory.ROOT_BARK), now);
        otherCrop = persist(new Crop(null, 107, "인삼", CropUsePartCategory.ROOT_BARK), now);
    }

    @Test
    void loadReturnsOrderedActiveRecordsWithDetailMapsAndDistinctMediaIds() {
        var pruning = persistRecord(WorkType.PRUNING, member, this.farm, this.crop, now.plusHours(6), false);
        var planting = persistRecord(WorkType.PLANTING, member, this.farm, this.crop, now.plusHours(1), false);
        persistPlanting(planting);
        var watering = persistRecord(WorkType.WATERING, member, this.farm, this.crop, now.plusHours(2), false);
        persistWatering(watering);
        var fertilizing = persistRecord(WorkType.FERTILIZING, member, this.farm, this.crop, now.plusHours(3), false);
        persistFertilizing(fertilizing);
        var pestControl = persistRecord(WorkType.PEST_CONTROL, member, this.farm, this.crop, now.plusHours(4), false);
        persistPestControl(pestControl);
        var weeding = persistRecord(WorkType.WEEDING, member, this.farm, this.crop, now.plusHours(5), false);
        persistWeeding(weeding);
        var harvest = persistRecord(WorkType.HARVEST, member, this.farm, this.crop, now.plusHours(7), false);
        persistHarvest(harvest, true);
        var etc = persistRecord(WorkType.ETC, member, this.farm, this.crop, now.plusHours(8), false);
        persistMedia(etc, 0);
        persistMedia(etc, 1);
        persistRecord(WorkType.WATERING, member, this.farm, this.crop, now.plusHours(9), true);
        persistRecord(WorkType.PLANTING, otherMember, this.farm, this.crop, now.plusHours(10), false);
        persistRecord(WorkType.PLANTING, member, otherFarm, this.crop, now.plusHours(11), false);
        persistRecord(WorkType.PLANTING, member, this.farm, otherCrop, now.plusHours(12), false);
        entityManager.flush();
        entityManager.clear();

        var snapshot = repository.load(
            Objects.requireNonNull(member.getId()),
            Objects.requireNonNull(farm.getId()),
            Objects.requireNonNull(crop.getId()));

        assertThat(snapshot.records().stream().map(it -> it.getId()).toList()).containsExactly(
            planting.getId(),
            watering.getId(),
            fertilizing.getId(),
            pestControl.getId(),
            weeding.getId(),
            pruning.getId(),
            harvest.getId(),
            etc.getId());
        assertThat(snapshot.records()).allMatch(it -> !it.isDeleted());
        assertThat(snapshot.plantingByRecordId()).containsKey(planting.getId());
        assertThat(snapshot.wateringByRecordId()).containsKey(watering.getId());
        assertThat(snapshot.fertilizingByRecordId()).containsKey(fertilizing.getId());
        assertThat(snapshot.pestControlByRecordId()).containsKey(pestControl.getId());
        assertThat(snapshot.weedingByRecordId()).containsKey(weeding.getId());
        assertThat(snapshot.harvestByRecordId().get(harvest.getId()).isLastHarvest()).isTrue();
        assertThat(snapshot.mediaRecordIds()).containsExactly(Objects.requireNonNull(etc.getId()));
    }

    private FarmingRecord persistRecord(WorkType workType, Member owner, Farm farm, Crop crop, LocalDateTime workedAt, boolean isDeleted) {
        return persist(
            new FarmingRecord(null, owner, farm, crop, workType, workedAt, "맑음", 24, "memo", EntryMode.MANUAL, isDeleted),
            now);
    }

    private void persistPlanting(FarmingRecord record) {
        persist(
            new PlantingRecord(null, record, PlantingMethod.SEED, new BigDecimal("1.2500"), SeedAmountUnit.G, null, null, null),
            now);
    }

    private void persistWatering(FarmingRecord record) {
        persist(
            new WateringRecord(null, record, IrrigationAmount.NORMAL, IrrigationMethod.DRIP),
            now);
    }

    private void persistFertilizing(FarmingRecord record) {
        persist(
            new FertilizingRecord(null, record, "유기질 비료", new BigDecimal("3.0000"), FertilizerAmountUnit.G, FertilizingMethod.SOIL),
            now);
    }

    private void persistPestControl(FarmingRecord record) {
        var pesticide = persist(new Pesticide(null, "살충제", "참참약", null, null, null, null, null, null), now);
        var pest = persist(new Pest(null, "진딧물"), now);
        persist(
            new PestControlRecord(null, record, pesticide, new BigDecimal("30.0000"), PesticideAmountUnit.ML, new BigDecimal("10000.0000"), SprayAmountUnit.ML, pest),
            now);
    }

    private void persistWeeding(FarmingRecord record) {
        persist(new WeedingRecord(null, record, WeedingMethod.HAND), now);
    }

    private void persistHarvest(FarmingRecord record, boolean isLastHarvest) {
        persist(
            new HarvestRecord(null, record, new BigDecimal("5.0000"), CropUsePartCategory.ROOT_BARK, HarvestSource.CULTIVATED, 24, isLastHarvest),
            now);
    }

    private void persistMedia(FarmingRecord record, int displayOrder) {
        var media = persist(
            new UploadedMedia(null, member, UploadedMediaType.IMAGE, UploadedMediaUsageType.FARMING_RECORD, "https://example.test/" + displayOrder + ".jpg", "farming/" + UUID.randomUUID(), UploadedMediaStatus.ATTACHED),
            now);
        persist(new FarmingRecordMedia(null, record, media, displayOrder), now);
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
