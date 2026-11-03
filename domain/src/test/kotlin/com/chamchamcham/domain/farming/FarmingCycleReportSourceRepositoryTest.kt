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
import com.chamchamcham.domain.pesticide.Pest
import com.chamchamcham.domain.pesticide.Pesticide
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
@ActiveProfiles("test")
@Import(FarmingCycleReportSourceRepositoryImpl::class)
class FarmingCycleReportSourceRepositoryTest @Autowired constructor(
    private val entityManager: TestEntityManager,
    private val repository: FarmingCycleReportSourceRepository,
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
    fun `load returns ordered active records with detail maps and distinct media ids`() {
        val pruning = persistRecord(WorkType.PRUNING, workedAt = now.plusHours(6))
        val planting = persistRecord(WorkType.PLANTING, workedAt = now.plusHours(1))
        persistPlanting(planting)
        val watering = persistRecord(WorkType.WATERING, workedAt = now.plusHours(2))
        persistWatering(watering)
        val fertilizing = persistRecord(WorkType.FERTILIZING, workedAt = now.plusHours(3))
        persistFertilizing(fertilizing)
        val pestControl = persistRecord(WorkType.PEST_CONTROL, workedAt = now.plusHours(4))
        persistPestControl(pestControl)
        val weeding = persistRecord(WorkType.WEEDING, workedAt = now.plusHours(5))
        persistWeeding(weeding)
        val harvest = persistRecord(WorkType.HARVEST, workedAt = now.plusHours(7))
        persistHarvest(harvest, isLastHarvest = true)
        val etc = persistRecord(WorkType.ETC, workedAt = now.plusHours(8))
        persistMedia(etc, displayOrder = 0)
        persistMedia(etc, displayOrder = 1)
        persistRecord(WorkType.WATERING, workedAt = now.plusHours(9), isDeleted = true)
        persistRecord(WorkType.PLANTING, owner = otherMember, workedAt = now.plusHours(10))
        persistRecord(WorkType.PLANTING, farm = otherFarm, workedAt = now.plusHours(11))
        persistRecord(WorkType.PLANTING, crop = otherCrop, workedAt = now.plusHours(12))
        entityManager.flush()
        entityManager.clear()

        val snapshot = repository.load(
            memberId = requireNotNull(member.id),
            farmId = requireNotNull(farm.id),
            cropId = requireNotNull(crop.id),
        )

        assertThat(snapshot.records.map { it.id }).containsExactly(
            planting.id,
            watering.id,
            fertilizing.id,
            pestControl.id,
            weeding.id,
            pruning.id,
            harvest.id,
            etc.id,
        )
        assertThat(snapshot.records).allMatch { !it.isDeleted }
        assertThat(snapshot.plantingByRecordId).containsKey(planting.id)
        assertThat(snapshot.wateringByRecordId).containsKey(watering.id)
        assertThat(snapshot.fertilizingByRecordId).containsKey(fertilizing.id)
        assertThat(snapshot.pestControlByRecordId).containsKey(pestControl.id)
        assertThat(snapshot.weedingByRecordId).containsKey(weeding.id)
        assertThat(snapshot.harvestByRecordId[harvest.id]?.isLastHarvest).isTrue()
        assertThat(snapshot.mediaRecordIds).containsExactly(requireNotNull(etc.id))
    }

    private fun persistRecord(
        workType: WorkType,
        owner: Member = member,
        farm: Farm = this.farm,
        crop: Crop = this.crop,
        workedAt: LocalDateTime,
        isDeleted: Boolean = false,
    ): FarmingRecord =
        persist(
            FarmingRecord(null, owner, farm, crop, workType, workedAt, "맑음", 24, "memo", EntryMode.MANUAL, isDeleted),
            now,
        )

    private fun persistPlanting(record: FarmingRecord) {
        persist(
            PlantingRecord(null, record, PlantingMethod.SEED, BigDecimal("1.2500"), SeedAmountUnit.G, null, null, null),
            now,
        )
    }

    private fun persistWatering(record: FarmingRecord) {
        persist(
            WateringRecord(null, record, IrrigationAmount.NORMAL, IrrigationMethod.DRIP),
            now,
        )
    }

    private fun persistFertilizing(record: FarmingRecord) {
        persist(
            FertilizingRecord(null, record, "유기질 비료", BigDecimal("3.0000"), FertilizerAmountUnit.G, FertilizingMethod.SOIL),
            now,
        )
    }

    private fun persistPestControl(record: FarmingRecord) {
        val pesticide = persist(Pesticide("살충제", "참참약"), now)
        val pest = persist(Pest("진딧물"), now)
        persist(
            PestControlRecord(null, record, pesticide, BigDecimal("30.0000"), PesticideAmountUnit.ML, BigDecimal("10000.0000"), SprayAmountUnit.ML, pest),
            now,
        )
    }

    private fun persistWeeding(record: FarmingRecord) {
        persist(WeedingRecord(null, record, WeedingMethod.HAND), now)
    }

    private fun persistHarvest(record: FarmingRecord, isLastHarvest: Boolean) {
        persist(
            HarvestRecord(null, record, BigDecimal("5.0000"), CropUsePartCategory.ROOT_BARK, HarvestSource.CULTIVATED, 24, isLastHarvest),
            now,
        )
    }

    private fun persistMedia(record: FarmingRecord, displayOrder: Int) {
        val media = persist(
            UploadedMedia(
                member,
                UploadedMediaType.IMAGE,
                UploadedMediaUsageType.FARMING_RECORD,
                "https://example.test/$displayOrder.jpg",
                "farming/${UUID.randomUUID()}"
            ).also { migratedUploadedMedia ->
                migratedUploadedMedia.status = UploadedMediaStatus.ATTACHED
            },
            now,
        )
        persist(FarmingRecordMedia(null, record, media, displayOrder), now)
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
