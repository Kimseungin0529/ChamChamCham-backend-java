package com.chamchamcham.application.coaching.common;

import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farming.FertilizerAmountUnit;
import com.chamchamcham.domain.farming.FertilizingMethod;
import com.chamchamcham.domain.farming.HarvestSource;
import com.chamchamcham.domain.farming.IrrigationAmount;
import com.chamchamcham.domain.farming.IrrigationMethod;
import com.chamchamcham.domain.farming.PesticideAmountUnit;
import com.chamchamcham.domain.farming.PlantingMethod;
import com.chamchamcham.domain.farming.PropagationMethod;
import com.chamchamcham.domain.farming.SeedAmountUnit;
import com.chamchamcham.domain.farming.SeedlingUnit;
import com.chamchamcham.domain.farming.WeedingMethod;
import com.chamchamcham.domain.farming.WorkType;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.util.*;
import com.chamchamcham.application.coaching.common.CoachingPromptLabelsKt;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContextKt;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

class CoachingPromptLabelsTest {

    @Test
    @DisplayName("maps every work type to an easy coaching label")
    void maps_every_work_type_to_an_easy_coaching_label() throws Exception {
        assertThat(associateWith(Arrays.asList(WorkType.values()), it -> CoachingPromptLabelsKt.toCoachingText(it))).containsExactlyEntriesOf(map(new AbstractMap.SimpleEntry<>(WorkType.PLANTING, "심기"), new AbstractMap.SimpleEntry<>(WorkType.WATERING, "물 주기"), new AbstractMap.SimpleEntry<>(WorkType.FERTILIZING, "거름 주기"), new AbstractMap.SimpleEntry<>(WorkType.PEST_CONTROL, "병이나 벌레 관리"), new AbstractMap.SimpleEntry<>(WorkType.WEEDING, "풀 뽑기"), new AbstractMap.SimpleEntry<>(WorkType.PRUNING, "가지 정리"), new AbstractMap.SimpleEntry<>(WorkType.HARVEST, "수확"), new AbstractMap.SimpleEntry<>(WorkType.ETC, "기타 작업")));
    }

    @Test
    @DisplayName("maps every planting value to an easy coaching label")
    void maps_every_planting_value_to_an_easy_coaching_label() throws Exception {
        assertThat(associateWith(Arrays.asList(PlantingMethod.values()), it -> CoachingPromptLabelsKt.toCoachingText(it))).containsExactlyEntriesOf(map(new AbstractMap.SimpleEntry<>(PlantingMethod.SEED, "씨앗을 심음"), new AbstractMap.SimpleEntry<>(PlantingMethod.SEEDLING, "모종을 심음")));
        assertThat(associateWith(Arrays.asList(PropagationMethod.values()), it -> CoachingPromptLabelsKt.toCoachingText(it))).containsExactlyEntriesOf(map(new AbstractMap.SimpleEntry<>(PropagationMethod.CUTTING, "가지를 잘라 심음"), new AbstractMap.SimpleEntry<>(PropagationMethod.GRAFTING, "서로 다른 줄기를 이어 붙임"), new AbstractMap.SimpleEntry<>(PropagationMethod.LAYERING, "가지를 흙에 묻어 뿌리내림"), new AbstractMap.SimpleEntry<>(PropagationMethod.DIVISION, "포기를 나눠 심음"), new AbstractMap.SimpleEntry<>(PropagationMethod.TISSUE_CULTURE, "작은 조직을 키운 모종을 심음"), new AbstractMap.SimpleEntry<>(PropagationMethod.PURCHASED, "구입한 모종을 심음")));
        assertThat(associateWith(Arrays.asList(SeedAmountUnit.values()), it -> CoachingPromptLabelsKt.toCoachingText(it))).containsExactlyEntriesOf(map(new AbstractMap.SimpleEntry<>(SeedAmountUnit.G, "그램")));
        assertThat(associateWith(Arrays.asList(SeedlingUnit.values()), it -> CoachingPromptLabelsKt.toCoachingText(it))).containsExactlyEntriesOf(map(new AbstractMap.SimpleEntry<>(SeedlingUnit.JU, "포기")));
    }

