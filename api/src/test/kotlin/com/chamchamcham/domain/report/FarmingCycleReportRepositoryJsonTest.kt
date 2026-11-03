package com.chamchamcham.domain.report

import com.chamchamcham.domain.common.BaseTimeEntity
import com.chamchamcham.domain.crop.Crop
import com.chamchamcham.domain.crop.CropUsePartCategory
import com.chamchamcham.domain.farm.Farm
import com.chamchamcham.domain.farming.FarmingRecord
import com.chamchamcham.domain.farming.EntryMode
import com.chamchamcham.domain.farming.WorkType
import com.chamchamcham.domain.member.Member
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:report-json-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
    ],
)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FarmingCycleReportRepositoryJsonTest @Autowired constructor(
    private val entityManager: TestEntityManager,
    private val repository: FarmingCycleReportRepository,
) {
    private lateinit var member: Member
    private lateinit var otherMember: Member
    private lateinit var farm: Farm
    private lateinit var crop: Crop
    private lateinit var finalHarvest: FarmingRecord

    private val otherMemberId: UUID
        get() = requireNotNull(otherMember.id) { "Persisted member id is required" }
    private val workedAt = LocalDateTime.of(2026, 6, 15, 8, 30)

    @BeforeEach
    fun setUp() {
        member = persist(Member("member@example.com", null))
        otherMember = persist(Member("other@example.com", null))
        farm = persist(Farm(member, "약초농장", "강원도 평창군"))
        crop = persist(
            Crop(422, "황기", CropUsePartCategory.ROOT_BARK),
        )
        finalHarvest = persist(
            FarmingRecord(null, member, farm, crop, WorkType.HARVEST, workedAt, "맑음", 24, "최종 수확", EntryMode.MANUAL, false),
        )
    }

    @Test
    fun `typed statistics round trip through production Jackson JSON mapping`() {
        val original = completedReport(statisticsWithEveryWorkType())
        val id = requireNotNull(repository.saveAndFlush(original).id)
        entityManager.clear()

        val loaded = repository.findById(id).orElseThrow()

        assertThat(loaded.statistics).isEqualTo(original.statistics)
        assertThat(loaded.sourceRevision).isEqualTo(original.sourceRevision)
    }

    @Test
    fun `statistics JSON saved before distribution fields remains readable`() {
        val statistics = jacksonObjectMapper().readValue<CycleReportStatistics>(
            """{"planting":{"recordCount":1},"harvest":{"recordCount":1}}""",
        )

        assertThat(statistics.planting.recordCount).isEqualTo(1)
        assertThat(statistics.planting.plantingMethodDistribution).isEmpty()
        assertThat(statistics.harvest.recordCount).isEqualTo(1)
        assertThat(statistics.harvest.growthPeriodDistribution).isEmpty()
    }

    @Test
    fun `statistics JSON rejects explicit null for non nullable default fields`() {
        val mapper = jacksonObjectMapper()
        listOf(
            """{"planting":null}""",
            """{"planting":{"weatherDistribution":null}}""",
            """{"harvest":{"amountCoverage":null}}""",
        ).forEach { json ->
            assertThatThrownBy { mapper.readValue<CycleReportStatistics>(json) }
                .isInstanceOf(com.fasterxml.jackson.databind.JsonMappingException::class.java)
        }
    }

    @Test
    fun `statistics JSON preserves empty arrays and nullable fields`() {
        val statistics = jacksonObjectMapper().readValue<CycleReportStatistics>(
            """{"planting":{"weatherDistribution":[],"firstWorkedOn":null},"harvest":{"totalAmountKg":null}}""",
        )
        assertThat(statistics.planting.weatherDistribution).isEmpty()
        assertThat(statistics.planting.firstWorkedOn).isNull()
        assertThat(statistics.harvest.totalAmountKg).isNull()
        assertThat(statistics.watering).isEqualTo(WateringStatistics.empty())
    }

    @Test
    fun `member scoped lookup does not expose another members report`() {
        val report = repository.saveAndFlush(completedReport())

        assertThat(repository.findByIdAndMember_Id(requireNotNull(report.id), otherMemberId))
            .isNull()
    }

    private fun completedReport(
        statistics: CycleReportStatistics = CycleReportStatistics.empty(),
    ): FarmingCycleReport {
        return FarmingCycleReport.create(
            member,
            farm,
            crop,
            FarmingCycleReportProjection(
                FarmingCycleReportStatus.COMPLETED,
                workedAt.minusMonths(6),
                workedAt,
                FarmingCycleStartBasis.AFTER_PREVIOUS_FINAL_HARVEST,
                finalHarvest,
                1,
                statistics,
            ),
        ).also { setTimestamps(it, workedAt) }
    }

    private fun statisticsWithEveryWorkType(): CycleReportStatistics {
        return CycleReportStatistics(
            PlantingStatistics(
                1,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 1),
                1,
                null,
                1,
                null,
                emptyList(),
                null,
                listOf(distribution("SEED", "씨앗 심기", 1)),
                listOf(
                    PropagationStatistics("SEED", "종자", 1, BigDecimal("100"), BigDecimal("10"), "g", Coverage(1, 1)),
                ),
            ),
            WateringStatistics(
                2,
                null,
                null,
                0,
                null,
                0,
                null,
                emptyList(),
                null,
                listOf(distribution("ENOUGH", "충분", 2)),
                listOf(distribution("SPRINKLE", "살수", 2)),
            ),
            FertilizingStatistics(
                3,
                null,
                null,
                0,
                null,
                0,
                null,
                emptyList(),
                null,
                BigDecimal("3"),
                BigDecimal("1"),
                Coverage(3, 3),
                emptyList(),
                listOf(
                    MaterialCategoryStatistics("ORGANIC_FERTILIZER", "유기질비료", 3, BigDecimal("100"), BigDecimal("3"), BigDecimal("100")),
                ),
                listOf(distribution("SOIL", "토양시비", 3)),
                listOf(
                    CategoryMethodStatistics("ORGANIC_FERTILIZER", "유기질비료", "SOIL", "토양시비", 3, BigDecimal("100")),
                ),
            ),
            PestControlStatistics(
                4,
                null,
                null,
                0,
                null,
                0,
                null,
                listOf(distribution("SUNNY", "맑음", 4)),
                null,
                listOf(distribution("FUNGICIDE", "살균제", 4)),
                listOf(
                    AmountByUnit("ml", BigDecimal("40"), Coverage(4, 4)),
                ),
                listOf(
                    CategoryAmountByUnit("FUNGICIDE", "살균제", "ml", 4, BigDecimal("40"), Coverage(4, 4)),
                ),
                BigDecimal("20"),
                Coverage(4, 4),
                listOf(TargetCount("진딧물", 4)),
            ),
            WeedingStatistics(5, null, null, 0, null, 0, null, emptyList(), null, listOf(distribution("HAND", "손제초", 5))),
            CommonOnlyStatistics(6, null, null, 0, null, 0, null, emptyList(), null),
            HarvestStatistics(
                7,
                null,
                null,
                0,
                null,
                0,
                null,
                emptyList(),
                null,
                BigDecimal("70"),
                BigDecimal("10"),
                Coverage(7, 7),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 15),
                listOf(
                    HarvestPartStatistics("ROOT_BARK", "뿌리·껍질", 7, BigDecimal("100"), BigDecimal("70"), BigDecimal("100"), Coverage(7, 7)),
                ),
                6,
                GrowthPeriodRange(5, 7),
                listOf(distribution("6", "6개월", 7)),
            ),
            CommonOnlyStatistics(8, null, null, 0, null, 0, null, emptyList(), null),
        )
    }
    private fun distribution(code: String, label: String, count: Int): CountDistribution {
        return CountDistribution(code, label, count, BigDecimal("100"))
    }

    private fun <T : BaseTimeEntity> persist(entity: T): T {
        setTimestamps(entity, workedAt)
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
