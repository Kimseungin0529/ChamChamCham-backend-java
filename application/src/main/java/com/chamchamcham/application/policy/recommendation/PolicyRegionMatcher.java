package com.chamchamcham.application.policy.recommendation;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class PolicyRegionMatcher {
    private final Map<String, List<String>> provinceAliases = new LinkedHashMap<>();
    private final Pattern cityPattern = Pattern.compile("[가-힣]+(?:시|군|구)");
    public PolicyRegionMatcher() {
        provinceAliases.put("충청북도", List.of("충청북도", "충북"));
        provinceAliases.put("충청남도", List.of("충청남도", "충남"));
        provinceAliases.put("경기도", List.of("경기도", "경기"));
        provinceAliases.put("강원특별자치도", List.of("강원특별자치도", "강원"));
        provinceAliases.put("전북특별자치도", List.of("전북특별자치도", "전북"));
        provinceAliases.put("전라남도", List.of("전라남도", "전남"));
        provinceAliases.put("경상북도", List.of("경상북도", "경북"));
        provinceAliases.put("경상남도", List.of("경상남도", "경남"));
        provinceAliases.put("제주특별자치도", List.of("제주특별자치도", "제주"));
    }
    public Set<String> extractRegionTokens(Collection<String> addresses) {
        Objects.requireNonNull(addresses, "addresses");
        var nonNull = new ArrayList<String>();
        for (String address : addresses) if (address != null) nonNull.add(address);
        String joined = String.join(" ", nonNull);
        var tokens = new LinkedHashSet<String>();
        provinceAliases.forEach((canonical, aliases) -> {
            if (aliases.stream().anyMatch(joined::contains)) tokens.add(canonical);
        });
        var matcher = cityPattern.matcher(joined);
        while (matcher.find()) tokens.add(matcher.group());
        return tokens;
    }
    public boolean matches(Set<String> memberRegions, Set<String> policyRegions) {
        Objects.requireNonNull(memberRegions, "memberRegions");
        Objects.requireNonNull(policyRegions, "policyRegions");
        return policyRegions.contains("전국") || memberRegions.stream().anyMatch(policyRegions::contains);
    }
}