    @Test
    @DisplayName("maps every watering value to an easy coaching label")
    void maps_every_watering_value_to_an_easy_coaching_label() throws Exception {
        assertThat(associateWith(Arrays.asList(IrrigationAmount.values()), it -> CoachingPromptLabelsKt.toCoachingText(it))).containsExactlyEntriesOf(map(new AbstractMap.SimpleEntry<>(IrrigationAmount.LOW, "적은 양"), new AbstractMap.SimpleEntry<>(IrrigationAmount.NORMAL, "보통 양"), new AbstractMap.SimpleEntry<>(IrrigationAmount.SUFFICIENT, "넉넉한 양")));
        assertThat(associateWith(Arrays.asList(IrrigationMethod.values()), it -> CoachingPromptLabelsKt.toCoachingText(it))).containsExactlyEntriesOf(map(new AbstractMap.SimpleEntry<>(IrrigationMethod.DRIP, "호스로 조금씩 물을 줌"), new AbstractMap.SimpleEntry<>(IrrigationMethod.SPRAYING, "물을 넓게 뿌려 줌"), new AbstractMap.SimpleEntry<>(IrrigationMethod.ETC, "다른 방법으로 물을 줌")));
    }

    @Test
    @DisplayName("maps every fertilizing value to an easy coaching label")
    void maps_every_fertilizing_value_to_an_easy_coaching_label() throws Exception {
        assertThat(associateWith(Arrays.asList(FertilizingMethod.values()), it -> CoachingPromptLabelsKt.toCoachingText(it))).containsExactlyEntriesOf(map(new AbstractMap.SimpleEntry<>(FertilizingMethod.SOIL, "흙에 거름을 줌"), new AbstractMap.SimpleEntry<>(FertilizingMethod.FOLIAR, "잎에 거름물을 뿌림")));
        assertThat(associateWith(Arrays.asList(FertilizerAmountUnit.values()), it -> CoachingPromptLabelsKt.toCoachingText(it))).containsExactlyEntriesOf(map(new AbstractMap.SimpleEntry<>(FertilizerAmountUnit.G, "그램"), new AbstractMap.SimpleEntry<>(FertilizerAmountUnit.ML, "밀리리터")));
    }

    @Test
    @DisplayName("maps every pest control value to an easy coaching label")
    void maps_every_pest_control_value_to_an_easy_coaching_label() throws Exception {
        assertThat(associateWith(Arrays.asList(PesticideAmountUnit.values()), it -> CoachingPromptLabelsKt.toCoachingText(it))).containsExactlyEntriesOf(map(new AbstractMap.SimpleEntry<>(PesticideAmountUnit.ML, "밀리리터"), new AbstractMap.SimpleEntry<>(PesticideAmountUnit.G, "그램")));
        assertThat(associateWith(java.util.Arrays.asList("L", "ML", "UNKNOWN"), it -> CoachingPromptLabelsKt.toSprayAmountCoachingText(it))).containsExactlyEntriesOf(map(new AbstractMap.SimpleEntry<>("L", "리터"), new AbstractMap.SimpleEntry<>("ML", "밀리리터"), new AbstractMap.SimpleEntry<>("UNKNOWN", null)));
    }

