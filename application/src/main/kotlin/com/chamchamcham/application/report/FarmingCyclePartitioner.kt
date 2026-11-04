package com.chamchamcham.application.report

import com.chamchamcham.domain.report.FarmingCycleReportStatus
import com.chamchamcham.domain.report.FarmingCycleStartBasis
import org.springframework.stereotype.Component

@Component
class FarmingCyclePartitioner {
    fun partition(records: List<CycleReportSourceRecord>): List<CycleSlice> {
        val sorted = records.sortedWith(
            compareBy(
                CycleReportSourceRecord::workedAt,
                CycleReportSourceRecord::createdAt,
                CycleReportSourceRecord::id,
            ),
        )
        if (sorted.isEmpty()) return emptyList()

        val result = mutableListOf<CycleSlice>()
        val current = mutableListOf<CycleReportSourceRecord>()
        var hasPreviousFinalHarvest = false

        sorted.forEach { record ->
            current += record
            if (record.harvest?.isLastHarvest == true) {
                result += CycleSlice(
            FarmingCycleReportStatus.COMPLETED,
            if (hasPreviousFinalHarvest) {
                        FarmingCycleStartBasis.AFTER_PREVIOUS_FINAL_HARVEST
                    } else {
                        FarmingCycleStartBasis.FIRST_RECORD
                    },
            current.toList(),
        )
                current.clear()
                hasPreviousFinalHarvest = true
            }
        }

        if (current.isNotEmpty()) {
            result += CycleSlice(
            FarmingCycleReportStatus.ACTIVE,
            if (hasPreviousFinalHarvest) {
                    FarmingCycleStartBasis.AFTER_PREVIOUS_FINAL_HARVEST
                } else {
                    FarmingCycleStartBasis.FIRST_RECORD
                },
            current.toList(),
        )
        }
        return result
    }
}
