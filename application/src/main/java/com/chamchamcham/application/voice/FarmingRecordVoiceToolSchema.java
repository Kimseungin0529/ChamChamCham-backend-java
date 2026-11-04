package com.chamchamcham.application.voice;

import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.farming.*;
import java.util.*;
import java.util.stream.Collectors;

public final class FarmingRecordVoiceToolSchema {
    private FarmingRecordVoiceToolSchema() {}

    public static Map<String, Object> build(List<FarmOption> farms, Map<String, List<CropOption>> cropsByFarm) {
        Objects.requireNonNull(farms, "farms");
        Objects.requireNonNull(cropsByFarm, "cropsByFarm");
        var cropOptions = new LinkedHashMap<java.util.UUID, CropOption>();
        for (var crops : cropsByFarm.values()) {
            for (var crop : crops) cropOptions.putIfAbsent(crop.getCropId(), crop);
        }
        return map(
            "type", "function",
            "name", "save_farming_record",
            "description",
                "사용자가 말한 영농 작업 내용을 영농일지 항목으로 구조화한다. " +
                "필수 항목은 farmId·cropId·workType·memo이며, 작업 유형별 상세(planting/harvest 등)는 " +
                "해당 유형일 때만 필요하다. 이 필수 항목이 누락됐으면 호출하지 말고 먼저 되물어라. " +
                "다만 WATERING·WEEDING·PRUNING·ETC(기타)처럼 별도 상세가 없는 유형은 필수 4개만 갖춰지면 " +
                "상세를 기다리지 말고 곧바로 이 도구를 호출한다.",
            "parameters", map(
                "type", "object",
                "properties", map(
                    "farmId", enumProperty("이 기록이 속한 농지 ID",
                        farms.stream().map(farm -> farm.getFarmId().toString()).collect(Collectors.toCollection(ArrayList::new))),
                    "cropId", enumProperty("이 기록이 속한 작물 ID",
                        cropOptions.values().stream().map(crop -> crop.getCropId().toString()).collect(Collectors.toCollection(ArrayList::new))),
                    "workType", enumProperty("작업 유형", names(WorkType.values())),
                    "workedAt", map(
                        "type", "string",
                        "format", "date-time",
                        "description", "작업 일시(ISO-8601). 사용자가 상대 시간을 언급하지 않으면 생략한다."),
                    "memo", map(
                        "type", "string",
                        "description", "작업 내용 요약(한국어 30자 이상 500자 이내). 사용자에게 메모를 불러 달라고 " +
                            "요청하지 말고, 대화 내용을 바탕으로 직접 작성한다."),
                    "planting", plantingSchema(),
                    "watering", wateringSchema(),
                    "fertilizing", fertilizingSchema(),
                    "pestControl", pestControlSchema(),
                    "weeding", weedingSchema(),
                    "harvest", harvestSchema()),
                "required", Arrays.asList("farmId", "cropId", "workType", "memo")));
    }

    private static Map<String, Object> plantingSchema() {
        return objectProperty(
        "심기 상세 (workType=PLANTING일 때 필수). plantingMethod=SEED(씨앗 심기)면 seedAmount/seedAmountUnit만 " +
            "채우고 seedlingCount/seedlingUnit/propagationMethod는 비운다. plantingMethod=SEEDLING(모종 심기)이면 " +
            "seedlingCount/seedlingUnit만 채우고(propagationMethod는 선택), seedAmount/seedAmountUnit은 비운다.",
        map(
            "plantingMethod", enumProperty("심기 방법(필수)", names(PlantingMethod.values())),
            "seedAmount", map("type", "number", "description", "파종량(plantingMethod=SEED일 때 필수)"),
            "seedAmountUnit", enumProperty("파종량 단위(plantingMethod=SEED일 때만)", names(SeedAmountUnit.values())),
            "seedlingCount", map("type", "integer", "description", "모종수(plantingMethod=SEEDLING일 때 필수)"),
            "seedlingUnit", enumProperty("모종수 단위(plantingMethod=SEEDLING일 때만)", names(SeedlingUnit.values())),
            "propagationMethod", enumProperty(
                "번식법(plantingMethod=SEEDLING일 때만 선택 입력 가능)",
                names(PropagationMethod.values()))));
    }

