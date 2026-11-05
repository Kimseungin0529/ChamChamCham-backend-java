package com.chamchamcham.application.policy.support;

import com.chamchamcham.application.policy.source.nongupez.NongupEzPolicyDetail;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class NongupEzPolicyTagExtractor {
    private final List<String> provinceNames = List.of(
        "서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시", "대전광역시", "울산광역시",
        "세종특별자치시", "경기도", "강원특별자치도", "충청북도", "충청남도", "전북특별자치도",
        "전라남도", "경상북도", "경상남도", "제주특별자치도");
    public ExtractedPolicyTags extract(NongupEzPolicyDetail detail) {
        Objects.requireNonNull(detail, "detail");
        var parts = new ArrayList<String>();
        for (String part : Arrays.asList(detail.getTitle(), detail.getSummary(), detail.getEligibility(), detail.getBenefit(), detail.getAgencyName())) if (part != null) parts.add(part);
        String text = String.join(" ", parts);
        var targetTags = new LinkedHashSet<String>();
        var cropTags = new LinkedHashSet<String>();
        var regionTags = new LinkedHashSet<String>();
        if (text.contains("청년")) targetTags.add("YOUNG_FARMER");
        if (text.contains("귀농") || text.contains("귀촌")) targetTags.add("RETURNING_FARMER");
        if (text.contains("농업경영정보") || text.contains("농업경영체")) targetTags.add("REGISTERED_FARMER");
        if (text.contains("법인")) targetTags.add("AGRICULTURAL_CORPORATION");
        if (text.contains("약용작물")) cropTags.add("MEDICINAL_CROP");
        if (text.contains("특용작물")) cropTags.add("SPECIAL_CROP");
        if (text.contains("친환경")) cropTags.add("ECO_FRIENDLY");
        for (String province : provinceNames) if (text.contains(province)) regionTags.add(province);
        if (regionTags.isEmpty()) regionTags.add("전국");
        return new ExtractedPolicyTags(targetTags, cropTags, regionTags);
    }
}
