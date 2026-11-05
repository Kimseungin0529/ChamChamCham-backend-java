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
import java.util.Objects;

public final class CoachingPromptLabels {
    private CoachingPromptLabels() {}
    @org.springframework.lang.NonNull public static String toCoachingText(WorkType value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case PLANTING -> "심기";
            case WATERING -> "물 주기";
            case FERTILIZING -> "거름 주기";
            case PEST_CONTROL -> "병이나 벌레 관리";
            case WEEDING -> "풀 뽑기";
            case PRUNING -> "가지 정리";
            case HARVEST -> "수확";
            case ETC -> "기타 작업";
        };
    }
    @org.springframework.lang.NonNull public static String toCoachingText(PlantingMethod value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case SEED -> "씨앗을 심음";
            case SEEDLING -> "모종을 심음";
        };
    }
    @org.springframework.lang.NonNull public static String toCoachingText(PropagationMethod value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case CUTTING -> "가지를 잘라 심음";
            case GRAFTING -> "서로 다른 줄기를 이어 붙임";
            case LAYERING -> "가지를 흙에 묻어 뿌리내림";
            case DIVISION -> "포기를 나눠 심음";
            case TISSUE_CULTURE -> "작은 조직을 키운 모종을 심음";
            case PURCHASED -> "구입한 모종을 심음";
        };
    }
    @org.springframework.lang.NonNull public static String toCoachingText(SeedAmountUnit value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case G -> "그램";
        };
    }
    @org.springframework.lang.NonNull public static String toCoachingText(SeedlingUnit value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case JU -> "포기";
        };
    }
    @org.springframework.lang.NonNull public static String toCoachingText(IrrigationAmount value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case LOW -> "적은 양";
            case NORMAL -> "보통 양";
            case SUFFICIENT -> "넉넉한 양";
        };
    }
    @org.springframework.lang.NonNull public static String toCoachingText(IrrigationMethod value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case DRIP -> "호스로 조금씩 물을 줌";
            case SPRAYING -> "물을 넓게 뿌려 줌";
            case ETC -> "다른 방법으로 물을 줌";
        };
    }
    @org.springframework.lang.NonNull public static String toCoachingText(FertilizerAmountUnit value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case G -> "그램";
            case ML -> "밀리리터";
        };
    }
    @org.springframework.lang.NonNull public static String toCoachingText(FertilizingMethod value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case SOIL -> "흙에 거름을 줌";
            case FOLIAR -> "잎에 거름물을 뿌림";
        };
    }
    @org.springframework.lang.NonNull public static String toCoachingText(PesticideAmountUnit value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case ML -> "밀리리터";
            case G -> "그램";
        };
    }
    @org.springframework.lang.Nullable public static String toSprayAmountCoachingText(String value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case "L" -> "리터";
            case "ML" -> "밀리리터";
            default -> null;
        };
    }
    @org.springframework.lang.NonNull public static String toCoachingText(WeedingMethod value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case HAND -> "손으로 풀을 뽑음";
            case MACHINE -> "기계로 풀을 정리함";
            case MULCHING -> "덮개를 깔아 풀을 막음";
            case HERBICIDE -> "약으로 풀을 없앰";
        };
    }
    @org.springframework.lang.NonNull public static String toCoachingText(CropUsePartCategory value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case WHOLE_HERB -> "식물 전체";
            case ROOT_BARK -> "뿌리와 껍질";
            case RHIZOME -> "땅속줄기";
            case LEAF -> "잎";
            case FLOWER -> "꽃";
            case FRUIT -> "열매";
            case SEED -> "씨앗";
            case STEM_BRANCH -> "줄기와 가지";
            case UNKNOWN -> "기타 부위";
        };
    }
    @org.springframework.lang.NonNull public static String toCoachingText(HarvestSource value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case CULTIVATED -> "밭에서 기름";
            case FORAGED -> "산이나 들에서 얻음";
        };
    }
    @org.springframework.lang.Nullable public static String toGrowthPeriodCoachingText(String value) {
        Objects.requireNonNull(value, "value");
        return switch (value) {
            case "YEAR" -> "년";
            case "MONTH" -> "개월";
            default -> null;
        };
    }
}