    @Test
    @DisplayName("maps every weeding and harvest value to an easy coaching label")
    void maps_every_weeding_and_harvest_value_to_an_easy_coaching_label() throws Exception {
        assertThat(associateWith(Arrays.asList(WeedingMethod.values()), it -> CoachingPromptLabelsKt.toCoachingText(it))).containsExactlyEntriesOf(map(new AbstractMap.SimpleEntry<>(WeedingMethod.HAND, "손으로 풀을 뽑음"), new AbstractMap.SimpleEntry<>(WeedingMethod.MACHINE, "기계로 풀을 정리함"), new AbstractMap.SimpleEntry<>(WeedingMethod.MULCHING, "덮개를 깔아 풀을 막음"), new AbstractMap.SimpleEntry<>(WeedingMethod.HERBICIDE, "약으로 풀을 없앰")));
        assertThat(associateWith(Arrays.asList(CropUsePartCategory.values()), it -> CoachingPromptLabelsKt.toCoachingText(it))).containsExactlyEntriesOf(map(new AbstractMap.SimpleEntry<>(CropUsePartCategory.WHOLE_HERB, "식물 전체"), new AbstractMap.SimpleEntry<>(CropUsePartCategory.ROOT_BARK, "뿌리와 껍질"), new AbstractMap.SimpleEntry<>(CropUsePartCategory.RHIZOME, "땅속줄기"), new AbstractMap.SimpleEntry<>(CropUsePartCategory.LEAF, "잎"), new AbstractMap.SimpleEntry<>(CropUsePartCategory.FLOWER, "꽃"), new AbstractMap.SimpleEntry<>(CropUsePartCategory.FRUIT, "열매"), new AbstractMap.SimpleEntry<>(CropUsePartCategory.SEED, "씨앗"), new AbstractMap.SimpleEntry<>(CropUsePartCategory.STEM_BRANCH, "줄기와 가지"), new AbstractMap.SimpleEntry<>(CropUsePartCategory.UNKNOWN, "기타 부위")));
        assertThat(associateWith(Arrays.asList(HarvestSource.values()), it -> CoachingPromptLabelsKt.toCoachingText(it))).containsExactlyEntriesOf(map(new AbstractMap.SimpleEntry<>(HarvestSource.CULTIVATED, "밭에서 기름"), new AbstractMap.SimpleEntry<>(HarvestSource.FORAGED, "산이나 들에서 얻음")));
        assertThat(associateWith(java.util.Arrays.asList("YEAR", "MONTH", "UNKNOWN"), it -> CoachingPromptLabelsKt.toGrowthPeriodCoachingText(it))).containsExactlyEntriesOf(map(new AbstractMap.SimpleEntry<>("YEAR", "년"), new AbstractMap.SimpleEntry<>("MONTH", "개월"), new AbstractMap.SimpleEntry<>("UNKNOWN", null)));
    }

    @Test
    @DisplayName("coaching labels never expose English or raw enum names")
    void coaching_labels_never_expose_English_or_raw_enum_names() throws Exception {
        var labels = new ArrayList<String>();
        labels.addAll(Arrays.asList(WorkType.values()).stream().map(it -> CoachingPromptLabelsKt.toCoachingText(it)).toList());
        labels.addAll(Arrays.asList(PlantingMethod.values()).stream().map(it -> CoachingPromptLabelsKt.toCoachingText(it)).toList());
        labels.addAll(Arrays.asList(PropagationMethod.values()).stream().map(it -> CoachingPromptLabelsKt.toCoachingText(it)).toList());
        labels.addAll(Arrays.asList(IrrigationAmount.values()).stream().map(it -> CoachingPromptLabelsKt.toCoachingText(it)).toList());
        labels.addAll(Arrays.asList(IrrigationMethod.values()).stream().map(it -> CoachingPromptLabelsKt.toCoachingText(it)).toList());
        labels.addAll(Arrays.asList(FertilizingMethod.values()).stream().map(it -> CoachingPromptLabelsKt.toCoachingText(it)).toList());
        labels.addAll(Arrays.asList(WeedingMethod.values()).stream().map(it -> CoachingPromptLabelsKt.toCoachingText(it)).toList());
        labels.addAll(Arrays.asList(CropUsePartCategory.values()).stream().map(it -> CoachingPromptLabelsKt.toCoachingText(it)).toList());
        labels.addAll(Arrays.asList(HarvestSource.values()).stream().map(it -> CoachingPromptLabelsKt.toCoachingText(it)).toList());
        assertThat(labels).allMatch(it -> !CoachingTextPolicy.INSTANCE.INSTANCE.containsEnglishLetter(it));
        Arrays.asList(WorkType.values()).forEach(value -> assertThat(CoachingPromptLabelsKt.toCoachingText(value)).isNotEqualTo(value.name()));
    }
    @SafeVarargs
    private static <K, V> Map<K, V> map(Map.Entry<K, V>... entries) {
        Map<K, V> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

    private static <T> Map<T, String> associateWith(List<T> values, java.util.function.Function<T, String> action) {
        Map<T, String> result = new LinkedHashMap<>();
        for (T value : values) result.put(value, action.apply(value));
        return result;
    }

}
