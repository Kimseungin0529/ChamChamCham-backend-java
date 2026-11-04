package com.chamchamcham.application.report;

import java.util.stream.Collectors;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
public class FarmingCyclePartitioner {
    public List<CycleSlice> partition(List<CycleReportSourceRecord> records) {
        var sorted = records.stream().sorted(Comparator.comparing(CycleReportSourceRecord::getWorkedAt)
            .thenComparing(CycleReportSourceRecord::getCreatedAt).thenComparing(CycleReportSourceRecord::getId)).collect(Collectors.toCollection(ArrayList::new));
        if (sorted.isEmpty()) return List.of();
        var result = new ArrayList<CycleSlice>();
        var current = new ArrayList<CycleReportSourceRecord>();
        boolean hasPreviousFinalHarvest = false;
        for (var record : sorted) {
            current.add(record);
            if (record.getHarvest() != null && record.getHarvest().isLastHarvest()) {
                result.add(new CycleSlice(FarmingCycleReportStatus.COMPLETED,
                    hasPreviousFinalHarvest ? FarmingCycleStartBasis.AFTER_PREVIOUS_FINAL_HARVEST : FarmingCycleStartBasis.FIRST_RECORD,
                    snapshot(current)));
                current.clear();
                hasPreviousFinalHarvest = true;
            }
        }
        if (!current.isEmpty()) {
            result.add(new CycleSlice(FarmingCycleReportStatus.ACTIVE,
                hasPreviousFinalHarvest ? FarmingCycleStartBasis.AFTER_PREVIOUS_FINAL_HARVEST : FarmingCycleStartBasis.FIRST_RECORD,
                snapshot(current)));
        }
        return result;
    }

    private List<CycleReportSourceRecord> snapshot(List<CycleReportSourceRecord> records) {
        return records.size() == 1 ? Collections.singletonList(records.getFirst()) : new ArrayList<>(records);
    }
}
