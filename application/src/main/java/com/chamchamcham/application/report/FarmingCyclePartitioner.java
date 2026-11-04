package com.chamchamcham.application.report;

import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class FarmingCyclePartitioner {
    public List<CycleSlice> partition(List<CycleReportSourceRecord> records) {
        var sorted = records.stream().sorted(Comparator.comparing(CycleReportSourceRecord::getWorkedAt)
            .thenComparing(CycleReportSourceRecord::getCreatedAt).thenComparing(CycleReportSourceRecord::getId)).toList();
        if (sorted.isEmpty()) return List.of();
        var result = new ArrayList<CycleSlice>();
        var current = new ArrayList<CycleReportSourceRecord>();
        boolean hasPreviousFinalHarvest = false;
        for (var record : sorted) {
            current.add(record);
            if (record.getHarvest() != null && record.getHarvest().isLastHarvest()) {
                result.add(new CycleSlice(FarmingCycleReportStatus.COMPLETED,
                    hasPreviousFinalHarvest ? FarmingCycleStartBasis.AFTER_PREVIOUS_FINAL_HARVEST : FarmingCycleStartBasis.FIRST_RECORD,
                    new ArrayList<>(current)));
                current.clear();
                hasPreviousFinalHarvest = true;
            }
        }
        if (!current.isEmpty()) {
            result.add(new CycleSlice(FarmingCycleReportStatus.ACTIVE,
                hasPreviousFinalHarvest ? FarmingCycleStartBasis.AFTER_PREVIOUS_FINAL_HARVEST : FarmingCycleStartBasis.FIRST_RECORD,
                new ArrayList<>(current)));
        }
        return result;
    }
}
