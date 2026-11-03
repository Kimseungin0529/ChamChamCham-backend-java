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
import static org.assertj.core.api.Assertions.assertThat;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Objects;
import com.chamchamcham.domain.member.MemberStatus;
import com.chamchamcham.domain.farm.FarmDataSource;

@DataJpaTest
@ActiveProfiles("test")
@Import(FarmingWorkReportSourceRepositoryImpl.class)
@TestPropertySource(
    properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true"
    }
)
class FarmingWorkReportSourceRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private FarmingWorkReportSourceRepository repository;

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
    void loadBatchesRecordsFinalHarvestFlagsAndDeterministicFirstImages() {
        var picturedRecord = persistRecord(member, farm, crop, WorkType.WATERING, now.plusHours(1), false);
        var finalHarvest = persistRecord(member, farm, crop, WorkType.HARVEST, now.plusHours(2), false);
        persist(
            new HarvestRecord(null, finalHarvest, BigDecimal.ONE, CropUsePartCategory.ROOT_BARK, HarvestSource.CULTIVATED, 12, true),
            now);
        var secondScopeRecord = persistRecord(member, otherFarm, otherCrop, WorkType.WATERING, now.plusHours(3), false);
        var crossProductRecord = persistRecord(member, farm, otherCrop, WorkType.WATERING, now.plusHours(4), false);
        var tiedMediaRecord = persistRecord(member, otherFarm, otherCrop, WorkType.WATERING, now.plusHours(5), false);
        persistRecord(otherMember, farm, crop, WorkType.WATERING, now.plusHours(5), false);
        persistRecord(member, farm, crop, WorkType.WATERING, now.plusHours(6), true);
        persistMedia(picturedRecord, "02", 1, "https://img/second.jpg");
        persistMedia(picturedRecord, "01", 0, "https://img/first.jpg");
        var tiedSecond = persistMedia(tiedMediaRecord, "12", 0, "https://img/tied-second.jpg");
        var tiedFirst = persistMedia(tiedMediaRecord, "11", 0, "https://img/tied-first.jpg");
        entityManager.flush();
        entityManager.clear();

        var snapshot = repository.load(
            Objects.requireNonNull(member.getId()),
            Set.of(Objects.requireNonNull(farm.getId()), Objects.requireNonNull(otherFarm.getId())),
            Set.of(Objects.requireNonNull(crop.getId()), Objects.requireNonNull(otherCrop.getId())));

        assertThat(snapshot.records().stream().map(it -> it.getId()).toList()).containsExactlyInAnyOrder(
            picturedRecord.getId(),
            finalHarvest.getId(),
            secondScopeRecord.getId(),
            crossProductRecord.getId(),
            tiedMediaRecord.getId());
        assertThat(snapshot.finalHarvestRecordIds()).containsExactly(Objects.requireNonNull(finalHarvest.getId()));
        assertThat(snapshot.firstImageUrlByRecordId().get(Objects.requireNonNull(picturedRecord.getId())))
            .isEqualTo("https://img/first.jpg");
        var expectedTiedUrl = tiedFirst.getId().compareTo(tiedSecond.getId()) < 0
            ? "https://img/tied-first.jpg" : "https://img/tied-second.jpg";
        assertThat(snapshot.firstImageUrlByRecordId().get(Objects.requireNonNull(tiedMediaRecord.getId())))
            .isEqualTo(expectedTiedUrl);
    }

    @Test
    void queryCountStaysFixedForOneScopeAndManyScopes() {
        persistRecord(member, farm, crop, WorkType.WATERING, now.plusHours(1), false);
        for (int index = 0; index < 5; index++) {
            persistRecord(member, otherFarm, otherCrop, WorkType.WATERING, now.plusHours((long) index + 2), false);
        }
        entityManager.flush();
        entityManager.clear();
        var statistics = entityManager.getEntityManager().getEntityManagerFactory()
            .unwrap(SessionFactory.class)
            .getStatistics();

        statistics.clear();
        repository.load(
            Objects.requireNonNull(member.getId()),
            Set.of(Objects.requireNonNull(farm.getId())),
            Set.of(Objects.requireNonNull(crop.getId())));
        var oneScopeQueryCount = statistics.getPrepareStatementCount();

        entityManager.clear();
        statistics.clear();
        repository.load(
            Objects.requireNonNull(member.getId()),
            Set.of(Objects.requireNonNull(farm.getId()), Objects.requireNonNull(otherFarm.getId())),
            Set.of(Objects.requireNonNull(crop.getId()), Objects.requireNonNull(otherCrop.getId())));
        var manyScopeQueryCount = statistics.getPrepareStatementCount();

        assertThat(oneScopeQueryCount).isEqualTo(3);
        assertThat(manyScopeQueryCount).isEqualTo(oneScopeQueryCount);
    }

    private FarmingRecord persistRecord(Member owner, Farm farm, Crop crop, WorkType workType, LocalDateTime workedAt, boolean isDeleted) {
        return persist(
            new FarmingRecord(null, owner, farm, crop, workType, workedAt, "맑음", 24, "memo", EntryMode.MANUAL, isDeleted),
            now);
    }

    private FarmingRecordMedia persistMedia(FarmingRecord record, String suffix, int displayOrder, String url) {
        var media = persist(
            new UploadedMedia(null, member, UploadedMediaType.IMAGE, UploadedMediaUsageType.FARMING_RECORD, url, "farming/" + suffix, UploadedMediaStatus.ATTACHED),
            now);
        return persist(
            new FarmingRecordMedia(null, record, media, displayOrder),
            now);
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
