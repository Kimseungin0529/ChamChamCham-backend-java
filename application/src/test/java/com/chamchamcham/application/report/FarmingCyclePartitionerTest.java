package com.chamchamcham.application.report;

import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.report.FarmingCycleReportStatus;
import com.chamchamcham.domain.report.FarmingCycleStartBasis;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;

class FarmingCyclePartitionerTest {
    private final FarmingCyclePartitioner partitioner = new FarmingCyclePartitioner();

    @Test
    @DisplayName("final harvest closes its cycle and partial harvest does not")
    void isFinalHarvestClosesItsCycleAndPartialHarvestDoesNot() throws Exception {
        var records = java.util.Arrays.asList(record("01", 1, WorkType.FERTILIZING, null, (1)), harvest("02", 10, false), harvest("03", 12, true),
            record("04", 20, WorkType.WATERING, null, (20)));
        var slices = partitioner.partition(records);
        assertThat(slices).hasSize(2);
        assertThat(slices.get(0).getStatus()).isEqualTo(FarmingCycleReportStatus.COMPLETED);
        assertThat(slices.get(0).getRecords().stream().map(it -> it.getId()).toList()).containsExactly(id("01"), id("02"), id("03"));
        assertThat(slices.get(1).getStatus()).isEqualTo(FarmingCycleReportStatus.ACTIVE);
        assertThat(slices.get(1).getStartBasis()).isEqualTo(FarmingCycleStartBasis.AFTER_PREVIOUS_FINAL_HARVEST);
    }

    @Test
    @DisplayName("work before planting and ETC stay inside the cycle")
    void workBeforePlantingAndETCStayInsideTheCycle() throws Exception {
        var records = java.util.Arrays.asList(record("01", 1, WorkType.ETC, null, (1)), record("02", 3, WorkType.PLANTING, null, (3)), harvest("03", 90, true));
        assertThat(single(partitioner.partition(records)).getRecords().stream().map(it -> it.getWorkType()).toList()).containsExactly(WorkType.ETC,
            WorkType.PLANTING, WorkType.HARVEST);
    }

    @Test
    @DisplayName("equal worked timestamps are ordered by created time then id")
    void equalWorkedTimestampsAreOrderedByCreatedTimeThenId() throws Exception {
        var oldestCreatedId = id("03");
        var newerCreatedId = id("01");
        var newestCreatedId = id("02");
        var equalWorkedAtRecords = java.util.Arrays.asList(record("02", 1, WorkType.WATERING, null, 3), record("03", 1, WorkType.WATERING, null, 2),
            record("01", 1, WorkType.WATERING, null, 3));
        assertThat(partitioner.partition(equalWorkedAtRecords).stream().flatMap(x -> CycleSlice.class.cast(x).getRecords().stream()).toList().stream().map(it -> it.getId()).toList()).containsExactly(oldestCreatedId,
            newerCreatedId, newestCreatedId);
    }
    private CycleReportSourceRecord harvest(String number, long day, boolean isFinal) {
        return record(number, day, WorkType.HARVEST, new HarvestReportSource(null, new CategoryRef("ROOT_BARK", "뿌리·껍질"), new CategoryRef("CULTIVATED",
            "재배"), null, null, 12, isFinal), (day));
    }
    private CycleReportSourceRecord record(String number, long day, WorkType workType, HarvestReportSource harvest, long createdDay) {
        return new CycleReportSourceRecord(id(number), LocalDateTime.of(2026, 1, 1, 9, 0).plusDays(day), LocalDateTime.of(2026, 1, 1, 9,
            0).plusDays(createdDay), workType, "맑음", 20, false, "", null, null, null, null, null, harvest);
    }
    private UUID id(String number) {
        return UUID.fromString("00000000-0000-0000-0000-0000000000" + number + "");
    }