    private static Map<String, Object> wateringSchema() {
        return objectProperty(
        "관수 상세 (workType=WATERING일 때만)",
        map(
            "irrigationAmount", enumProperty("관수량", names(IrrigationAmount.values())),
            "irrigationMethod", enumProperty("관수 방법", names(IrrigationMethod.values()))));
    }

    private static Map<String, Object> fertilizingSchema() {
        return objectProperty(
        "시비 상세 (workType=FERTILIZING일 때 필수)",
        map(
            "materialName", map("type", "string", "description", "비료명(필수)"),
            "amount", map("type", "number", "description", "시비량(필수)"),
            "amountUnit", enumProperty("시비량 단위(필수)", names(FertilizerAmountUnit.values())),
            "applicationMethod", enumProperty("시비 방법", names(FertilizingMethod.values()))));
    }

    private static Map<String, Object> pestControlSchema() {
        return objectProperty(
        "병해충 방제 상세 (workType=PEST_CONTROL일 때 필수)",
        map(
            "pesticideName", map(
                "type", "string",
                "description", "농약명(필수). 사용자 발음이 부정확할 수 있으니 지침의 농약 목록에 비슷한 이름이 " +
                    "있으면 확인 후 그 정확한 명칭을 사용하고, 목록에 없으면 들리는 대로 기록한다."),
            "pesticideAmount", map("type", "number", "description", "농약량(필수)"),
            "pesticideAmountUnit", enumProperty("농약량 단위(필수)", names(PesticideAmountUnit.values())),
            "totalSprayAmount", map("type", "number", "description", "총 살포량(필수)"),
            "totalSprayAmountUnit", enumProperty("총 살포량 단위(필수)", names(SprayAmountUnit.values())),
            "pestTarget", map("type", "string", "description", "방제 대상 병해충")));
    }

    private static Map<String, Object> weedingSchema() {
        return objectProperty(
        "제초 상세 (workType=WEEDING일 때만)",
        map("weedingMethod", enumProperty("제초 방법", names(WeedingMethod.values()))));
    }

    private static Map<String, Object> harvestSchema() {
        return objectProperty(
        "수확 상세 (workType=HARVEST일 때 필수). 수확량은 항상 kg 기준이다. 사용자가 수확량을 모른다고 " +
            "말하면 harvestAmountUnknown=true로 설정하고 harvestAmount는 비운다. 수확 부위(medicinalPart)는 " +
            "선택 입력이다. 재배기간(growthPeriod)은 항상 개월 단위이며 필수 입력이다. " +
            "마지막 수확 여부(isLastHarvest)도 필수 입력이다.",
        map(
            "harvestAmount", map("type", "number", "description", "수확량(kg). 모르면 비운다."),
            "harvestAmountUnknown", map(
                "type", "boolean",
                "description", "수확량을 모르면 true. 이때 harvestAmount는 비운다."),
            "medicinalPart", enumProperty("수확 부위(선택)", names(CropUsePartCategory.values())),
            "harvestSource", enumProperty("수확 출처", names(HarvestSource.values())),
            "growthPeriod", map("type", "integer", "description", "재배기간(개월, 필수)"),
            "isLastHarvest", map(
                "type", "boolean",
                "description", "이 작물의 마지막 수확 여부(필수). 사용자가 말하지 않으면 예/아니오로 한 번만 묻는다.")));
    }

    private static Map<String, Object> enumProperty(String description, List<String> values) {
        return map("type", "string", "description", description, "enum", values);
    }
    private static Map<String, Object> objectProperty(String description, Map<String, Object> properties) {
        return map("type", "object", "description", description, "properties", properties);
    }
    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.toCollection(ArrayList::new));
    }
    private static Map<String, Object> map(Object... entries) {
        if (entries.length == 2) return Collections.singletonMap((String) entries[0], entries[1]);
        var map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < entries.length; i += 2) map.put((String) entries[i], entries[i + 1]);
        return map;
    }
}
