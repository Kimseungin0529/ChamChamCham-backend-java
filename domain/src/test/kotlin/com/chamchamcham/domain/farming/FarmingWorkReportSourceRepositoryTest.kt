package com.chamchamcham.domain.farming

import com.chamchamcham.domain.common.BaseTimeEntity
import com.chamchamcham.domain.crop.Crop
import com.chamchamcham.domain.crop.CropUsePartCategory
import com.chamchamcham.domain.farm.Farm
import com.chamchamcham.domain.media.UploadedMedia
import com.chamchamcham.domain.media.UploadedMediaStatus
import com.chamchamcham.domain.media.UploadedMediaType
import com.chamchamcham.domain.media.UploadedMediaUsageType
import com.chamchamcham.domain.member.Member
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
@ActiveProfiles("test")
@Import(FarmingWorkReportSourceRepositoryImpl::class)
@TestPropertySource(
    properties = [
        "spring.jpa.properties.hibernate.generate_statistics=true",
    ],
)
class FarmingWorkReportSourceRepositoryTest @Autowired constructor(
    private val entityManager: TestEntityManager,
    private val repository: FarmingWorkReportSourceRepository,
) {
    private lateinit var member: Member
    private lateinit var otherMember: Member
    private lateinit var farm: Farm
    private lateinit var otherFarm: Farm
    private lateinit var crop: Crop
    private lateinit var otherCrop: Crop

    private val now = LocalDateTime.of(2026, 7, 1, 8, 0)

    @BeforeEach
    fun setUp() {
        member = persist(Member("member@example.com", null), now)
        otherMember = persist(Member("other@example.com", null), now)
        farm = persist(Farm(member, "약초농장", "서울시 강남구"), now)
        otherFarm = persist(Farm(member, "다른 농장", "서울시 서초구"), now)
        crop = persist(Crop(422, "황기", CropUsePartCategory.ROOT_BARK), now)
        otherCrop = persist(Crop(107, "인삼", CropUsePartCategory.ROOT_BARK), now)
    }

    @Test
    fun `load batches records final harvest flags and deterministic first images`() {
        val picturedRecord = persistRecord(farm = farm, crop = crop, workedAt = now.plusHours(1))
        val finalHarvest = persistRecord(
            farm = farm,
            crop = crop,
            workType = WorkType.HARVEST,
            workedAt = now.plusHours(2),
        )
        persist(
            HarvestRecord(null, finalHarvest, BigDecimal.ONE, CropUsePartCategory.ROOT_BARK, HarvestSource.CULTIVATED, 12, true),
            now,
        )
        val secondScopeRecord = persistRecord(farm = otherFarm, crop = otherCrop, workedAt = now.plusHours(3))
        val crossProductRecord = persistRecord(farm = farm, crop = otherCrop, workedAt = now.plusHours(4))
        val tiedMediaRecord = persistRecord(farm = otherFarm, crop = otherCrop, workedAt = now.plusHours(5))
        persistRecord(owner = otherMember, farm = farm, crop = crop, workedAt = now.plusHours(5))
        persistRecord(farm = farm, crop = crop, workedAt = now.plusHours(6), isDeleted = true)
        persistMedia(picturedRecord, suffix = "02", displayOrder = 1, url = "https://img/second.jpg")
        persistMedia(picturedRecord, suffix = "01", displayOrder = 0, url = "https://img/first.jpg")
        val tiedSecond = persistMedia(tiedMediaRecord, suffix = "12", displayOrder = 0, url = "https://img/tied-second.jpg")
        val tiedFirst = persistMedia(tiedMediaRecord, suffix = "11", displayOrder = 0, url = "https://img/tied-first.jpg")
        entityManager.flush()
        entityManager.clear()

        val snapshot = repository.load(
            memberId = requireNotNull(member.id),
            farmIds = setOf(requireNotNull(farm.id), requireNotNull(otherFarm.id)),
            cropIds = setOf(requireNotNull(crop.id), requireNotNull(otherCrop.id)),
        )

        assertThat(snapshot.records.map { it.id }).containsExactlyInAnyOrder(
            picturedRecord.id,
            finalHarvest.id,
            secondScopeRecord.id,
            crossProductRecord.id,
            tiedMediaRecord.id,
        )
        assertThat(snapshot.finalHarvestRecordIds).containsExactly(requireNotNull(finalHarvest.id))
        assertThat(snapshot.firstImageUrlByRecordId[requireNotNull(picturedRecord.id)])
            .isEqualTo("https://img/first.jpg")
        val expectedTiedUrl = if (requireNotNull(tiedFirst.id) < requireNotNull(tiedSecond.id)) {
            "https://img/tied-first.jpg"
        } else {
            "https://img/tied-second.jpg"
        }
        assertThat(snapshot.firstImageUrlByRecordId[requireNotNull(tiedMediaRecord.id)])
            .isEqualTo(expectedTiedUrl)
    }

    @Test
    fun `query count stays fixed for one scope and many scopes`() {
        persistRecord(farm = farm, crop = crop, workedAt = now.plusHours(1))
        repeat(5) { index ->
            persistRecord(farm = otherFarm, crop = otherCrop, workedAt = now.plusHours(index.toLong() + 2))
        }
        entityManager.flush()
        entityManager.clear()
        val statistics = entityManager.entityManager.entityManagerFactory
            .unwrap(SessionFactory::class.java)
            .statistics

        statistics.clear()
        repository.load(
            memberId = requireNotNull(member.id),
            farmIds = setOf(requireNotNull(farm.id)),
            cropIds = setOf(requireNotNull(crop.id)),
        )
        val oneScopeQueryCount = statistics.prepareStatementCount

        entityManager.clear()
        statistics.clear()
        repository.load(
            memberId = requireNotNull(member.id),
            farmIds = setOf(requireNotNull(farm.id), requireNotNull(otherFarm.id)),
            cropIds = setOf(requireNotNull(crop.id), requireNotNull(otherCrop.id)),
        )
        val manyScopeQueryCount = statistics.prepareStatementCount

        assertThat(oneScopeQueryCount).isEqualTo(3)
        assertThat(manyScopeQueryCount).isEqualTo(oneScopeQueryCount)
    }

    private fun persistRecord(
        owner: Member = member,
        farm: Farm,
        crop: Crop,
        workType: WorkType = WorkType.WATERING,
        workedAt: LocalDateTime,
        isDeleted: Boolean = false,
    ): FarmingRecord =
        persist(
            FarmingRecord(null, owner, farm, crop, workType, workedAt, "맑음", 24, "memo", EntryMode.MANUAL, isDeleted),
            now,
        )

    private fun persistMedia(
        record: FarmingRecord,
        suffix: String,
        displayOrder: Int,
        url: String,
    ): FarmingRecordMedia {
        val media = persist(
            UploadedMedia(
                member,
                UploadedMediaType.IMAGE,
                UploadedMediaUsageType.FARMING_RECORD,
                url,
                "farming/$suffix"
            ).also { migratedUploadedMedia ->
                migratedUploadedMedia.status = UploadedMediaStatus.ATTACHED
            },
            now,
        )
        return persist(
            FarmingRecordMedia(null, record, media, displayOrder),
            now,
        )
    }

    private fun <T : BaseTimeEntity> persist(entity: T, createdAt: LocalDateTime): T {
        setTimestamps(entity, createdAt)
        return entityManager.persistAndFlush(entity)
    }

    private fun setTimestamps(entity: BaseTimeEntity, createdAt: LocalDateTime) {
        BaseTimeEntity::class.java.getDeclaredField("createdAt").apply {
            isAccessible = true
            set(entity, createdAt)
        }
        BaseTimeEntity::class.java.getDeclaredField("updatedAt").apply {
            isAccessible = true
            set(entity, createdAt)
        }
    }
}