    @Test
    void reportSourceJsonRetainsOmittedDefaultsAndRejectsExplicitNulls() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(new com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        var json = "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"workedAt\":\"2026-01-01T09:00:00\",\"workType\":\"WATERING\",\"weatherCondition\":\"맑음\",\"weatherTemperature\":20,\"hasPhoto\":false}";
        var source = mapper.readValue(json, CycleReportSourceRecord.class);
        assertThat(source.getCreatedAt()).isEqualTo(source.getWorkedAt());
        assertThat(source.getMemo()).isEmpty();
        var prefix = json.substring(0, json.length() - 1);
        org.junit.jupiter.api.Assertions.assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class,
            () -> mapper.readValue(prefix + ",\"createdAt\":null}", CycleReportSourceRecord.class));
        org.junit.jupiter.api.Assertions.assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class,
            () -> mapper.readValue(prefix + ",\"memo\":null}", CycleReportSourceRecord.class));
        var harvest = mapper.readValue("{\"growthPeriodMonths\":null,\"isLastHarvest\":true}", HarvestReportSource.class);
        assertThat(harvest.getHarvestSource()).isEqualTo(new CategoryRef("CULTIVATED", "재배"));
        assertThat(harvest.isLastHarvest()).isTrue();
        assertThat(mapper.valueToTree(harvest).has("lastHarvest")).isFalse();
        assertThat(mapper.valueToTree(harvest).get("isLastHarvest").booleanValue()).isTrue();
        org.junit.jupiter.api.Assertions.assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class,
            () -> mapper.readValue("{\"growthPeriodMonths\":null,\"isLastHarvest\":true,\"harvestSource\":null}", HarvestReportSource.class));
    }

    @Test
    void searchConditionJsonRetainsMissingCollectionDefaults() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(new com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        var json = "{\"memberId\":\"00000000-0000-0000-0000-000000000001\",\"cursor\":null,\"size\":20}";
        var farming = mapper.readValue(json, com.chamchamcham.application.farming.FarmingRecordSearchCondition.class);
        assertThat(farming.getCropIds()).isEmpty();
        assertThat(farming.getWorkTypes()).isEmpty();
        assertThat(farming.getKeyword()).isNull();
        var cycle = mapper.readValue(json, FarmingCycleReportSearchCondition.class);
        assertThat(cycle.getFarmIds()).isEmpty();
        assertThat(cycle.getCropIds()).isEmpty();
        var work = mapper.readValue(json, FarmingWorkReportSearchCondition.class);
        assertThat(work.getFarmIds()).isEmpty();
        assertThat(work.getCropIds()).isEmpty();
        assertThat(work.getWorkTypes()).isEmpty();
        var invalid = json.substring(0, json.length() - 1) + ",\"cropIds\":null}";
        org.junit.jupiter.api.Assertions.assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class,
            () -> mapper.readValue(invalid, com.chamchamcham.application.farming.FarmingRecordSearchCondition.class));
    }
    @Test
    void cycleSliceKeepsDerivedHarvestIdAtConstructionWhileRetainingListAliasingAndValueEquality() {
        var records = new ArrayList<CycleReportSourceRecord>();
        records.add(record("01", 1, WorkType.WATERING, null, 1));
        var first = new CycleSlice(FarmingCycleReportStatus.ACTIVE, FarmingCycleStartBasis.FIRST_RECORD, records);
        records.add(harvest("02", 2, true));
        var second = new CycleSlice(FarmingCycleReportStatus.ACTIVE, FarmingCycleStartBasis.FIRST_RECORD, records);
        assertThat(first.getRecords()).isSameAs(records);
        assertThat(first.getRecords()).hasSize(2);
        assertThat(first.getFinalHarvestRecordId()).isNull();
        assertThat(second.getFinalHarvestRecordId()).isEqualTo(id("02"));
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        records.clear();
        assertThat(second.getFinalHarvestRecordId()).isEqualTo(id("02"));
    }

    private static <T> T single(List<T> values) {
        assertThat(values).hasSize(1);
        return values.getFirst();
    }

    @Test
    void cycleSliceJsonRetainsTheOriginalDerivedFieldPopulation() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(new com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build());
        var slice = mapper.readValue("{\"status\":\"ACTIVE\",\"startBasis\":\"FIRST_RECORD\",\"records\":[],\"finalHarvestRecordId\":\"00000000-0000-0000-0000-000000000001\"}", CycleSlice.class);
        assertThat(slice.getRecords()).isEmpty();
        assertThat(slice.getFinalHarvestRecordId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }


    @Test
    void partitionSnapshotsRetainSingletonAndMultiRecordMutabilityForBothStatuses() {
        var empty = partitioner.partition(List.of());
        assertThat(empty).isEmpty();
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> empty.add(null));
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> empty.remove(0));
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> empty.remove((Object) null));
        for (boolean completed : new boolean[] {false, true}) {
            for (int size : new int[] {1, 2}) {
                var last = completed ? harvest("02", 2, true) : record("02", 2, WorkType.WATERING, null, 2);
                var input = size == 1 ? List.of(last) : List.of(record("01", 1, WorkType.WATERING, null, 1), last);
                var slice = single(partitioner.partition(input));
                assertThat(slice.getStatus()).isEqualTo(completed ? FarmingCycleReportStatus.COMPLETED : FarmingCycleReportStatus.ACTIVE);
                var records = slice.getRecords();
                assertThat(records).containsExactlyElementsOf(input);
                assertThat(records.contains(null)).isFalse();
                assertThat(records.remove((Object) null)).isFalse();
                if (size == 1) {
                    org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> records.set(0, null));
                    org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> records.set(0, last));
                    org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> records.add(null));
                    org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> records.remove(0));
                    org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> records.remove(last));
                } else {
                    var first = records.set(0, null);
                    assertThat(first).isSameAs(input.getFirst());
                    assertThat(records.getFirst()).isNull();
                    records.set(0, first);
                    assertThat(records.add(null)).isTrue();
                    assertThat(records.remove((Object) null)).isTrue();
                    assertThat(records.remove(0)).isSameAs(first);
                    records.add(0, first);
                    assertThat(records).containsExactlyElementsOf(input);
                }
                assertThat(slice.getFinalHarvestRecordId()).isEqualTo(completed ? last.getId() : null);
            }
        }
    }

}
