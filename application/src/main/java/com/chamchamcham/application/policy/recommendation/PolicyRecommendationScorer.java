package com.chamchamcham.application.policy.recommendation;

import com.chamchamcham.domain.member.ManagementType;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Component
public class PolicyRecommendationScorer {
    private static final String NATIONAL_REGION_TAG = "전국";
    private static final Set<String> MATCHABLE_TARGETS = new LinkedHashSet<>(List.of("YOUNG_FARMER", "REGISTERED_FARMER", "AGRICULTURAL_CORPORATION", "RETURNING_FARMER"));
    private static final Set<String> STRICT_TARGETS = new LinkedHashSet<>(List.of("YOUNG_FARMER", "RETURNING_FARMER"));
    private static final Set<String> MATCHABLE_CROPS = new LinkedHashSet<>(List.of("MEDICINAL_CROP", "SPECIAL_CROP"));
    private final PolicyRegionMatcher regionMatcher;
    public PolicyRecommendationScorer(PolicyRegionMatcher regionMatcher) { this.regionMatcher = Objects.requireNonNull(regionMatcher, "regionMatcher"); }
    public PolicyScore score(PolicyMemberProfile profile, Set<String> policyTargetTags, Set<String> policyCropTags,
        Set<String> policyRegionTags, boolean onlineApplyAvailable, LocalDate today) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(policyTargetTags, "policyTargetTags");
        Objects.requireNonNull(policyCropTags, "policyCropTags");
        Objects.requireNonNull(policyRegionTags, "policyRegionTags");
        Objects.requireNonNull(today, "today");
        if (!isEligible(profile, policyTargetTags, policyCropTags, policyRegionTags, today)) return new PolicyScore(false, BigDecimal.ZERO, "추천 조건이 맞지 않아요.");
        int score = 30;
        var signals = new ArrayList<String>();
        if (policyTargetTags.contains("YOUNG_FARMER") && matchesYoungFarmer(profile, today)) { score += 20; signals.add("청년농 대상"); }
        if (policyTargetTags.contains("REGISTERED_FARMER") && matchesRegisteredFarmer(profile)) { score += 15; signals.add("경영체 유형"); }
        if (policyTargetTags.contains("AGRICULTURAL_CORPORATION") && matchesAgriculturalCorporation(profile)) { score += 15; signals.add("법인 유형"); }
        if (policyTargetTags.contains("RETURNING_FARMER") && matchesReturningFarmer(profile)) { score += 12; signals.add("영농경력"); }
        if (policyCropTags.contains("MEDICINAL_CROP") && matchesMedicinalCrop(profile)) { score += 18; signals.add("재배 품목"); }
        if (policyCropTags.contains("SPECIAL_CROP") && matchesSpecialCrop(profile)) { score += 10; signals.add("작물 분야"); }
        if (regionMatcher.matches(profile.getFarmRegionTokens(), policyRegionTags)) { score += 10; signals.add("농장 지역"); }
        if (onlineApplyAvailable) { score += 3; signals.add("온라인 신청"); }
        String reason;
        if (signals.contains("청년농 대상") && signals.contains("재배 품목")) reason = "청년농 대상이고 재배 품목이 맞아요.";
        else if (signals.contains("경영체 유형") && signals.contains("재배 품목")) reason = "경영체 유형과 재배 품목이 맞아요.";
        else if (signals.contains("농장 지역")) reason = "신청 가능한 정책이고 농장 지역이 맞아요.";
        else if (!signals.isEmpty()) reason = String.join("과 ", signals.subList(0, Math.min(2, signals.size()))) + "이 맞아요.";
        else reason = "신청 가능한 정책이에요.";
        return new PolicyScore(true, BigDecimal.valueOf(score), reason);
    }
    private boolean isEligible(PolicyMemberProfile profile, Set<String> targets, Set<String> crops, Set<String> regions, LocalDate today) {
        var targetRequirements = new LinkedHashSet<>(targets);
        targetRequirements.retainAll(MATCHABLE_TARGETS);
        var cropRequirements = new LinkedHashSet<>(crops);
        cropRequirements.retainAll(MATCHABLE_CROPS);
        boolean specificRegion = regions.stream().anyMatch(region -> !Objects.equals(region, NATIONAL_REGION_TAG));
        if (targetRequirements.isEmpty() && cropRequirements.isEmpty() && !specificRegion) return false;
        var strict = new LinkedHashSet<>(targetRequirements);
        strict.retainAll(STRICT_TARGETS);
        var alternative = new LinkedHashSet<>(targetRequirements);
        alternative.removeAll(strict);
        if (strict.stream().anyMatch(tag -> !matchesTargetTag(profile, tag, today))) return false;
        if (!alternative.isEmpty() && alternative.stream().noneMatch(tag -> matchesTargetTag(profile, tag, today))) return false;
        if (!cropRequirements.isEmpty() && cropRequirements.stream().noneMatch(tag -> matchesCropTag(profile, tag))) return false;
        return !specificRegion || regionMatcher.matches(profile.getFarmRegionTokens(), regions);
    }
    private boolean matchesTargetTag(PolicyMemberProfile profile, String tag, LocalDate today) {
        return switch (tag) {
            case "YOUNG_FARMER" -> matchesYoungFarmer(profile, today);
            case "REGISTERED_FARMER" -> matchesRegisteredFarmer(profile);
            case "AGRICULTURAL_CORPORATION" -> matchesAgriculturalCorporation(profile);
            case "RETURNING_FARMER" -> matchesReturningFarmer(profile);
            default -> false;
        };
    }
    private boolean matchesCropTag(PolicyMemberProfile profile, String tag) {
        return switch (tag) { case "MEDICINAL_CROP" -> matchesMedicinalCrop(profile); case "SPECIAL_CROP" -> matchesSpecialCrop(profile); default -> false; };
    }
    private boolean matchesYoungFarmer(PolicyMemberProfile profile, LocalDate today) { return profile.getBirthDate() != null && Period.between(profile.getBirthDate(), today).getYears() < 40; }
    private boolean matchesRegisteredFarmer(PolicyMemberProfile profile) { return profile.getManagementType() != null && profile.getManagementType() != ManagementType.NON_REGISTERED_FARMER; }
    private boolean matchesAgriculturalCorporation(PolicyMemberProfile profile) { return profile.getManagementType() == ManagementType.AGRICULTURAL_CORPORATION; }
    private boolean matchesReturningFarmer(PolicyMemberProfile profile) { return (profile.getExperienceLevel() == null ? 99 : profile.getExperienceLevel()) <= 3; }
    private boolean matchesMedicinalCrop(PolicyMemberProfile profile) { return profile.getCropUsePartCategories().contains("ROOT_BARK"); }
    private boolean matchesSpecialCrop(PolicyMemberProfile profile) { return profile.getCropNames().stream().anyMatch(name -> name.contains("작물") || name.contains("당귀") || name.contains("작약")); }
}
