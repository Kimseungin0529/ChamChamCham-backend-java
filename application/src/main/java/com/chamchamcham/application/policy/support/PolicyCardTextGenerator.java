package com.chamchamcham.application.policy.support;

import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class PolicyCardTextGenerator {
    private static final int MAX_CARD_CHARS = 19;
    public String eligibilitySummary(String text) { return eligibilityCategory(text).getLabel(); }
    public PolicyEligibilityCategory eligibilityCategory(String text) {
        String source = text == null ? "" : text;
        if (containsAny(source, "청년", "청년농", "청년후계", "만 40세")) return PolicyEligibilityCategory.YOUNG_FARMER;
        if (containsAny(source, "귀농", "귀촌")) return PolicyEligibilityCategory.RETURNING_FARMER;
        if (containsAny(source, "여성농업인", "결혼이민여성")) return PolicyEligibilityCategory.FEMALE_FARMER;
        if (containsAny(source, "60세", "65세", "고령", "은퇴", "농지이양")) return PolicyEligibilityCategory.SENIOR_RETIRED_FARMER;
        if (containsAny(source, "임산부", "학생", "취약계층", "기초생활", "차상위")) return PolicyEligibilityCategory.SPECIAL_TARGET;
        if (containsAny(source, "재해", "피해", "재난", "전염병", "질병")) return PolicyEligibilityCategory.DAMAGED_FARM;
        if (containsAny(source, "친환경인증", "친환경 인증", "GAP", "HACCP", "인증농가", "인증을 받은")) return PolicyEligibilityCategory.CERTIFIED_FARMER;
        if (containsAny(source, "축산", "가축", "한우", "젖소", "돼지", "양돈", "낙농", "송아지", "암소")) return PolicyEligibilityCategory.LIVESTOCK_FARM;
        if (containsAny(source, "수출농가", "수출업체", "수출기업", "해외", "박람회")) return PolicyEligibilityCategory.EXPORT_BUSINESS;
        if (containsAny(source, "농식품 기업", "농식품기업", "식품기업", "외식기업", "벤처", "창업기업")) return PolicyEligibilityCategory.FOOD_AGRI_COMPANY;
        if (containsAny(source, "농업법인", "영농조합", "농업회사법인", "생산자단체", "농협", "협동조합")) return PolicyEligibilityCategory.AGRI_CORPORATION;
        if (containsAny(source, "농업경영체", "농업경영정보", "경영체 등록")) return PolicyEligibilityCategory.REGISTERED_FARMER;
        if (containsAny(source, "농업인", "임업인", "농가")) return PolicyEligibilityCategory.FARMER;
        return PolicyEligibilityCategory.UNKNOWN;
    }
    public String benefitSummary(String text) { return benefitCategory(text).getLabel(); }
    public PolicyBenefitCategory benefitCategory(String text) {
        String source = text == null ? "" : text;
        if (containsAny(source, "직불금", "지원금", "보조금", "장려금", "수당", "바우처")) return PolicyBenefitCategory.GRANT;
        if (containsAny(source, "융자", "정책자금", "대출", "금리", "이자")) return PolicyBenefitCategory.FINANCE;
        if (containsAny(source, "시설", "장비", "농기계", "설치", "개보수")) return PolicyBenefitCategory.FACILITY_EQUIPMENT;
        if (containsAny(source, "교육", "컨설팅", "상담", "연수")) return PolicyBenefitCategory.EDUCATION;
        if (containsAny(source, "보험", "보험료", "연금", "건강", "복지")) return PolicyBenefitCategory.WELFARE;
        if (containsAny(source, "인증", "검정", "품질", "무병묘", "저탄소")) return PolicyBenefitCategory.CERTIFICATION;
        if (containsAny(source, "박람회", "수출", "판로", "홍보", "브랜드")) return PolicyBenefitCategory.MARKET;
        if (containsAny(source, "창업", "사업화", "벤처", "R&D")) return PolicyBenefitCategory.STARTUP;
        if (containsAny(source, "수질", "용수", "저수지", "가뭄", "환경", "인프라")) return PolicyBenefitCategory.ENVIRONMENT_INFRA;
        return PolicyBenefitCategory.ETC;
    }
    public String periodLabel(LocalDate start, LocalDate end, String notice) {
        String sourceNotice = notice == null ? null : trim(notice);
        String label;
        if (start != null && end != null) label = full(start) + "~" + String.format("%02d.%02d", end.getMonthValue(), end.getDayOfMonth());
        else if (sourceNotice != null && !sourceNotice.isEmpty()) label = sourceNotice;
        else if (end != null) label = full(end) + "까지";
        else label = "접수기관문의";
        String normalized = trim(label.replaceAll("\\s+", " "));
        if (normalized.isEmpty()) return "접수기관문의";
        return normalized.substring(0, Math.min(MAX_CARD_CHARS, normalized.length()));
    }
    private static String full(LocalDate date) { return String.format("%04d.%02d.%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth()); }
    private static boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            for (int i = 0; i <= source.length() - keyword.length(); i++) {
                if (source.regionMatches(true, i, keyword, 0, keyword.length())) return true;
            }
        }
        return false;
    }
    private static String trim(String value) {
        int start = 0, end = value.length();
        while (start < end && whitespace(value.charAt(start))) start++;
        while (end > start && whitespace(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }
    private static boolean whitespace(char value) {
        return Character.isWhitespace(value) || Character.isSpaceChar(value);
    }
}
