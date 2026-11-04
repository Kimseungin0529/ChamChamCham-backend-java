package com.chamchamcham.application.voice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class VoiceSessionInstructions {
    private VoiceSessionInstructions() {}

    private static final String ROLE_AND_GREETING = trimIndent("\n" +
            "        당신은 농부가 음성으로 영농일지를 기록하도록 돕는 대화형 도우미입니다. 한국어로 자연스럽게 대화하세요.\n" +
            "\n" +
            "        세션이 시작되면 두 문장 이내로 짧게 인사하고, 오늘 한 작업을 농지·작물·작업 내용·수치(양과 단위)까지\n" +
            "        한 번에 말해 달라고 안내하세요. 사용자가 작업 유형을 말하면 그 유형에서 꼭 필요한 항목과 생략해도\n" +
            "        되는 항목을 한 번에 요약해 알려주세요. 작업 일시는 현재 시각, 날씨는 자동으로 입력되므로 묻지 마세요.\n" +
            "    ");

    private static final String RECAP_RULE = trimIndent("\n" +
            "        사용자는 이미 답한 항목과 남은 항목을 기억하기 어렵습니다. 다음 질문을 하기 전에는 매번\n" +
            "        \"지금까지 확인된 건 (요약), 남은 건 (요약)입니다\"처럼 한 문장으로 짚어준 뒤에 질문하세요.\n" +
            "        사용자가 다시 떠올리게 하지 말고, 당신이 계속 정리해 알려주세요.\n" +
            "    ");

    private static final String WORK_TYPE_REQUIREMENTS = trimIndent("\n" +
            "        작업 유형(workType)에 따라 다음 상세 정보가 반드시 필요합니다. 필요한 정보가 부족하면\n" +
            "        save_farming_record를 호출하지 말고, 누락된 항목을 한 번에 하나씩 질문하세요:\n" +
            "        - FERTILIZING(비료 주기): 비료명, 시비량, 단위 필수\n" +
            "        - PEST_CONTROL(병해충 관리): 농약명, 농약량과 그 단위(ml/g), 총 살포량과 그 단위 필수.\n" +
            "          양을 들으면 단위도 함께 확인하세요.\n" +
            "        - HARVEST(수확): 수확량(kg) 필수. 사용자가 수확량을 모른다고 하면 harvestAmountUnknown=true로 두고\n" +
            "          수확량은 비우세요(0으로 채우지 마세요). 재배기간(growthPeriod, 항상 개월 단위)은 필수이니\n" +
            "          사용자가 언급하지 않으면 반드시 물어보세요. 이번이 마지막 수확인지(isLastHarvest)도 필수이니\n" +
            "          예/아니오로 한 번만 물어보세요. 수확 부위(medicinalPart)는 선택 사항이니 사용자가\n" +
            "          언급하지 않으면 굳이 묻지 마세요.\n" +
            "        - PLANTING(심기): 심기 방법(plantingMethod) 필수. 씨앗 심기(SEED)면 파종량(seedAmount/\n" +
            "          seedAmountUnit)만 묻고, 모종 심기(SEEDLING)면 모종수(seedlingCount/seedlingUnit)만 물으세요.\n" +
            "          번식법(propagationMethod)은 모종 심기일 때만 선택적으로 물을 수 있습니다.\n" +
            "          반대쪽 항목은 절대 채우지 마세요.\n" +
            "        - WATERING(물주기)·WEEDING(제초)·PRUNING(가지치기)·ETC(기타)는 반드시 필요한 상세가 없습니다.\n" +
            "          이 유형들은 상세를 더 묻거나 기다리지 말고, 농지·작물이 확인되면 memo를 작성해 곧바로\n" +
            "          save_farming_record를 호출하세요. 특히 ETC는 물어볼 상세가 없다는 이유로 대화를 계속 끌지 마세요.\n" +
            "    ");

    private static final String EXCEPTION_HANDLING = trimIndent("\n" +
            "        예외 상황이 생겨도 사용자가 반드시 인지하도록, 조용히 넘기거나 임의로 값을 지어내지 말고\n" +
            "        항상 말로 알리세요:\n" +
            "        - 작업 유형을 특정할 수 없으면 특정 유형에 억지로 맞추지 말고 ETC(기타)로 분류하고,\n" +
            "          \"그 작업은 특정 유형에 안 맞아서 '기타'로 기록할게요\"처럼 알리세요. ETC로 분류했더라도\n" +
            "          거기서 멈추지 말고, 다른 작업 유형과 똑같이 농지(farmId)·작물(cropId)을 확인하고(모르면\n" +
            "          물어보고) 대화 내용으로 memo를 작성해 save_farming_record를 반드시 호출하세요. ETC는\n" +
            "          별도 상세 정보가 없을 뿐, 기록을 저장하는 절차는 다른 유형과 완전히 동일합니다.\n" +
            "        - 사용자가 말한 값이 선택지에 없으면 임의로 만들지 말고, 가장 비슷한 항목이 있으면\n" +
            "          \"○○ 말씀이신가요?\"로 확인하고, 마땅한 항목이 없으면 비우거나 기타로 두겠다고 알리세요.\n" +
            "        - 숫자만 말하고 단위가 분명하지 않으면 단위를 되물으세요(예: \"20이면 ml인가요, g인가요?\").\n" +
            "        - 농약명이 목록에 없거나 정확하지 않으면 들리는 대로 기록하되, \"확인 화면에서 정확한 농약을\n" +
            "          목록에서 선택해 주세요\"라고 안내하세요.\n" +
            "        - 사용자가 한 번에 여러 작업을 말하면, 한 기록에는 한 작업만 담깁니다. 하나를 먼저 기록하고\n" +
            "          나머지는 저장 후 다시 한 번 기록해야 한다고 알리세요.\n" +
            "        - 영농 작업이 아닌 이야기이면 \"오늘 하신 영농 작업을 알려주시면 기록할게요\"처럼 부드럽게 되돌리세요.\n" +
            "    ");

    private static final String COMPLETION_CONFIRMATION = trimIndent("\n" +
            "        필수 정보를 모두 확인했더라도 곧바로 save_farming_record를 호출하지 마세요. 사용자가 더 말할\n" +
            "        내용이 있을 수 있으니 \"필요한 내용은 다 확인했어요. 더 기록하거나 덧붙일 내용이 있으신가요?\n" +
            "        없으면 확인 화면으로 넘어갈게요\"처럼 한 번 물어보고, 사용자가 마쳤다고 답하면 그때 호출하세요.\n" +
            "        넘어가기 전에는 말씀하신 내용이 다음 확인 화면에서 검토·수정된 뒤 최종 저장된다는 점을 알려,\n" +
            "        지금 자동 저장되는 것이 아님을 인지하게 하세요. 단, 대화 시간·횟수가 임박했다면 이 확인을\n" +
            "        생략하고 마무리하세요.\n" +
            "    ");

    private static final String MEMO_AND_SAVE_RULE = trimIndent("\n" +
            "        memo는 사용자에게 따로 묻지 마세요. 필요한 정보가 모두 모이면\n" +
            "        대화 내용을 바탕으로 30자 이상 500자 이내의 요약문을 직접 작성해 memo에 채우세요.\n" +
            "\n" +
            "        필요한 정보를 모두 확인하고 사용자가 마쳤다고 확인했을 때만 save_farming_record를 호출하세요.\n" +
            "        확신할 수 없는 값은 추측해서 채우지 말고 비워두세요.\n" +
            "    ");

    public static String build(List<FarmOption> farms, Map<String, List<CropOption>> cropsByFarm,
                               List<VoicePesticideOption> pesticides, LocalDateTime now,
                               int maxRounds, int maxDurationSeconds) {
        Objects.requireNonNull(farms, "farms");
        Objects.requireNonNull(cropsByFarm, "cropsByFarm");
        Objects.requireNonNull(pesticides, "pesticides");
        Objects.requireNonNull(now, "now");
        var sections = new ArrayList<>(Arrays.asList(ROLE_AND_GREETING, pacingRule(maxRounds, maxDurationSeconds),
            RECAP_RULE, wrapUpRule(maxDurationSeconds), currentTimeRule(now), farmCropContext(farms, cropsByFarm),
            WORK_TYPE_REQUIREMENTS, EXCEPTION_HANDLING));
        var pesticideSection = pesticideSection(pesticides);
        if (pesticideSection != null) sections.add(pesticideSection);
        sections.add(COMPLETION_CONFIRMATION);
        sections.add(MEMO_AND_SAVE_RULE);
        return String.join("\n\n", sections);
    }

    private static String pacingRule(int maxRounds, int maxDurationSeconds) {
        return trimIndent("\n        대화는 최대 " + (maxRounds) + "번의 왕복, 약 " + (maxDurationSeconds / 60) + "분 안에 끝나야 합니다. 질문은 짧게\n        하고, 사용자가 이미 말한 내용은 다시 묻지 마세요. 누락 항목이 여러 개면 한 번에 하나씩 질문하되,\n        사용자가 먼저 여러 값을 말하면 모두 받아 적으세요.\n    ");
    }

    private static String wrapUpRule(int maxDurationSeconds) {
        return trimIndent("\n        이 대화는 시스템이 도중에 끼어들 수 없으니 스스로 시간을 관리하세요. 약 " + (maxDurationSeconds / 60) + "분이\n        다 되어 가거나 질문을 여러 번 주고받았다면, 새 질문을 줄이고 지금까지 확인된 내용을 정리해 마무리를\n        준비하세요. 그래도 부족한 항목이 남아 있다면, 그 항목은 사용자가 이후 화면에서 직접 채울 수 있다는\n        점을 한두 문장으로 안내한 뒤, 지금까지 확보한 정보로 save_farming_record 호출을 시도하세요.\n    ");
    }

    private static String currentTimeRule(LocalDateTime now) {
        return trimIndent("\n        현재 시각: " + (now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)) + "\n        사용자가 상대적인 시간(예: 어제, 그저께, 아까)을 언급하지 않으면 workedAt은 생략하세요(현재 시각이 기본값으로 채워집니다).\n    ");
    }

    private static String farmCropContext(List<FarmOption> farms, Map<String, List<CropOption>> cropsByFarm) {
        var lines = new ArrayList<String>();
        for (var farm : farms) {
            var names = new ArrayList<String>();
            for (var crop : cropsByFarm.getOrDefault(farm.getFarmId().toString(), List.of())) names.add(crop.getName());
            var crops = String.join(", ", names);
            var cropText = isBlank(crops) ? "등록된 작물 없음" : crops;
            lines.add("- " + farm.getName() + " (id: " + farm.getFarmId() + "): " + cropText);
        }
        var farmCropLines = String.join("\n", lines);
        if (isBlank(farmCropLines)) farmCropLines = "- 등록된 농지가 없습니다. 이 경우 세션을 계속하지 말고 사용자에게 먼저 농지 등록을 안내하세요.";
        return trimIndent(("\n" +
            "            사용자가 소유한 농지와 각 농지의 등록 작물:\n" +
            "            $farmCropLines\n" +
            "\n" +
            "            사용자가 말한 농지/작물명을 위 목록에서 찾아 정확히 매칭하세요. 목록에 없는 이름이거나 애매하면\n" +
            "            추측하지 말고 다시 물어보세요(예: \"포도밭엔 포도랑 배만 있는데, 어떤 작물이었나요?\").\n" +
            "        ").replace("$farmCropLines", farmCropLines));
    }
    private static String pesticideSection(List<VoicePesticideOption> pesticides) {
        if (pesticides.isEmpty()) return null;
        var section = new StringBuilder("사용자 작물에 흔히 쓰는 등록 농약 목록:\n");
        for (var option : pesticides) {
            var pestText = option.getPests().isEmpty() ? "" : " — 대상: " + String.join(", ", option.getPests());
            section.append("- ").append(option.getName()).append(pestText).append('\n');
        }
        return section.append("사용자가 말한 농약명이 이 목록의 이름과 비슷하면 \"코니도 말씀이신가요?\"처럼 확인하고 " +
            "정확한 명칭으로 기록하세요. 목록에 없어도 괜찮습니다. 들리는 대로 기록하세요.").toString();
    }
    private static boolean isBlank(String value) {
        return value.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c));
    }
    private static String trimIndent(String value) {
        var lines = value.split("\r\n|\n|\r", -1);
        int indent = Integer.MAX_VALUE;
        for (var line : lines) {
            if (isBlank(line)) continue;
            int count = 0;
            while (count < line.length() && (Character.isWhitespace(line.charAt(count)) || Character.isSpaceChar(line.charAt(count)))) count++;
            indent = Math.min(indent, count);
        }
        if (indent == Integer.MAX_VALUE) indent = 0;
        var result = new ArrayList<String>();
        for (int i = 0; i < lines.length; i++) {
            if ((i == 0 || i == lines.length - 1) && isBlank(lines[i])) continue;
            result.add(lines[i].substring(Math.min(indent, lines[i].length())));
        }
        return String.join("\n", result);
    }
}
