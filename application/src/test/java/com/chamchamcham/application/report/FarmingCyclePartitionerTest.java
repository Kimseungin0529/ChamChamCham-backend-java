package com.chamchamcham.application.report;

import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.HarvestSource;
import org.junit.jupiter.api.DisplayName;

class FarmingCyclePartitionerTest {
    private final FarmingCyclePartitioner partitioner = new FarmingCyclePartitioner();

    @Test
    @DisplayName("final harvest closes its cycle and partial harvest does not")
    void isFinal_harvest_closes_its_cycle_and_partial_harvest_does_not() throws Exception {
        var records = java.util.Arrays.asList(record("01", 1, WorkType.FERTILIZING, null, (1)), harvest("02", 10, false), harvest("03", 12, true), record("04", 20, WorkType.WATERING, null, (20)));
        var slices = partitioner.partition(records);
        assertThat(slices).hasSize(2);
        assertThat(slices.get(0).getStatus()).isEqualTo(FarmingCycleReportStatus.COMPLETED);
        assertThat(slices.get(0).getRecords().stream().map(it -> it.getId()).toList()).containsExactly(id("01"), id("02"), id("03"));
        assertThat(slices.get(1).getStatus()).isEqualTo(FarmingCycleReportStatus.ACTIVE);
        assertThat(slices.get(1).getStartBasis()).isEqualTo(FarmingCycleStartBasis.AFTER_PREVIOUS_FINAL_HARVEST);
    }

    @Test
    @DisplayName("work before planting and ETC stay inside the cycle")
    void work_before_planting_and_ETC_stay_inside_the_cycle() throws Exception {
        var records = java.util.Arrays.asList(record("01", 1, WorkType.ETC, null, (1)), record("02", 3, WorkType.PLANTING, null, (3)), harvest("03", 90, true));
        assertThat(partitioner.partition(records).get(0).getRecords().stream().map(it -> it.getWorkType()).toList()).containsExactly(WorkType.ETC, WorkType.PLANTING, WorkType.HARVEST);
    }

    @Test
    @DisplayName("equal worked timestamps are ordered by created time then id")
    void equal_worked_timestamps_are_ordered_by_created_time_then_id() throws Exception {
        var oldestCreatedId = id("03");
        var newerCreatedId = id("01");
        var newestCreatedId = id("02");
        var equalWorkedAtRecords = java.util.Arrays.asList(record("02", 1, WorkType.WATERING, null, 3), record("03", 1, WorkType.WATERING, null, 2), record("01", 1, WorkType.WATERING, null, 3));
        assertThat(partitioner.partition(equalWorkedAtRecords).stream().flatMap(x -> CycleSlice.class.cast(x).getRecords().stream()).toList().stream().map(it -> it.getId()).toList()).containsExactly(oldestCreatedId, newerCreatedId, newestCreatedId);
    }
    private CycleReportSourceRecord harvest(String number, long day, boolean isFinal) {
        return record(number, day, WorkType.HARVEST, new HarvestReportSource(null, new CategoryRef("ROOT_BARK", "뿌리·껍질"), new CategoryRef("CULTIVATED", "재배"), null, null, 12, isFinal), (day));
    }
    private CycleReportSourceRecord record(String number, long day, WorkType workType, HarvestReportSource harvest, long createdDay) {
        return new CycleReportSourceRecord(id(number), LocalDateTime.of(2026, 1, 1, 9, 0).plusDays(day), LocalDateTime.of(2026, 1, 1, 9, 0).plusDays(createdDay), workType, "맑음", 20, false, "", null, null, null, null, null, harvest);
    }
    private UUID id(String number) {
        return UUID.fromString("00000000-0000-0000-0000-0000000000" + number + "");
    }
}
