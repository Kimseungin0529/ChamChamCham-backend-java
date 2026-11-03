package com.chamchamcham.domain.report;

import com.chamchamcham.domain.farming.WorkType;
import org.junit.jupiter.api.Test;
import java.beans.Introspector;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class CycleReportStatisticsTest {
    private final List<String> commonPropertyNames = List.of(
        "recordCount", "firstWorkedOn", "lastWorkedOn", "workedDayCount", "averageIntervalDays",
        "photoAttachedRecordCount", "photoAttachmentRatePct", "weatherDistribution", "averageTemperatureC");

    @Test
    void emptyStatisticsContainEveryWorkTypeWithDirectCommonFields() {
        var result = CycleReportStatistics.empty();
        assertThat(result.getPlanting().getRecordCount()).isZero();
        assertThat(result.getWatering().getFirstWorkedOn()).isNull();
        assertThat(result.getFertilizing().getPhotoAttachmentRatePct()).isNull();
        assertThat(result.getPestControl().getWeatherDistribution()).isEmpty();
        assertThat(result.getWeeding().getMethodDistribution()).isEmpty();
        assertThat(result.getPruning().getRecordCount()).isZero();
        assertThat(result.getHarvest().getTotalAmountKg()).isNull();
        assertThat(result.getEtc().getRecordCount()).isZero();
        assertThat(propertyNames(CycleReportStatistics.class)).doesNotContain("common");
    }

    @Test
    void everyWorkStatisticsTypeDirectlyExposesExactCommonPropertyNames() {
        var result = CycleReportStatistics.empty();
        for (var statistics : List.of(result.getPlanting(), result.getWatering(), result.getFertilizing(), result.getPestControl(),
            result.getWeeding(), result.getPruning(), result.getHarvest(), result.getEtc())) {
            var names = propertyNames(statistics.getClass());
            assertThat(names).containsAll(commonPropertyNames);
            assertThat(names.stream().filter(commonPropertyNames::contains).toList()).containsExactlyInAnyOrderElementsOf(commonPropertyNames);
        }
    }

    @Test
    void emptyStatisticsPreserveValueEqualityAndWorkTypeLookups() {
        var first = CycleReportStatistics.empty();
        assertThat(first).isEqualTo(CycleReportStatistics.empty());
        assertThat(first.hashCode()).isEqualTo(CycleReportStatistics.empty().hashCode());
        for (var workType : WorkType.values()) {
            assertThat(first.recordCountFor(workType)).isZero();
            assertThat(first.lastWorkedOnFor(workType)).isNull();
        }
    }

    private List<String> propertyNames(Class<?> type) {
        try {
            return Arrays.stream(Introspector.getBeanInfo(type, Object.class).getPropertyDescriptors())
                .map(PropertyDescriptor::getName).toList();
        } catch (IntrospectionException e) { throw new AssertionError(e); }
    }
}
